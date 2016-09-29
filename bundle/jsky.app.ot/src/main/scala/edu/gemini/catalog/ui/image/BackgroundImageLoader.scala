package edu.gemini.catalog.ui.image

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}

import edu.gemini.catalog.image._
import edu.gemini.shared.util.immutable.ScalaConverters._

import scala.collection.JavaConverters._
import edu.gemini.pot.sp._
import edu.gemini.spModel.core.{Coordinates, Wavelength}
import jsky.app.ot.tpe.{ImageCatalogPanel, TpeContext, TpeImageWidget, TpeManager}
import jsky.image.gui.ImageLoadingException
import jsky.util.Preferences

import scalaz._
import Scalaz._
import scala.swing.Swing
import scalaz.concurrent.{Strategy, Task}

/**
  * Describes a requested image for an observation and wavelength
  */
case class TargetImageRequest(key: SPNodeKey, coordinates: Coordinates, obsWavelength: Option[Wavelength])

object TargetImageRequest {
  implicit val equal: Equal[TargetImageRequest] = Equal.equalA[TargetImageRequest]
}

/**
  * This interface can be used to listen when the image is being loaded and update the UI
  */
trait ImageLoadingListener {
  def downloadStarts(): Task[Unit]
  def downloadCompletes(): Task[Unit]
  def downloadError(): Task[Unit]
}

/**
  * Listens for program changes and download images as required. It listens
  * for program changes and requests downloading images if they are not already
  * presents.
  * It also updates the UI as needed
  */
object BackgroundImageLoader {
  private val ImageDownloadsThreadFactory = new ThreadFactory {
    private val threadNumber: AtomicInteger = new AtomicInteger(1)
    private val defaultThreadFactory = Executors.defaultThreadFactory()

    override def newThread(r: Runnable): Thread = {
      val name = s"Background Image Downloads - ${threadNumber.getAndIncrement()}"
      defaultThreadFactory.newThread(r) <| {_.setDaemon(true)} <| {_.setName(name)} <| {_.setPriority(Thread.MIN_PRIORITY)}
    }
  }

  /**
    * Execution context lets up to 6 low priority threads
    */
  private val lowPriorityEC = Executors.newFixedThreadPool(6, ImageDownloadsThreadFactory)

  /**
    * Regular execution context
    */
  private val highPriorityEC = Strategy.DefaultExecutorService

  // Directory where the image cache exists. Note that getPreferences calls to disk
  private def cacheDir: Task[Path] = Task.delay(Preferences.getPreferences.getCacheDir.toPath)

  /** Called when a program is created to download its images */
  def watch(prog: ISPProgram): Unit = {
    // Listen for future changes
    prog.addCompositeChangeListener(ChangeListener)
    val targets = prog.getAllObservations.asScala.toList.flatMap(_.getObsComponents.asScala.flatMap(n => requestedImage(TpeContext(n))))
    // remove duplicates and request images
    val tasks = targets.distinct.map(requestImageDownload(lowPriorityEC))
    // Run as low priority
    runAsync(tasks)(_ => ())(lowPriorityEC)
  }

  /** Called when a program is removed to clear the cache */
  def unwatch(prog: ISPProgram): Unit = {
    prog.removeCompositeChangeListener(ChangeListener)
  }

  private val taskUnit = Task.now(())

  /**
    * Display an image if available on disk or request the download if necessary
    */
  def loadImageOnTheTpe(tpe: TpeContext): Unit = {
    val task = requestedImage(tpe).map(requestImageDownload(highPriorityEC)).getOrElse(taskUnit)

    // This method called on an explicit user interaction so we'd rather
    // Request the execution in a higher priority thread
    // Execute and set the image on the tpe
    runAsync(task)(_ => ())(highPriorityEC)
  }

