package edu.gemini.catalog.ui.image

import java.beans.{PropertyChangeEvent, PropertyChangeListener}

import edu.gemini.catalog.ui.tpe.ImageCatalogLoader
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.obsComp.TargetObsComp

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import edu.gemini.pot.sp.{ISPNode, ISPProgram}

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

object BackgroundImageLoader {
  val instance = this

  def downloadImage(target: SPTarget): Task[Unit] = {
    target.getTarget.coords(0).map(ImageCatalogLoader.loadImage).getOrElse(Task.now(()))
  }

  def watch(prog: ISPProgram): Unit = {
    prog.addCompositeChangeListener(CompositePropertyChangeListener)
    val tasks = prog.getAllObservations.asScala.toList.flatMap(_.getObsComponents.asScala).flatMap(k => k.getDataObject match {
      case t: TargetObsComp => Option(t.getTargetEnvironment.getBase).map(downloadImage)
      case _                => Task.now(()).some
    })
    // Run
    Task.gatherUnordered(tasks).unsafePerformAsync  {
      case \/-(e) => // Sucessful case
      case -\/(e) => println(e)
    }
  }

  def unwatch(prog: ISPProgram): Unit = {
    prog.removeCompositeChangeListener(CompositePropertyChangeListener)
  }

  private object CompositePropertyChangeListener extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit = {
      val task = Option(evt.getSource).collect {
        case node: ISPNode => node.getDataObject match {
          case t: TargetObsComp => Option(t.getTargetEnvironment.getBase).map(downloadImage)
          case _ => Task.now(()).some
        }
      }
      task.flatten.foreach(_.unsafePerformAsync {
        case \/-(_) => // Sucessful case
        case -\/(e) => println(e)
      })
    }
  }

}
