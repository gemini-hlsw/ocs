package edu.gemini.catalog.ui.image

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.time.Instant

import edu.gemini.catalog.image.{ImageCatalog, ImageCatalogClient, ImageEntry}
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.shared.util.immutable.ScalaConverters._

import scala.collection.JavaConverters._
import edu.gemini.pot.sp.{ISPNode, ISPProgram}
import edu.gemini.spModel.core.Coordinates
import jsky.app.ot.tpe.TpeContext
import jsky.util.Preferences

import scalaz._
import Scalaz._
import scala.swing.Swing
import scalaz.concurrent.Task

case class ImageLoadRequest(catalog: ImageCatalog, c: Coordinates)

/**
  * Provides interfaces to the Java side
  */
object BackgroundImageLoader4Java {
  import jsky.app.ot.tpe.{TpeImageWidget, TpeManager}

  /**
    * Display an image if available on disk or request the download if necessary
    */
  def findIfAvailable(tpe: TpeContext, c: Coordinates, iw: TpeImageWidget): Unit = {
    val r: Task[Option[ImageEntry]] = ImageCatalogClient.findImage(c, BackgroundImageLoader.cacheDir).flatMap {
      case Some(f) => Task.now(Some(f))
      case None => BackgroundImageLoader.requestImageDownload(tpe)// Should request a new image
    }

    BackgroundImageLoader.runAsync(r) {
      case \/-(Some(x)) =>
        setOnTpe(x)
      case \/-(_) => // Successful case
      case -\/(e) => println(e)
    }
  }

  // Attempts to set the image on the tpe
  def setOnTpe(entry: ImageEntry): Unit = {
    Swing.onEDT {
      val tpe = TpeManager.get()
      Option(tpe).foreach(_.getImageWidget.setFilename(entry.file.getAbsolutePath))
    }
  }
}

abstract class BackgroundImageLoader4Java

object BackgroundImageLoader {
  val instance = this
  val cacheDir = Preferences.getPreferences.getCacheDir

  def runAsync[A](tasks: List[Task[A]])(f: Throwable \/ List[A] => Unit) = {
    Task.fork(Task.gatherUnordered(tasks)).unsafePerformAsync(f)
  }

  def runAsync[A](tasks: Task[A])(f: Throwable \/ A => Unit) = {
    Task.fork(tasks).unsafePerformAsync(f)
  }

  def watch(prog: ISPProgram): Unit = {
      prog.addCompositeChangeListener(CompositePropertyChangeListener)

      val tasks = prog.getAllObservations.asScala.toList.flatMap(_.getObsComponents.asScala).map(node => node.getDataObject match {
        case t: TargetObsComp =>
          val tpeContext = TpeContext(node)
          requestImageDownload(tpeContext)
        case _ => Task.now(none)
      })
      // Run
      runAsync(tasks) {
        case \/-(e) => println(e.mkString("\n")) // Successful case
        case -\/(e) => println(e)
      }
    }

  private[image] def requestImageDownload(tpe: TpeContext): Task[Option[ImageEntry]] = {
    // Read the context, scheduling block, target and read the image
    (for {
      ctx <- tpe.obsContext
      te  <- tpe.targets.base
      when = ctx.getSchedulingBlockStart.asScalaOpt | Instant.now.toEpochMilli
      c   <- te.getTarget.coords(when)
    } yield ImageCatalogClient.loadImage(cacheDir)(c)).sequenceU
  }

  def unwatch(prog: ISPProgram): Unit = {
    prog.removeCompositeChangeListener(CompositePropertyChangeListener)
  }

  private object CompositePropertyChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit = {
      val task = Option(evt.getSource).collect {
        case node: ISPNode =>
          node.getDataObject match {
            case t: TargetObsComp =>
              val tpe  = TpeContext(node)

              runAsync(requestImageDownload(tpe)) {
                case \/-(_) => // Sucessful case
                case -\/(e) => println(e)
              }
            case _                => Task.now(none)
          }
      }
    }
  }

}
