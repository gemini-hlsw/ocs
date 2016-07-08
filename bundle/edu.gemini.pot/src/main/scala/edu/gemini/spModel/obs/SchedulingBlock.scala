package edu.gemini.spModel.obs

import edu.gemini.skycalc.ObservingNight
import edu.gemini.spModel.core.Site

/** A reference time for planning observations. */
sealed abstract class SchedulingBlock {

  /** A point in time. */
  def start: Long

  /** An optional duration, positive if explicit, negative if computed. */
  def duration: Option[Long]

  /** Duration, if any, otherwise zero. */
  def durationOrZero = duration.getOrElse(0L)

  /** Observing night of the scheduling block start, at the given site. */
  def observingNight(site: Site): ObservingNight =
    new ObservingNight(site, start)

  /** True if these blocks fall on the same observing night. */
  def sameObservingNightAs(other: SchedulingBlock): Boolean =
    observingNight(Site.GN) == other.observingNight(Site.GN) // site is arbitrary

}

object SchedulingBlock {

  // Private impl allows us to have default equality and so on with smart constructors
  private case class Impl(val start: Long, val duration: Option[Long]) extends SchedulingBlock

  // N.B. yes, return type should be Some[...]
  def unapply(s: SchedulingBlock): Some[(Long, Option[Long])] =
    Some((s.start, s.duration))

  def apply(start: Long): SchedulingBlock =
    apply(start, None)

  def apply(start: Long, duration: Long): SchedulingBlock =
    apply(start, Some(duration))

  def apply(start: Long, duration: Option[Long]): SchedulingBlock =
    new Impl(start, duration)

  def unsafeFromStrings(startString: String, durationString: String): SchedulingBlock =
    apply(startString.toLong, durationString.toLong)

  def unsafeFromStrings(startString: String): SchedulingBlock =
    apply(startString.toLong)

}
