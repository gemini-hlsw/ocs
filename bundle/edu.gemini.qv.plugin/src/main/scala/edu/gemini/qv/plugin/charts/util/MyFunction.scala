package edu.gemini.qv.plugin.charts.util

import edu.gemini.util.skycalc.calc._
import edu.gemini.util.skycalc.Night
import edu.gemini.spModel.core.Site

trait MyFunction {

  def defined: Solution
  def times: Vector[Long]
  def valueAt(t: Long): Double

  protected def valueAt(t: Long, t0: Long, v0: Double, t1: Long, v1: Double): Double = {
    val v = v0 + (t - t0).toDouble/(t1 - t0) * (v1 - v0)
    require((v0 >= v1 && v0 >= v && v >= v1) || (v0 < v1 && v0 <= v && v <= v1))
    v
  }

}

class NightlyFunction(nights: Seq[Night], f: Night => Double) extends MyFunction {
  require(nights.size > 0)
  val times = nights.map(_.middleNightTime).toVector
  val values = nights.map(f).toVector
  val defined = Solution(Interval(nights.head.start, nights.last.end))

  def valueAt(t: Long): Double = {
    val leftVal = left(t)
    val rightVal = right(t)
    (leftVal, rightVal) match {
      case (Some((t0, v0)), Some((t1, v1))) => valueAt(t, t0, v0, t1, v1)
      case (None,           Some((t1, v1))) => v1
      case (Some((t0, v0)), None)           => v0
      case (None,           None)           => throw new RuntimeException("??")
    }
  }

  protected def left(t: Long) = times.zipWithIndex.reverse.find(_._1 <= t).map({ case (t,ix) => (t, values(ix))})
  protected def right(t: Long) = times.zipWithIndex.find(_._1 > t).map({ case (t,ix) => (t, values(ix))})

}

class NightlyOptionalFunction(nights: Seq[Night], site: Site, f: Night => Option[Double]) extends MyFunction {

  val times = nights.map(_.middleNightTime).toVector
  val values = nights.map(f).toVector
  val intervals = nights.zip(values).filter(_._2.isDefined).map(_._1.interval)
  val defined = Solution(Seq(intervals:_*)).allDay(site.timezone)

  val samples = times.size
  val start = defined.earliest.getOrElse(Long.MinValue)
  val end = defined.latest.getOrElse(Long.MaxValue)

  // ===
  // Some relevant preconditions:
  // 1) all values inside of definition intervals must be defined!
  // 2) vice versa all values outside of definition intervals must be empty!
  // 3) for each defined interval there must be at least one value!
  times.zip(values).foreach { case (t, v) =>
    if (defined.contains(t)) require(v.isDefined) else require(v.isEmpty)
  }
  defined.intervals.foreach { i =>
    require(times.exists(t => t >= i.start && t < i.end))
  }
  // ===

  def valueAt(t: Long): Double = {
    require(t >= start)
    require(t <= end)
    val left = times.zipWithIndex.reverse.find(_._1 <= t).map({ case (t,ix) => (t, values(ix))})
    val right = times.zipWithIndex.find(_._1 > t).map({ case (t,ix) => (t, values(ix))})
    (left, right) match {
      case (Some((t0, Some(v0))), Some((t1, Some(v1)))) => valueAt(t, t0, v0, t1, v1)
      case (Some((t0, Some(v0))), Some((t1, None))) => v0
      case (Some((t0, None)),     Some((t1, Some(v1)))) => v1
      case (Some((t0, None)),     Some((t1, None))) => throw new RuntimeException("??")
      case (Some((t0, v0)), None)           => v0.get
      case (None,           Some((t1, v1))) => v1.get
      case (None, None)                     => throw new RuntimeException("??")
    }
  }

}
