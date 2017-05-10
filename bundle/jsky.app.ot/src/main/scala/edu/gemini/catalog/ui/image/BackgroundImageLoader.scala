package edu.gemini.catalog.ui.image

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}
import java.util.logging.{Level, Logger}

import edu.gemini.catalog.image._
import edu.gemini.shared.util.immutable.ScalaConverters._

import edu.gemini.pot.sp._
import edu.gemini.spModel.core.{Angle, Coordinates, Site, Wavelength}
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.rich.pot.sp._
import jsky.app.ot.tpe.{ImageCatalogPanel, TpeContext, TpeImageWidget, TpeManager}
import jsky.app.ot.userprefs.observer.ObserverPreferences
import jsky.image.gui.ImageLoadingException

import scalaz._
import Scalaz._
import scala.swing.Swing
import scalaz.concurrent.Task

/**
  * Describes a requested image for an observation and wavelength
  */
case class TargetImageRequest(key: SPNodeKey, coordinates: Coordinates, obsWavelength: Option[Wavelength], site: Option[Site])

object TargetImageRequest {
  /** @group Typeclass Instances */
  implicit val equal: Equal[TargetImageRequest] = Equal.equalA[TargetImageRequest]
}

/**
  * This interface can be used to listen when the image is being loaded and update the UI
  */
case class ImageLoadingListener[A](downloadStarts: Task[A], downloadCompletes: Task[A], downloadError: Task[A])

/**
  * Listens for program changes and download images as required. It listens
  * for program changes and requests downloading images if they are not already
  * presents.
  * It also updates the UI as needed
  */
object BackgroundImageLoader {
  val Log: Logger = Logger.getLogger(this.getClass.getName)
  private val taskUnit = Task.now(())

  private def imageDownloadsThreadFactory(priority: Int) = new ThreadFactory {
    private val threadNumber: AtomicInteger = new AtomicInteger(1)
    private val defaultThreadFactory = Executors.defaultThreadFactory()

    override def newThread(r: Runnable): Thread = {
      val name = s"Background Image Downloads - ${threadNumber.getAndIncrement()} - priority: $priority"
      defaultThreadFactory.newThread(r) <| {_.setDaemon(true)} <| {_.setName(name)} <| {_.setPriority(priority)}
    }
  }

  def newExecutor(priority: Int): ExecutorService =
    Executors.newFixedThreadPool(ImageCatalog.all.length, imageDownloadsThreadFactory(priority))

  /**
    * Execution context for lower priority downloads
    */
  private val lowPriorityEC = newExecutor(Thread.MIN_PRIORITY)

  /**
    * Regular execution context for higher priority tasks, i.e. UI requests
    */
  private val highPriorityEC = newExecutor(Thread.NORM_PRIORITY)

  private def logError[A](x: Throwable \/ A): Unit = x match {
    case -\/(e) => Log.log(Level.SEVERE, e.getMessage, e)
    case \/-(_) => // Ignore, successful case
  }

  /** Called when a program is created to download its images */
  def watch(prog: ISPProgram): Unit = {
    // At startup only load images for active programs
    def needsImage(ctx: TpeContext): Boolean =
      ctx.obsShell.exists(ObservationStatus.computeFor(_).isActive)

    // Listen for future changes
    prog.addCompositeChangeListener(ChangeListener)
    val targets = for {
        p      <- prog.allObservations
        tpeCtx  = TpeContext(p)
        if needsImage(tpeCtx)
        i      <- requestedImage(tpeCtx)
      } yield i

    // remove duplicates and request images
    val tasks = targets.distinct.map(requestImageDownload(lowPriorityEC))
    // Run as low priority
    runAsync(tasks)(logError)(lowPriorityEC)
  }

  /** Called when a program is removed to clear the cache */
  def unwatch(prog: ISPProgram): Unit = {
    prog.removeCompositeChangeListener(ChangeListener)
  }

  /**
    * Display an image if available on disk or request the download if necessary
    */
  def loadImageOnTheTpe(tpe: TpeContext): Unit = {
    val task = requestedImage(tpe).fold(taskUnit)(requestImageDownload(highPriorityEC))

    // This method called on an explicit user interaction so we'd rather
    // Request the execution in a higher priority thread
    // Execute and set the image on the tpe
    runAsync(task)(logError)(highPriorityEC)
  }

