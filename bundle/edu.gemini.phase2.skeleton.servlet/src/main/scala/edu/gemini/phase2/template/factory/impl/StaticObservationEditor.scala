package edu.gemini.phase2.template.factory.impl

import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.pot.sp.{ISPObsComponent, SPComponentType, ISPObservation}
import edu.gemini.spModel.rich.pot.sp._

case class StaticObservationEditor[I <: ISPDataObject](obs: ISPObservation, instType: SPComponentType) {
  def instrument: Maybe[ISPObsComponent] =
    obs.findObsComponentByType(instType).toRight("Observation '%s' does not have an instrument of type '%s'".format(obs.libraryId, instType.readableStr))

  private def instrumentDataObject(i: ISPObsComponent): Maybe[I] =
    for {
      dobj <- i.dataObject.map(_.asInstanceOf[I]).toRight("Observation '%s' has an instrument but it is empty").right
    } yield dobj

  def instrumentDataObject: Maybe[I] =
    for {
      inst <- instrument.right
      dobj <- instrumentDataObject(inst).right
    } yield dobj

  def updateInstrument(up: I => Unit): Maybe[Unit] =
    for {
      inst <- instrument.right
      dobj <- instrumentDataObject(inst).right
    } yield {
      up(dobj)
      inst.setDataObject(dobj)
    }
}
