package edu.gemini.spModel.syntax

import edu.gemini.skycalc.ObservingNight

final class ObservingNightOps(val self: ObservingNight) {

  /**
   * Stream of all nights starting with this night.
   */
  def stream: Stream[ObservingNight] =
    self #:: new ObservingNightOps(self.next).stream

  /**
   * List of observing nights starting with this night and ending (inclusive)
   * with `end`.
   */
  def to(end: ObservingNight): List[ObservingNight] =
    stream.takeWhile(n => n.getStartTime <= end.getStartTime).toList

}

trait ToObservingNightOps {
  implicit def ToObservingNightOps(n: ObservingNight): ObservingNightOps =
    new ObservingNightOps(n)
}

object night extends ToObservingNightOps
