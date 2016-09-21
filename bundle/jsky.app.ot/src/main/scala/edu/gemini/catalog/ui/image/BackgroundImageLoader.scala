package edu.gemini.catalog.ui.image

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}

import edu.gemini.catalog.image._
import edu.gemini.shared.util.immutable.ScalaConverters._

import scala.collection.JavaConverters._
import edu.gemini.pot.sp.{ISPNode, ISPProgram, SPNodeKey}
import edu.gemini.spModel.core.Coordinates
import jsky.app.ot.tpe.{TpeContext, TpeImageWidget, TpeManager}
import jsky.util.Preferences

import scalaz._
import Scalaz._
import scala.swing.Swing
import scalaz.concurrent.{Strategy, Task}

case class ImageLoadRequest(catalog: ImageCatalog, c: Coordinates)

object BackgroundImageLoader {
  val cacheDir: Path = Preferences.getPreferences.getCacheDir.toPath

  val ImageDownloadsThreadFactory = new ThreadFactory {
    private val threadNumber: AtomicInteger = new AtomicInteger(1)
    private val defaultThreadFactory = Executors.defaultThreadFactory()

    override def newThread(r: Runnable): Thread = {
      val t = defaultThreadFactory.newThread(r)
      t.setDaemon(true)
      t.setName("Background Image Downloads - " + threadNumber.getAndIncrement())
      t.setPriority(Thread.MIN_PRIORITY)
      t
    }
  }

  /**
    * Execution context lets up to 4 low priority threads
    */
  private val lowPriorityEC = Executors.newFixedThreadPool(4, ImageDownloadsThreadFactory)

  /**
    * Regular execution context
    */
  private val highPriorityEC = Strategy.DefaultExecutorService

  /** Called when a program is created to download its images */
  def watch(prog: ISPProgram): Unit = {
    prog.addCompositeChangeListener(ChangeListener)
    val targets = prog.getAllObservations.asScala.toList.flatMap(_.getObsComponents.asScala.map(n => tpeCoordinates(TpeContext(n))))
    // remove duplicates
    val tasks = targets.flatten.distinct.map(Function.tupled(requestImageDownload(ImageLoadingListener.zero)))
    // Run
    runAsync(tasks) {
      case \/-(e) => println(e)// done
      case -\/(e) => println(e)
    }(lowPriorityEC)
  }

  /** Called when a program is removed to clear the cache */
  def unwatch(prog: ISPProgram): Unit = {
    prog.removeCompositeChangeListener(ChangeListener)
  }

  /******************************************
    * Methods interacting with the java side
    *****************************************/
  /**
    * Display an image if available on disk or request the download if necessary
    */
  def loadImageOnTheTpe(tpe: TpeContext, listener: ImageLoadingListener): Unit = {
    val task = tpeCoordinates(tpe).map(Function.tupled(requestImageDownload(listener))).getOrElse(Task.now(()))

    // This is called on an explicit user interaction so we'd rather
    // Request the execution in a higher priority thread
    // Execute and set the image on the tpe
    runAsync(task) {
      case \/-(_)       => // No image was found, don't do anything
      case -\/(e)       => // Ignore the errors
    }(highPriorityEC)
  }

  // Watches for changes to existing observations, runs BAGS on them when updated.
  object ChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit =
      evt.getSource match {
        case node: ISPNode => Option(node.getContextObservation).foreach { o =>
          val task = tpeCoordinates(TpeContext(o)).map(Function.tupled(requestImageDownload(ImageLoadingListener.zero))).getOrElse(Task.now(()))

          // Run it in the background as it is lower priority than GUI
          runAsync(task) {
            case \/-(_)       => // No image was found, don't do anything
            case -\/(e)       => // Ignore the errors
          }(lowPriorityEC)

        }
      }
  }

  /**
    * Creates a task to load an image and set it on the tpe
    */
  private[image] def requestImageDownload(listener: ImageLoadingListener)(key: SPNodeKey, c: Coordinates): Task[Unit] =
    for {
      catalog <- ObservationCatalogOverrides.catalogFor(key)
      image   <- ImageCatalogClient.loadImage(cacheDir)(ImageSearchQuery(catalog, c))(listener)
    } yield image match {
      case Some(e) => setTpeImage(e)
      case _       => // Ignore
    }

  /**
    * Finds the coordinates for the base target of the tpe
    */
  private def tpeCoordinates(tpe: TpeContext): Option[(SPNodeKey, Coordinates)] =
    for {
      ctx <- tpe.obsContext
      te  <- tpe.targets.base
      when = ctx.getSchedulingBlockStart.asScalaOpt | Instant.now.toEpochMilli
      c   <- te.getTarget.coords(when)
      k   <- tpe.obsKey
    } yield (k, c)

  /**
    * Utility methods to run the tasks on separate threads of the pool
    */
  private def runAsync[A](tasks: List[Task[A]])(f: Throwable \/ List[A] => Unit)(pool: ExecutorService) =
    Task.gatherUnordered(tasks.map(t => Task.fork(t)(pool))).unsafePerformAsync(f)

  private def runAsync[A](task: Task[A])(f: Throwable \/ A => Unit)(pool: ExecutorService) =
    Task.fork(task).unsafePerformAsync(f)

  /**
    * Attempts to set the image on the tpe, note that this is called from a separate
    * thread so we need to go to Swing for updating the UI
    * Since an image download may take a while the tpe may have moved.
    * We'll only update the position if the coordinates match
    */
  private def setTpeImage(entry: ImageEntry): Unit = {
    def markAndSet(iw: TpeImageWidget): Task[Unit] = StoredImagesCache.markAsUsed(entry) *> Task.now(iw.setFilename(entry.file.getAbsolutePath))

    Swing.onEDT {
      for {
        tpe <- Option(TpeManager.get())
        iw  <- Option(tpe.getImageWidget)
        c   <- tpeCoordinates(iw.getContext)
        if entry.query.isNearby(c._2) // The TPE may have moved so only display if the coordinates match
      } {
        val r = ImagesInProgress.contains(entry.query) >>= { inProgress => if (!inProgress) markAndSet(iw) else Task.now(())}
        // TODO: Handle errors
        r.unsafePerformSync
      }
    }
  }
}
