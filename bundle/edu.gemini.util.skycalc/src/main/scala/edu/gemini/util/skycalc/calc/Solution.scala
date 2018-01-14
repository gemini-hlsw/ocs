package edu.gemini.util.skycalc.calc

import java.util.TimeZone

import edu.gemini.shared.util.DateTimeUtils

/**
 * Representation of a solution for a constraint defined by an arbitrary number of intervals.
 * The intervals are sorted by their start time and don't overlap or abut, i.e. the solution is always represented
 * by the smallest possible set of intervals.
 */
case class Solution(intervals: Seq[Interval]) {

  /** True if the solution (i.e. any of its intervals) contains time t. */
  def contains(t: Long) = intervals.exists(_.contains(t))

  def never: Boolean = intervals.isEmpty
  def earliest: Option[Long] = intervals.headOption.map(_.start)
  def latest: Option[Long] = intervals.lastOption.map(_.end)
  def duration: Long = intervals.map(_.duration).sum

  /**
   * Adds a solution to this solution.
   * @param other
   * @return
   */
  def add(other: Solution): Solution =
    Solution(addIntervals(intervals, other.intervals))

  /**
   * Adds an interval to this solution.
   * @param interval
   * @return
   */
  def add(interval: Interval): Solution =
    Solution(addIntervals(intervals, Seq(interval)))

  /**
   * True if any part of this solution overlaps with the given interval.
   * @param interval
   * @return
   */
  def overlaps(interval: Interval): Boolean =
    intervals.exists(i => i.overlaps(interval))

  /**
   * Restricts a solution to only the intervals in the given interval.
   * @param interval
   * @return
   */
  def restrictTo(interval: Interval): Solution =
    Solution(
      intervals.
        filter(i => i.end >= interval.start && i.start <= interval.end).
        map(i => Interval(Math.max(i.start, interval.start), Math.min(i.end, interval.end)))
    )


  def allDay(localTime: TimeZone): Solution = {
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

    if (this == Solution.Always)
      Solution.Always                                               // Always can not be "blown" up, don't try
    else
      Solution(removeDuplicates(Seq(), intervals.map(blowUp)))

  }

  /**
   * Combines two solutions.
   * Merges all overlapping and abutting intervals.
   */
  def combine(s: Solution): Solution = Solution(Interval.combine(intervals, s.intervals))

  /**
   * Combines this solution with a sequence of ordered intervals.
   * Merges all overlapping and abutting intervals.
   * @param otherIntervals
   * @return
   */
  def combine(otherIntervals: Seq[Interval]): Solution = Solution(Interval.combine(intervals, otherIntervals))

  /**
   * Intersects a solution with another one.
   * The result will contain all intervals of this solution which are covered by both solutions.
   */
  def intersect(s: Solution): Solution = Solution(Interval.intersect(intervals, s.intervals))


  /**
   * Reduce a solution by another one.
   * The result will contain all intervals of this solution which are NOT covered by the the given solution.
   * @param s
   * @return
   */
  def reduce(s: Solution): Solution = Solution(Interval.reduce(intervals, s.intervals))

  def reduce(otherIntervals: Seq[Interval]): Solution = Solution(Interval.reduce(intervals, otherIntervals))


  // ===== helpers

  private def addIntervals(i1: Seq[Interval], i2: Seq[Interval]): Seq[Interval] =
    if (i1.isEmpty && i2.isEmpty) Seq()
    else if (i1.nonEmpty && i2.isEmpty)  i1
    else if (i1.isEmpty  && i2.nonEmpty) i2
    else {
      if (i1.last.abuts(i2.head)) (i1.dropRight(1) :+ i1.last.plus(i2.head)) ++ i2.tail
      else i1 ++ i2
    }

}


/**
 * Companion objects with convenience constructors.
 */
object Solution {
  /** Solution that is always true (i.e. for any time t). */
  val Always = new Solution(Seq(Interval(0, Long.MaxValue)))

  /** Solution that is never true. */
  val Never = Solution()

  /** Convenience constructors. */
  def apply(): Solution = new Solution(Seq())
  def apply(start: Long, end: Long) = new Solution(Seq(Interval(start, end)))
  def apply(interval: Interval): Solution = apply(Seq(interval))
}
