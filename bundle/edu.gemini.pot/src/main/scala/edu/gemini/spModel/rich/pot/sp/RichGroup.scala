package edu.gemini.spModel.rich.pot.sp

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.obscomp.SPGroup

import scala.collection.JavaConverters._

/**
 * Adds convenience to ISPGroup
 */
class RichGroup(grp: ISPGroup) {

  def spGroup: Option[SPGroup] = grp.dataObject.map(_.asInstanceOf[SPGroup])

  def update(f: SPGroup => Unit) {
    spGroup foreach { dataObj =>
      f(dataObj)
      grp.dataObject = dataObj
    }
  }

  def libraryId: Option[String] =
    for {
      g <- spGroup
      l <- Option(g.getLibraryId)
    } yield l

  def findObservation(pred: ISPObservation => Boolean): Option[ISPObservation] =
    for {
      lst <- Option(grp.getAllObservations)
      obs <- lst.asScala.find(pred)
    } yield obs
}
