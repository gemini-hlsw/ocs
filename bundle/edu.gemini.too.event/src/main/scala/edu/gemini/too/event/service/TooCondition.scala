package edu.gemini.too.event.service

import edu.gemini.pot.spdb.IDBTriggerCondition
import edu.gemini.pot.sp.{ISPObservation, SPUtil, SPCompositeChange}
import edu.gemini.spModel.obs.{ObsClassService, ObservationStatus, SPObservation}
import edu.gemini.spModel.obs.ObsPhase2Status.ON_HOLD
import edu.gemini.spModel.obs.ObservationStatus.READY
import edu.gemini.spModel.obsclass.ObsClass.SCIENCE
import edu.gemini.spModel.too.Too


/**
 * Condition that must be met in order to register a ToO event. Namely, an
 * observation's status must be transitioned from `ON_HOLD` to `READY` and be
 * a ToO observation.
 *
 * (REL-566: skip acquisition and calibration observations as well.)
 *
 * This condition is registered with the database such that when it occurs, the
 * [[edu.gemini.too.event.service.TooService]] is executed to record the event.
 */
object TooCondition extends IDBTriggerCondition {
  def triggeredObservation(change: SPCompositeChange): Option[ISPObservation] = {
    def castIf[T:Manifest](o: Object): Option[T] =
      for { t <- Option(o) if manifest[T].runtimeClass.isInstance(t) } yield t.asInstanceOf[T]

    def dataObject(dataObj: Object): Option[SPObservation] =
      castIf[SPObservation](dataObj)

    def observation(change: SPCompositeChange): Option[ISPObservation] =
      castIf[ISPObservation](change.getModifiedNode)

    def meritsAnAlert(o: ISPObservation): Boolean =
      Too.isToo(o)                                   &&
        ObservationStatus.computeFor(o)   == READY   &&
        ObsClassService.lookupObsClass(o) == SCIENCE

    if (change.getPropertyName == SPUtil.getDataObjectPropertyName)
      for {
        obs <- observation(change) if meritsAnAlert(obs)
        o   <- dataObject(change.getOldValue) if o.getPhase2Status == ON_HOLD
      } yield obs
    else
      None
  }

  override def matches(change: SPCompositeChange): ISPObservation =
    triggeredObservation(change).orNull
}