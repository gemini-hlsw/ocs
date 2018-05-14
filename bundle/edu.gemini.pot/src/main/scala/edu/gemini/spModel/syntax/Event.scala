package edu.gemini.spModel.syntax

import edu.gemini.skycalc.ObservingNight
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.event.{OverlapEvent, ObsExecEvent}

import java.time.Instant


final class ObsExecEventOps(self: ObsExecEvent) {
  def timestamp: Long =
    self.getTimestamp

  def instant: Instant =
    Instant.ofEpochMilli(timestamp)

  /**
   * Gets the Site associated with the event, if it can be determined.
   */
  def site: Option[Site] =
    Option(self.getObsId.getProgramID.site)

  /**
   * Gets the observing night on which the event falls, if it can be
   * determined.
   */
  def night: Option[ObservingNight] =
    site.map(new ObservingNight(_, self.getTimestamp))

  /**
   * Determines whether this event is an overlap event.  The overlap event
   * drives special treatment of subsequent events.  Overlap signifies that
   * another observation is running simultaneously and bearing the cost of
   * observing.
   */
  def isOverlap: Boolean =
    self match {
      case _: OverlapEvent => true
      case _               => false
    }

}

trait ToObsExecEventOps {
  implicit def ToObsExecEventOps(e: ObsExecEvent): ObsExecEventOps =
    new ObsExecEventOps(e)
}

object event extends ToObsExecEventOps
