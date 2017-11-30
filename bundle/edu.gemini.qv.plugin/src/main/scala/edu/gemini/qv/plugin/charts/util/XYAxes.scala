package edu.gemini.qv.plugin.charts.util

import edu.gemini.qv.plugin.selector.OptionsSelector._
import edu.gemini.shared.util.DateTimeUtils
import org.jfree.chart.axis.{Axis, NumberAxis, NumberTickUnit, ValueAxis}
import org.jfree.chart.plot.XYPlot

/**
 * Definitions of axes used in different xy plots and some utility functions around them.
 * Note: Using this as a trait instead of an object because sharing axes between plots introduces memory leaks
 * (it seems the axes keep a pointer to all plots in which they are used (?)), so we need to make sure every
 * plot has its own set of axes to work with.
 */
trait XYAxes {

  // the main or primary axes, one of them is going to be shown to the left of the plot
  object MainHourAxis extends NumberAxis("Hours Between Nautical Twilights") {
    setAutoRange(false)
    setRange(0, DateTimeUtils.StartOfDayHour)
  }
  object MainRiseTimeAxis extends NumberAxis("Rise Time Between Nautical Twilights") {
    setAutoRange(false)
    setInverted(true)
    setTickUnit(new HourOfDayTickUnit(1))
    setRange(17, 32) // 17:00 - 08:00 next day
  }
  object MainSetTimeAxis extends NumberAxis("Set Time Between Nautical Twilights") {
    setAutoRange(false)
    setInverted(true)
    setTickUnit(new HourOfDayTickUnit(1))
    setRange(17, 32) // 17:00 - 08:00 next day
  }
  object MainElevationAxis extends NumberAxis("Elevation") {
    setAutoRange(false)
    setTickUnit(new DegreesTickUnit(10))
    setRange(0, 90)
  }
  object MainMidNightElevationAxis extends NumberAxis("Middle Nighttime Elevation") {
    setAutoRange(false)
    setTickUnit(new DegreesTickUnit(10))
    setRange(0, 90)
  }

  // this is a bit of a special case, this axis can replace the elevation main axis or additional elevation axes
  object AirmassAxis extends NumberAxis("Airmass") {
    setAutoRange(false)
    setTickUnit(new AirmassTickUnit(10))
    setRange(0, 90)
  }
  object MidNightAirmassAxis extends NumberAxis("Middle Nighttime Airmass") {
    setAutoRange(false)
    setTickUnit(new AirmassTickUnit(10))
    setRange(0, 90)
  }

  // a set of additional secondary axes, these will be shown to the right of the plot if needed
  object ElevationAxis extends NumberAxis("Elevation") {
    setAutoRange(false)
    setTickUnit(new DegreesTickUnit(10))
    setRange(0, 90)
  }
  object ParallacticAngleAxis extends NumberAxis("Parallactic Angle") {
    setAutoRange(false)
    setTickUnit(new DegreesTickUnit(30))
    setRange(-180, 180)
  }
  object SkyBrightnessAxis extends NumberAxis("Sky Brightness") {
    setAutoRange(false)
    setInverted(true)
    setTickUnit(new NumberTickUnit(1))
    setRange(17, 22)
  }
  object LunarDistanceAxis extends NumberAxis("Lunar Distance") {
    setAutoRange(false)
    setTickUnit(new DegreesTickUnit(20))
    setRange(0, 180)
  }
  object HourAngleAxis extends NumberAxis("Hour Angle") {
    setAutoRange(false)
    setTickUnit(new NumberTickUnit(2))
    setRange(-12, 12)
  }

  def axisFor(curve: ChartOption, options: Set[ChartOption]): ValueAxis = {
    curve match {
      case ElevationCurve => if (options.contains(AirmassRuler)) AirmassAxis else ElevationAxis
      case ParallacticAngleCurve => ParallacticAngleAxis
      case SkyBrightnessCurve => SkyBrightnessAxis
      case LunarDistanceCurve => LunarDistanceAxis
      case HourAngleCurve => HourAngleAxis
    }
  }

  def findAxis(plot: XYPlot, axis: Axis): Option[Int] = {
    for (i <- 0 to plot.getDatasetCount - 1) {
      if (plot.getRangeAxis(i) == axis) return Some(i)
    }
    None
  }

  private class DegreesTickUnit(size: Double) extends NumberTickUnit(size) {
    override def valueToString(v: Double) = f"$v%.0f°"
  }

  private class HourOfDayTickUnit(size: Double) extends NumberTickUnit(size) {
    override def valueToString(hrs: Double) = f"${hrs%24}%.0f"
  }

  private class AirmassTickUnit(size: Double) extends NumberTickUnit(size) {
    override def valueToString(value: Double) = value match {
      case v if v == 90 => "1.00"
      case v if v == 80 => "1.02"
      case v if v == 70 => "1.06"
      case v if v == 60 => "1.15"
      case v if v == 50 => "1.30"
      case v if v == 40 => "1.55"
      case v if v == 30 => "2.00"
      case v if v == 20 => "2.90"
      case v if v == 10 => "5.55"
      case _ => "∞"
    }
  }

}
