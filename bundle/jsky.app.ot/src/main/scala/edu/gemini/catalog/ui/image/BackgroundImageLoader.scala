package edu.gemini.catalog.ui.image

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.time.Instant

import edu.gemini.catalog.image.{ImageCatalog, ImageCatalogClient, ImageEntry}
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.shared.util.immutable.ScalaConverters._

import scala.collection.JavaConverters._
import edu.gemini.pot.sp.{ISPNode, ISPProgram}
import edu.gemini.spModel.core.Coordinates
import jsky.app.ot.tpe.{TpeContext, TpeManager}
import jsky.util.Preferences

import scalaz._
import Scalaz._
import scala.swing.Swing
import scalaz.concurrent.Task

case class ImageLoadRequest(catalog: ImageCatalog, c: Coordinates)

object BackgroundImageLoader {
  val cacheDir = Preferences.getPreferences.getCacheDir

  /** Called when a program is created to download its images */
  def watch(prog: ISPProgram): Unit = {
      val tasks = prog.getAllObservations.asScala.toList.flatMap(_.getObsComponents.asScala).map(node => node.getDataObject match {
        case t: TargetObsComp => requestImageDownload(TpeContext(node))
        case _                => Task.now(none)
      })
      // Run
      runAsync(tasks) {
        case \/-(e) => e.flatten.foreach(setTpeImage) // Try to set the image on the tpe for any target being displayed
        case -\/(e) => println(e)
      }
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
    // Attempt to find the image locally
    val local = for {
      c <- OptionT(Task.now(tpeCoordinates(tpe)))
      i <- OptionT(ImageCatalogClient.findImage(c, BackgroundImageLoader.cacheDir))
    } yield i

    // If not found request to download
    val image = local.orElse(OptionT(BackgroundImageLoader.requestImageDownload(tpe)))

    // Execute and set the image on the tpe
    runAsync(image.run) {
      case \/-(Some(x)) => setTpeImage(x)
      case \/-(_)       => // No image was found, don't do anything
      case -\/(e)       => // Ignore the errors
    }
  }

  /**
    * Creates a task to load an image
    */
  private[image] def requestImageDownload(tpe: TpeContext): Task[Option[ImageEntry]] =
    // Read the context, scheduling block, target and read the image
    tpeCoordinates(tpe).map(ImageCatalogClient.loadImage(cacheDir)).sequenceU

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
    * Utility methods to run the tasks on a separate thread
    */
  private def runAsync[A](tasks: List[Task[A]])(f: Throwable \/ List[A] => Unit) =
    Task.fork(Task.gatherUnordered(tasks)).unsafePerformAsync(f)

  private def runAsync[A](tasks: Task[A])(f: Throwable \/ A => Unit) =
    Task.fork(tasks).unsafePerformAsync(f)

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
