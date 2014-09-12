package edu.gemini.too.event.service

import edu.gemini.pot.spdb.IDBTriggerCondition
import edu.gemini.pot.sp.{ISPObservation, SPUtil, SPCompositeChange}
import edu.gemini.spModel.obs.{ObservationStatus, SPObservation}
import edu.gemini.spModel.obs.ObsPhase2Status.ON_HOLD
import edu.gemini.spModel.obs.ObservationStatus.READY
import edu.gemini.spModel.too.Too


/**
 * Condition that must be met in order to register a ToO event. Namely, an
 * observation's status must be transitioned from `ON_HOLD` to `READY` and be
 * a ToO observation.
 *
 * This condition is registered with the database such that when it occurs, the
 * [[edu.gemini.too.event.service.TooService]] is executed to record the event.
 */
object TooCondition extends IDBTriggerCondition {
  private def isDataObjectUpdate(change: SPCompositeChange): Boolean =
    change.getPropertyName == SPUtil.getDataObjectPropertyName

  private def castIf[T:Manifest](o: Object): Option[T] =
    for { t <- Option(o) if manifest[T].runtimeClass.isInstance(t) } yield t.asInstanceOf[T]

  private def dataObject(dataObj: Object) = castIf[SPObservation](dataObj)
  private def observation(change: SPCompositeChange) = castIf[ISPObservation](change.getModifiedNode)

  def triggeredObservation(change: SPCompositeChange): Option[ISPObservation] =
    if (isDataObjectUpdate(change)) for {
        obs <- observation(change) if Too.isToo(obs) && ObservationStatus.computeFor(obs) == READY
        o <- dataObject(change.getOldValue) if o.getPhase2Status == ON_HOLD
      } yield obs
    else None

    def matches(change: SPCompositeChange): ISPObservation =
      triggeredObservation(change).orNull
}