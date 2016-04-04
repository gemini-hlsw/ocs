package edu.gemini.spModel.obs

/** A reference time for planning observations. */
sealed abstract class SchedulingBlock {

  /** A point in time. */
  def start: Long

  /** An optional duration, always positive if present. */
  def duration: Option[Long]

  /** Duration, if any, otherwise zero. */
  def durationOrZero = duration.getOrElse(0L)

}

object SchedulingBlock {

  // Private impl allows us to have default equality and so on with smart constructors
  private case class Impl(val start: Long, val duration: Option[Long]) extends SchedulingBlock

  def apply(start: Long): SchedulingBlock =
    apply(start, None)

  def apply(start: Long, duration: Long): SchedulingBlock =
    apply(start, Some(duration))

  def apply(start: Long, duration: Option[Long]): SchedulingBlock =
    new Impl(start, duration.filter(_ > 0))

  def unsafeFromStrings(startString: String, durationString: String): SchedulingBlock =
    apply(startString.toLong, durationString.toLong)

}
