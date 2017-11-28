package edu.gemini.util.skycalc.calc

import java.util.TimeZone

import edu.gemini.shared.util.DateTimeUtils

/**
 * Representation of an interval between two points in time, including the start time and excluding the end time
 * (i.e. the interval [start, end)) and a set of operations on top of these intervals. Note that an interval
 * can not represent a single point in time (i.e. start == end) because such an interval could not contain any
 * time t for which t >= start && t < end.
 * Note: This is a bare bones implementation aimed at replacing {@see edu.gemini.skycalc.util.Interval}. Add whichever
 * functionality you need from that class here to make the "old" one obsolete.
 */
case class Interval(start: Long, end: Long) extends Ordered[Interval] {
  require(start < end, "start of interval must be < end")

  /** True if this interval covers time t. */
  def contains(t: Long): Boolean = t >= start && t < end

  /** True if this interval covers the given interval. */
  def contains(i: Interval): Boolean = i.start >= start && i.end <= end

  /** True if this and the other interval abut each other. */
  def abuts(other: Interval): Boolean =
    start == other.end || other.start == end

  /** True if this and the other interval overlap each other either fully or partially. */
  def overlaps(other: Interval): Boolean =
    start < other.end && end > other.start

  /** The duration of this interval in milliseconds. */
  def duration: Long = end - start

  /** Adds an interval to this interval. This operation is only defined if the two intervals overlap
    * or abut each, i.e. in all cases where adding the two intervals results in one single interval.
    * @param other
    * @return
    */
  def plus(other: Interval): Interval = {
    require(overlaps(other) || abuts(other))
    val s = Math.min(start, other.start)
    val e = Math.max(end, other.end)
    Interval(s, e)
  }

  /** The overlapping part of two intervals. */
  def overlap(other: Interval): Interval = {
    require(overlaps(other))
    val s = Math.max(start, other.start)
    val e = Math.min(end, other.end)
    Interval(s, e)
  }

  /** Compares to intervals and orders them by their start time. */
  def compare(that: Interval): Int = {
    (this.start - that.start).toInt
  }

  /** Gets duration of interval as hours. */
  def asHours: Double = duration.toDouble / DateTimeUtils.MillisecondsPerHour

  /** Gets duration of interval as days. */
  def asDays: Double = duration.toDouble / DateTimeUtils.MillisecondsPerDay
}

object Interval {

  def combine(left: Seq[Interval], right: Seq[Interval]): Seq[Interval] = {
    if (left.isEmpty && right.isEmpty) Nil
    else if (left.isEmpty) right
    else if (right.isEmpty) left
    else if (left.head.abuts(right.head) || left.head.overlaps(right.head)) {
      val start = Math.min(left.head.start, right.head.start)
      val end = Math.max(left.head.end, right.head.end)
      combine(new Interval(start, end) +: left.tail, right.tail)
    }
    else if (left.head.start < right.head.start)
      left.head +: combine(left.tail, right)
    else
      right.head +: combine(left, right.tail)
  }

  def intersect(left: Seq[Interval], right: Seq[Interval]): Seq[Interval] =
    if (left.isEmpty || right.isEmpty) Nil
    else {
      val h1 = left.head
      val h2 = right.head

      if (!h1.overlaps(h2)) {
        if (h1.end > h2.end)
          intersect(left, right.tail)
        else
          intersect(left.tail, right)

      } else {
        if (h1.end > h2.end)
          h1.overlap(h2) +: intersect(left, right.tail)
        else
          h1.overlap(h2) +: intersect(left.tail, right)
      }
    }

  def reduce(left: Interval, right: Seq[Interval]): Seq[Interval] =
    reduce(Seq(left), right)

  def reduce(left: Seq[Interval], right: Seq[Interval]): Seq[Interval] =
    if (left.isEmpty) Nil
    else
      if (right.isEmpty) left
      else {
        val h1 = left.head
        val h2 = right.head
        if (!h1.overlaps(h2)) {
          if (h1.end <= h2.start) h1 +: reduce(left.tail, right)  // no overlap and h1 is before h2 => h1 won't be touched again by any h2, add it to result
          else reduce(left, right.tail)                           // no overlap and h1 is after h2 => we can skip h2
        } else {
          reduce(reduce(h1, h2) ++ left.tail, right)              // overlap: replace h1 with reduce(h1,h2) and continue
        }
      }

  def reduce(i1: Interval, i2: Interval): Seq[Interval] =
    if (i1.start < i2.start && i1.end > i2.end) Seq(Interval(i1.start, i2.start), Interval(i2.end, i1.end))
    else if (i1.start < i2.start && i1.end <= i2.end) Seq(Interval(i1.start, i2.start))
    else if (i1.start >= i2.start && i1.end > i2.end) Seq(Interval(i2.end, i1.end))
    else Seq()

  def invert(intervals: Seq[Interval]): Seq[Interval] =
    if (intervals.size < 2) Seq()
    else
      intervals.sliding(2).map({
        case Seq(i, j) => Interval(i.end, j.start)
      }).toSeq

  /**
   * Takes a sequence of intervals and transforms it into a sequence of full days (i.e. 14:00 first day to 14:00
   * on the next day) that covers all given intervals. Abutting full days are combined so that the resulting
   * sequence contains the minimal number of intervals needed to cover the original sequence.
   * @param intervals
   * @param localTime
   * @return
   */
  def allDay(intervals: Seq[Interval], localTime: TimeZone): Seq[Interval] = {
    // blow intervals up to cover 24hrs (or multiples thereof); days start/end at 14hrs local time
    def blowUp(interval: Interval): Interval =
      Interval(
        DateTimeUtils.startOfDayInMs(interval.start, localTime.toZoneId),
        DateTimeUtils.endOfDayInMs  (interval.end,   localTime.toZoneId)
      )

    // note that a single interval can stretch several days (e.g. for time windows)
    def removeDuplicates(res: Seq[Interval], intervals: Seq[Interval]): Seq[Interval] = {
      intervals match {
        case Nil => res
        case head::Nil =>
          res :+ head
        case head::tail =>
          val h = head
          val t = tail.head
          if (h.abuts(t) || h.overlaps(t)) {
            removeDuplicates(res, h.plus(t) +: tail.drop(1))
          } else {
            removeDuplicates(res :+ h, tail)
          }
      }
    }

    removeDuplicates(Seq(), intervals.map(blowUp))

  }



}
