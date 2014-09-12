package edu.gemini.rollover.servlet

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.obs.{ObservationStatus, ObsClassService}
import edu.gemini.spModel.obsclass.ObsClass.SCIENCE

/**
 * A predicate that determines whether a given observation should be
 * considered when making the rollover report.  Observations that should be
 * included are
 *
 * <ul>
 * <li>Science observations</li>
 * <li>Not yet fully observed according to their observation status</li>
 * </ul>
 */
object IncludeObservation extends (ISPObservation => Boolean) with Serializable {

  private def isScience(obsShell: ISPObservation): Boolean =
    ObsClassService.lookupObsClass(obsShell) == SCIENCE

  private def isActive(obsShell: ISPObservation): Boolean =
    ObservationStatus.computeFor(obsShell).isActive

  def apply(obsShell: ISPObservation): Boolean =
    isScience(obsShell) && isActive(obsShell)
}