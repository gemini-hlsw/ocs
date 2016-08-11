package edu.gemini.catalog.ui.image

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}

import edu.gemini.catalog.image.{ImageCatalog, ImageCatalogClient, ImageEntry}
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.shared.util.immutable.ScalaConverters._

import scala.collection.JavaConverters._
import edu.gemini.pot.sp.ISPProgram
import edu.gemini.spModel.core.Coordinates
import jsky.app.ot.tpe.{TpeContext, TpeManager}
import jsky.util.Preferences

import scalaz._
import Scalaz._
import scala.swing.Swing
import scalaz.concurrent.{Strategy, Task}

case class ImageLoadRequest(catalog: ImageCatalog, c: Coordinates)

object BackgroundImageLoader {
  val cacheDir = Preferences.getPreferences.getCacheDir

  val ImageDownloadsThreadFactory = new ThreadFactory {
    private val threadNumber: AtomicInteger = new AtomicInteger(1)
    val defaultThreadFactory = Executors.defaultThreadFactory()
    def newThread(r: Runnable) = {
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
  val ec = Executors.newFixedThreadPool(4, ImageDownloadsThreadFactory)

  /** Called when a program is created to download its images */
  def watch(prog: ISPProgram): Unit = {
      val targets = prog.getAllObservations.asScala.toList.flatMap(_.getObsComponents.asScala.map(n => (n, n.getDataObject)).collect {
        case (node, t: TargetObsComp) =>
          tpeCoordinates(TpeContext(node))
      })
      // remove duplicates
      val tasks = targets.flatten.distinct.map(requestImageDownload)
      // Run
      runAsync(tasks) {
        case \/-(e) => // done
        case -\/(e) => println(e)
      }(ec)
    }

  /** Called when a program is removed to clear the cache */
  def unwatch(prog: ISPProgram): Unit = {
    // not necessary
  }

  /******************************************
    * Methods interacting with the java side
    *****************************************/
  /**
    * Display an image if available on disk or request the download if necessary
    */
  def loadImageOnTheTpe(tpe: TpeContext): Unit = {
    val t = tpeCoordinates(tpe).map(requestImageDownload).getOrElse(Task.now(()))

    // This is called on an explicit user interaction so we'd rather
    // Request the execution in a higher priority thread
    // Execute and set the image on the tpe
    runAsync(t) {
      case \/-(_)       => // No image was found, don't do anything
      case -\/(e)       => // Ignore the errors
    }(Strategy.DefaultExecutorService)
  }

  /**
    * Creates a task to load an image and set it on the tpe
    */
  private[image] def requestImageDownload(c: Coordinates): Task[Unit] =
    ImageCatalogClient.loadImage(cacheDir)(c).map(setTpeImage)

  /**
    * Finds the coordinates for the base target of the tpe
    */
  private def tpeCoordinates(tpe: TpeContext): Option[Coordinates] =
    for {
      ctx <- tpe.obsContext
      te  <- tpe.targets.base
      when = ctx.getSchedulingBlockStart.asScalaOpt | Instant.now.toEpochMilli
      c   <- te.getTarget.coords(when)
    } yield c

  /**
    * Utility methods to run the tasks on separate threads of the pool
    */
  private def runAsync[A](tasks: List[Task[A]])(f: Throwable \/ List[A] => Unit)(pool: ExecutorService) = {
    Task.gatherUnordered(tasks.map(t => Task.fork(t))).unsafePerformAsync(f)
  }

  private def runAsync[A](task: Task[A])(f: Throwable \/ A => Unit)(pool: ExecutorService) =
    Task.fork(task).unsafePerformAsync(f)

  /**
    * Attempts to set the image on the tpe, note that this is called from a separate
    * thread so we need to go to Swing for updating the UI
    * Since an image download may take a while the tpe may have moved.
    * We'll only update the position if the coordinates match
    */
  private def setTpeImage(entry: ImageEntry): Unit = {
    Swing.onEDT {
      for {
        tpe <- Option(TpeManager.get())
        iw  <- Option(tpe.getImageWidget)
        c   <- tpeCoordinates(iw.getContext)
        if c == entry.coordinates // The TPE may have moved so only display if the coordinates match
      } {
        iw.setFilename(entry.file.getAbsolutePath)
      }
    }
  }
}
