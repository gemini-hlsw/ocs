package edu.gemini.catalog.ui.image

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.io.File
import java.time.Instant

import edu.gemini.catalog.image.{ImageCatalogClient, ImageEntry}
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.shared.util.immutable.{Option => JOption}

import scala.collection.JavaConverters._
import edu.gemini.pot.sp.{ISPNode, ISPProgram}
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.obs.context.ObsContext
import jsky.util.Preferences

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

object BackgroundImageLoader {
  val instance = this
  val cacheDir = Preferences.getPreferences.getCacheDir

  /**
    * Called from the java side
    */
  def findIfAvailable(c: Coordinates): JOption[File] = {
    ImageCatalogClient.findImage(c, cacheDir).map(_.asGeminiOpt).unsafePerformSync
  }

  def watch(prog: ISPProgram): Unit = {
    prog.addCompositeChangeListener(CompositePropertyChangeListener)
    val tasks = prog.getAllObservations.asScala.toList.flatMap(_.getObsComponents.asScala).flatMap(node => node.getDataObject match {
      case t: TargetObsComp => requestImageDownload(node, t)
      case _                => Task.now(()).some
    })
    // Run
    Task.gatherUnordered(tasks).unsafePerformAsync  {
      case \/-(e) => println(e.mkString("\n"))// Sucessful case
      case -\/(e) => println(e)
    }
  }

  def requestImageDownload(node: ISPNode, t: TargetObsComp): Option[Task[ImageEntry]] =
    // Read the context, scheduling block, target and read the image
    for {
      ctx <- ObsContext.create(node.getContextObservation).asScalaOpt
      te <- Option(t.getTargetEnvironment.getBase)
      when = ctx.getSchedulingBlockStart.asScalaOpt | Instant.now.toEpochMilli
      c <- te.getTarget.coords(when)
    } yield ImageCatalogClient.loadImage(cacheDir)(c)

  def unwatch(prog: ISPProgram): Unit =
    prog.removeCompositeChangeListener(CompositePropertyChangeListener)

  private object CompositePropertyChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit = {
      val task = Option(evt.getSource).collect {
        case node: ISPNode =>
          node.getDataObject match {
            case t: TargetObsComp => requestImageDownload(node, t)
            case _                => Task.now(()).some
          }
      }
      task.flatten.foreach(_.unsafePerformAsync {
        case \/-(_) => // Sucessful case
        case -\/(e) => println(e)
      })
    }
  }

}
