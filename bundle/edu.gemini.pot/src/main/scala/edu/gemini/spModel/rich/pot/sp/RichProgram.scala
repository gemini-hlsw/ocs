package edu.gemini.spModel.rich.pot.sp

import edu.gemini.pot.sp._
import edu.gemini.spModel.gemini.obscomp.SPProgram
import scala.collection.JavaConverters._

class RichProgram(prog:ISPProgram) {
  def spProgram:Option[SPProgram] = prog.dataObject.map(_.asInstanceOf[SPProgram])

  def update(f:SPProgram => Unit): Unit =
    spProgram.foreach { dataObj =>
      f(dataObj)
      prog.dataObject = dataObj
    }

  def templateGroups: List[ISPTemplateGroup] =
    Option(prog.getTemplateFolder).toList.flatMap(_.getTemplateGroups.asScala.toList)

  private def allObs(oc: ISPObservationContainer): List[ISPObservation] =
    oc.getAllObservations.asScala.toList

  def allObservations: List[ISPObservation] =
    allObs(prog)

  def allObservationsIncludingTemplateObservations: List[ISPObservation] =
    (List(allObs(prog))/:templateGroups) { (l, tg) =>
      allObs(tg) :: l
    }.flatten

  def obsByLibraryId(lid:String):Either[String, ISPObservation] =
    allObservations.find(_.libraryId.exists(_ == lid)).toRight(s"Observation with library id '$lid' was not found.")

  def obsByLibraryIds(lids:Seq[String]):Either[String, List[ISPObservation]] = {
    val empty:Either[String, List[ISPObservation]] = Right(Nil)
    (empty/:lids) { (eos, lid) =>
      for {
        os <- eos.right
        o <- obsByLibraryId(lid).right
      } yield o :: os
    }
  }
}
