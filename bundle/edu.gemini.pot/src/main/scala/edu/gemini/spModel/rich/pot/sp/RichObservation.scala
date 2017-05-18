package edu.gemini.spModel.rich.pot.sp

import edu.gemini.pot.sp._
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.obs.{ObservationStatus, SPObservation}
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.target.obsComp.TargetObsComp

import scala.collection.JavaConverters._
import scalaz.\/

/**
 * Adds convenience to ISPObservation instances.
 */
class RichObservation(obs: ISPObservation) {
  def spObservation: Option[SPObservation] = obs.dataObject.map(_.asInstanceOf[SPObservation])

  def update(f: SPObservation => Unit) {
    spObservation foreach { dataObj =>
      f(dataObj)
      obs.dataObject = dataObj
    }
  }

  def libraryId: Option[String] =
    for {
      o <- spObservation
      l <- Option(o.getLibraryId)
    } yield l

  def findTargetObsComp: Option[TargetObsComp] =
    findObsComponentByType(TargetObsComp.SP_TYPE).map(_.getDataObject.asInstanceOf[TargetObsComp])

  def findObsComponentByType(t: SPComponentType): Option[ISPObsComponent] =
    findObsComponent(_.getType == t)

  def findSeqComponentByType(t: SPComponentType): Option[ISPSeqComponent] =
    findSeqComponent(_.getType == t)

  def findObsComponent(pred: ISPObsComponent => Boolean): Option[ISPObsComponent] =
    for {
      obsComps <- Option(obs.getObsComponents)
      comp     <- obsComps.asScala.find(pred)
    } yield comp

  def findSeqComponent(pred: ISPSeqComponent => Boolean): Option[ISPSeqComponent] =
    for {
      seq  <- Option(obs.getSeqComponent)
      comp <- seq.find(pred)
    } yield comp

  def findSeqComponents(pred: ISPSeqComponent => Boolean) =
    Option(obs.getSeqComponent).toList.flatMap(_.flatten.filter(pred))

  def findSeqComponentsByType(t: SPComponentType) = findSeqComponents(_.getType == t)

  def sites: Set[Site] =
    findObsComponent { _.getType.broadType == SPComponentBroadType.INSTRUMENT }.map { oc =>
      oc.getDataObject.asInstanceOf[SPInstObsComp].getSite.asScala.toSet
    }.fold(Set.empty[Site])(identity)

  def isObserved: Boolean =
    \/.fromTryCatchNonFatal(ObservationStatus.computeFor(obs)).exists(_ == ObservationStatus.OBSERVED)
}