  // Watches for changes to existing observations, runs BAGS on them when updated.
  object ChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit =
      evt.getSource match {
        case node: ISPNode => Option(node.getContextObservation).foreach { o =>
          val task = requestedImage(TpeContext(o)).map(requestImageDownload(highPriorityEC)).getOrElse(taskUnit)

          // Run it in the background as it is lower priority than GUI
          runAsync(task)(_ => ())(lowPriorityEC)
        }
      }
  }

  /**
    * Creates a task to load an image and set it on the tpe
    */
  private[image] def requestImageDownload(pool: ExecutorService)(t: TargetImageRequest): Task[Unit] =
    for {
      cacheDir <- cacheDir
      catalog  <- ObservationCatalogOverrides.catalogFor(t.key, t.obsWavelength)
      image    <- loadImage(ImageSearchQuery(catalog, t.coordinates), cacheDir, ImageCatalogPanel.resetListener)(pool)
    } yield image match {
      case Some(e) => updateTpeImage(e) // Try to set it on the UI
      case _       => // Ignore, the image was invalid or not found
    }

  /**
    * Load an image for the given query
    * It will check if the image is in the cache or in progress before requesting a download
    * It updates the listener as needed to update the UI
    */
  private def loadImage(query: ImageSearchQuery, dir: Path, listener: ImageLoadingListener)(pool: ExecutorService): Task[Option[ImageEntry]] = {

    def addToCacheAndGet(f: Path): Task[ImageEntry] = {
      val i = ImageEntry(query, f, f.toFile.length())
      // Add to cache and prune the cache
      // Note that cache pruning goes in a different thread
      StoredImagesCache.add(i) *> ImageCacheOnDisk.pruneCache *> Task.now(i)
    }

    def readImageToFile: NonEmptyList[Task[Path]] =
      query.url.map(ImageCatalogClient.downloadImageToFile(dir, _, query.fileName))

    def downloadImage: Task[ImageEntry] = {
      val task = for {
        _ <- ImagesInProgress.start(query) *> listener.downloadStarts()
        f <- TaskHelper.selectFirstToComplete(readImageToFile)(pool)
        e <- addToCacheAndGet(f)
      } yield e

      // Remove query from registry and Inform listeners at the end
      task.onFinish {
        case Some(_) => ImagesInProgress.failed(query) *> listener.downloadError()
        case _       => ImagesInProgress.completed(query) *> listener.downloadCompletes()
      }
    }

    def checkIfNeededAndDownload: Task[Option[ImageEntry]] =
      ImagesInProgress.inProgress(query) >>= { inProcess => if (inProcess) Task.now(None) else downloadImage.map(Some.apply) }

    // Try to find the image on the cache, else download
    StoredImagesCache.find(query) >>= { _.filter(_.file.toFile.exists()).fold(checkIfNeededAndDownload)(f => Task.now(Some(f))) }
  }
  /**
    * Extracts the data to request an image from the current context
    */
  private def requestedImage(tpe: TpeContext): Option[TargetImageRequest] =
    for {
      ctx    <- tpe.obsContext
      base   <- tpe.targets.base
      when   = ctx.getSchedulingBlockStart.asScalaOpt | Instant.now.toEpochMilli
      coords <- base.getTarget.coords(when)
      key    <- tpe.obsKey
    } yield TargetImageRequest(key, coords, ObsWavelengthExtractor.extractObsWavelength(tpe))

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
    * We'll only update the position if the coordinates match
    *
    */
  private def updateTpeImage(entry: ImageEntry): Unit = {
    def updateCacheAndDisplay(iw: TpeImageWidget): Task[Unit] = StoredImagesCache.markAsUsed(entry) *> Task.delay(iw.setFilename(entry.file.toAbsolutePath.toString, false))

    Swing.onEDT {
      for {
        tpe     <- Option(TpeManager.get())
        iw      <- Option(tpe.getImageWidget)
        request <- requestedImage(iw.getContext)
        if entry.query.isNearby(request.coordinates) // The TPE may have moved so only display if the coordinates match
        if ImageCatalogPanel.isCatalogSelected(entry.query.catalog) // Only set the image if the catalog matches
      } {
        val r = ImagesInProgress.inProgress(entry.query) >>= { inProgress => if (!inProgress) updateCacheAndDisplay(iw) else taskUnit}

        // Function to capture an exception and request a new download
        val reDownload: PartialFunction[Throwable, Task[Unit]] = {
          case _: ImageLoadingException =>
            // This happens typically if the image is corrupted
            // Let's try to re-download
            Task.delay(entry.file.toFile.delete).ifM(Task.delay(TpeContext.fromTpeManager.foreach(loadImageOnTheTpe)), taskUnit)
        }
        // We don't really care about the result but want to intercept
          // file errors to redownload the image
        r.handleWith(reDownload).unsafePerformSync
      }
    }
  }
}