  // Watches for changes to existing observations, runs BAGS on them when updated.
  object ChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit =
      evt.getSource match {
        case node: ISPNode => Option(node.getContextObservation).foreach { o =>
          val task = requestedImage(TpeContext(o)).fold(taskUnit)(requestImageDownload(highPriorityEC))

          // Run it in the background as it is lower priority than GUI
          runAsync(task)(logError)(lowPriorityEC)
        }
      }
  }

  /**
    * Creates a task to load an image and set it on the tpe
    */
  private[image] def requestImageDownload(pool: ExecutorService)(t: TargetImageRequest): Task[Unit] =
    for {
      catalog  <- ObservationCatalogOverrides.catalogFor(t.key, t.obsWavelength)
      image    <- loadImage(ImageSearchQuery(catalog, t.coordinates, catalog.imageSize, t.site), ImageCatalogPanel.resetListener)(pool)
      _        <- image.fold(taskUnit)(e => Task.delay(updateTpeImage(e)))
    } yield ()

  /**
    * Load an image for the given query
    * It will check if the image is in the cache or in progress before requesting a download
    * It updates the listener as needed to update the UI
    */
  private def loadImage(query: ImageSearchQuery, listener: ImageLoadingListener[Unit])(pool: ExecutorService): Task[Option[ImageInFile]] = {

    def readImageToFile(dir: Path): NonEmptyList[Task[ImageInFile]] =
      query.url.map(ImageCatalogClient.downloadImageToFile(dir, _, query))

    def downloadImage(prefs: ImageCatalogPreferences): Task[ImageInFile] = {
      val task = for {
        _ <- KnownImagesSets.start(query) *> listener.downloadStarts
        f <- TaskHelper.selectFirstToComplete(readImageToFile(prefs.cacheDir))(pool)
        _ <- StoredImagesCache.add(f) *> ImageCacheOnDisk.pruneCache(prefs.imageCacheSize) // Add to cache and prune. Cache pruning goes in a different thread
      } yield f

      // Remove query from registry and inform listeners at the end
      task.onFinish {
        case Some(_) => KnownImagesSets.failed(query) *> listener.downloadError
        case _       => KnownImagesSets.completed(query) *> listener.downloadCompletes
      }
    }

    def checkIfNeededAndDownload(prefs: ImageCatalogPreferences): Task[Option[ImageInFile]] =
      KnownImagesSets.inProgress(query).ifM(Task.now(none), downloadImage(prefs).map(Some.apply))

    // Try to find the image on the cache, else download
    for {
      prefs   <- ImageCatalogPreferences.preferences()
      inCache <- StoredImagesCache.find(query)
      exists  <- Task.delay(inCache.filter(_.file.toFile.exists()))
      file    <- exists.fold(checkIfNeededAndDownload(prefs))(f => Task.now(f.some))
    } yield file

  }

  /**
    * Extracts the data to request an image from the current context
    */
  private def requestedImage(tpe: TpeContext): Option[TargetImageRequest] =
    for {
      ctx    <- tpe.obsContext
      ast    <- tpe.targets.asterism
      when   = ctx.getSchedulingBlockStart.asScalaOpt.map(ms => Instant.ofEpochMilli(ms.toLong))
      coords <- ast.basePosition(when orElse Some(Instant.now))
      key    <- tpe.obsKey
      site   = Option(ObserverPreferences.fetch.observingSite())
    } yield TargetImageRequest(key, coords, ObsWavelengthExtractor.extractObsWavelength(tpe), site)

  /**
    * Utility methods to run the tasks on separate threads of the pool
    */
  private def runAsync[A](tasks: List[Task[A]])(f: Throwable \/ List[A] => Unit)(pool: ExecutorService) =
    Task.gatherUnordered(tasks.map(t => Task.fork(t)(pool))).unsafePerformAsync(f)

  private def runAsync[A](task: Task[A])(f: Throwable \/ A => Unit)(pool: ExecutorService) =
    Task.fork(task).unsafePerformAsync(f)

  /**
    * Attempts to set the image on the tpe, note that this is called from a separate
    * thread, typically after an image download so we need to go to Swing for updating the UI
    *
    * Since an image download may take a while the tpe may have moved.
    * We'll only update the position if the coordinates and catalog match
    *
    */
  private def updateTpeImage(entry: ImageInFile): Unit = {
    def updateCacheAndDisplay(iw: TpeImageWidget): Task[Unit] = StoredImagesCache.markAsUsed(entry) *> Task.delay(iw.setFilename(entry.file.toAbsolutePath.toString, false))

    Swing.onEDT {
      // Run inside the EDT, we need to catch possible exceptions when setting the file on the UI
      for {
        tpe     <- Option(TpeManager.get())
        iw      <- Option(tpe.getImageWidget)
        ctx     =  iw.getContext
        request <- requestedImage(ctx)
        // TODO This check seems to be failing in a few spots on the sky, it may need to be loosened
        if entry.contains(request.coordinates, Angle.zero) // The TPE may have moved so only display if the coordinates match
        if ImageCatalogPanel.isCatalogSelected(entry.query.catalog) // Only set the image if the catalog matches
      } {
        val task = KnownImagesSets.inProgress(entry.query).ifM(taskUnit, updateCacheAndDisplay(iw))

        // Function to capture an exception and request a new download
        val reDownload: PartialFunction[Throwable, Task[Unit]] = {
          case _: ImageLoadingException =>
            // This happens typically if the image is corrupted
            // Let's try to re-download
            Task.delay(entry.file.toFile.delete).ifM(Task.delay(loadImageOnTheTpe(ctx)), taskUnit)
        }

        // We don't really care about the result but want to intercept
        // file errors to redownload the image
        task.handleWith(reDownload).unsafePerformSync
      }
    }
  }
}
