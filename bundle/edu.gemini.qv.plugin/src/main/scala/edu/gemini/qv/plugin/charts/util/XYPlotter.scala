package edu.gemini.qv.plugin.charts.util

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.selector.OptionsSelector._
import edu.gemini.qv.plugin.selector.{ConstraintsSelector, OptionsSelector}
import edu.gemini.qv.plugin.util.ConstraintsCache.{MaxElevation, MinElevation}
import edu.gemini.qv.plugin.util.{SemesterData, SolutionProvider}
import edu.gemini.spModel.core.{Coordinates, Site}
import edu.gemini.util.skycalc.calc.{Interval, Solution, TargetCalculator}
import edu.gemini.util.skycalc.Night
import java.awt.{Color, Stroke}
import java.time.Instant
import java.util.UUID

import edu.gemini.spModel.target.env.Asterism
import org.jfree.chart.axis.ValueAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.{XYLineAndShapeRenderer, XYSplineRenderer}
import org.jfree.data.xy.{XYSeries, XYSeriesCollection}

import scala.collection._
import scala.concurrent.duration._


/**
 * Some helper functionality to draw QV related functions on a JFreeChart XYPlot.
 */
class XYPlotter(ctx: QvContext, nights: Seq[Night], constraints: ConstraintsSelector, options: OptionsSelector, plot : XYPlot) extends XYAxes {

  val range = intervalFor(nights)
  val sampling = if (nights.size < 8) regularSampling else midNightTimeSampling
  val overSampling = if (nights.size < 8) regularSampling else overMidNightTimeSampling
  val elevationAxis =
    if (options.isSelected(AirmassRuler))
      if (nights.size > 8) MidNightAirmassAxis else AirmassAxis
    else
      if (nights.size > 8) MainMidNightElevationAxis else MainElevationAxis

  def plotCurves(obs: Seq[Obs], options: Set[ChartOption], inRenderer: XYLineAndShapeRenderer, outRenderer: XYLineAndShapeRenderer): Unit = {

    def targetCalcFor(o: Obs): TargetCalculator =
      XYPlotter.getCalculator(ctx.site, o.getTargetEnvironment.getAsterism, overSampling)

    val tcs = obs.map(o => o -> targetCalcFor(o)).toMap

    options.foreach {
      case ElevationCurve =>
        plotSolution(elevationAxis, obs, inRenderer, sampling, o => t => {
          // in order to make it more obvious where a target is visible sometime during the night but just
          // happens to be below the horizon at middle night time we draw negative elevation values during
          // solution intervals as 0
          val e = tcs(o).elevationAt(t)
          if (e > 0) e else 0
        })
      case SkyBrightnessCurve =>
        plotSolution(SkyBrightnessAxis, obs, inRenderer, sampling, o => t => tcs(o).skyBrightnessAt(t))
      case ParallacticAngleCurve =>
        plotSolution(ParallacticAngleAxis, obs, inRenderer, sampling, o => t => tcs(o).parallacticAngleAt(t))
      case LunarDistanceCurve =>
        plotSolution(LunarDistanceAxis, obs, inRenderer, sampling, o => t => tcs(o).lunarDistanceAt(t))
      case HourAngleCurve =>
        plotSolution(HourAngleAxis, obs, inRenderer, sampling, o => t => tcs(o).hourAngleAt(t))
      case _ =>                     // Ignore
    }

    options.foreach {
      case ElevationCurve =>
        plotFunction(elevationAxis, obs, outRenderer, sampling, (o, t) => tcs(o).elevationAt(t))
      case SkyBrightnessCurve =>
        plotFunction(SkyBrightnessAxis, obs, outRenderer, sampling, (o, t) => tcs(o).skyBrightnessAt(t))
      case ParallacticAngleCurve =>
        plotFunction(ParallacticAngleAxis, obs, outRenderer, sampling, (o, t) => tcs(o).parallacticAngleAt(t))
      case LunarDistanceCurve =>
        plotFunction(LunarDistanceAxis, obs, outRenderer, sampling, (o, t) => tcs(o).lunarDistanceAt(t))
      case HourAngleCurve =>
        plotFunction(HourAngleAxis, obs, outRenderer, sampling, (o, t) => tcs(o).hourAngleAt(t))
      case _ =>                     // Ignore
    }

  }

  def plotFunction(axis: ValueAxis, obs: Seq[Obs], renderer: XYLineAndShapeRenderer, sampling: Seq[Long], f: (Obs, Long) => Double): Unit = {
    val data = new XYSeriesCollection
    obs.foreach { o => plotFunction(data, o, sampling, f) }
    plotCurves(axis, data, renderer)
  }

  def plotFunction(data: XYSeriesCollection, o: Obs, sampling: Seq[Long], f: (Obs, Long) => Double): Unit = {
    val series = new XYSeries(o)
    sampling.foreach(t => series.add(t, f(o, t)))
    data.addSeries(series)
  }

  def plotFunction(axis: ValueAxis, renderer: XYLineAndShapeRenderer, f: MyFunction): Unit = {
    val data = new XYSeriesCollection
    val series = new XYSeries(UUID.randomUUID())
    plotSolution2(series, f.defined.restrictTo(range), f.times, t => f.valueAt(t))
    data.addSeries(series)
    plotCurves(axis, data, renderer)
  }

  def plotFunction(axis: ValueAxis, obs: Seq[Obs], renderer: XYLineAndShapeRenderer, f: Obs => MyFunction): Unit = {
    val data = new XYSeriesCollection
    obs.foreach { o =>
      val func = f(o)
      val series = new XYSeries(o)
      plotSolution2(series, func.defined.restrictTo(range), func.times, t => func.valueAt(t))
      data.addSeries(series)
    }
    plotCurves(axis, data, renderer)
  }

  def plotSolution(axis: ValueAxis, obs: Seq[Obs], renderer: XYLineAndShapeRenderer, f: Obs => MyFunction): Unit = {
    val data = new XYSeriesCollection
    obs.foreach { o =>
      val func = f(o)
      val s = SolutionProvider(ctx).solution(nights, constraints.selected, o).restrictTo(range)
      val solution = if (nights.size < 8) s else s.allDay(ctx.timezone)
      val series = new XYSeries(o)
      plotSolution2(series, func.defined.intersect(solution), func.times, t => func.valueAt(t))
      data.addSeries(series)
    }
    plotCurves(axis, data, renderer)
  }

  def plotSolution(axis: ValueAxis, obs: Seq[Obs], renderer: XYLineAndShapeRenderer, sampling: Seq[Long], f: Obs => Long => Double): Unit = {
    val data = new XYSeriesCollection
    obs.foreach { o =>
      val s = SolutionProvider(ctx).solution(nights, constraints.selected, o).restrictTo(range)
      val solution = if (nights.size < 8) s else s.allDay(ctx.timezone)
      val series = new XYSeries(o)
      plotSolution2(series, solution, sampling, f(o))
      data.addSeries(series)
    }
    plotCurves(axis, data, renderer)
  }

  private def plotSolution2(series: XYSeries, solution: Solution, sampling: Seq[Long], f: Long => Double): Unit = {
    solution.intervals.foreach { i =>
      series.add(i.start-1, null)
      series.add(i.start, f(i.start))
      sampling.filter(t => t > i.start && t < i.end).foreach { t =>
        series.add(t, f(t))
      }
      series.add(i.end, f(i.end))
      series.add(i.end+1, null)
    }
  }

  /** Adds all currently selected options. */
  def plotOptions(obs: Seq[Obs], details: Set[ChartOption], inRenderer: XYLineAndShapeRenderer, outRenderer: XYLineAndShapeRenderer): Unit = {
    if (details.contains(MinElevationCurve))
      plotFunction(elevationAxis, obs, inRenderer,  o => new NightlyFunction(nights, n => SolutionProvider(ctx).value(MinElevation, n, o)))
    if (details.contains(MaxElevationCurve))
      plotFunction(elevationAxis, obs, inRenderer,  o => new NightlyFunction(nights, n => SolutionProvider(ctx).value(MaxElevation, n, o)))

    if (details.contains(MinElevationCurve))
      plotSolution(elevationAxis, obs, outRenderer,  o => new NightlyFunction(nights, n => SolutionProvider(ctx).value(MinElevation, n, o)))
    if (details.contains(MaxElevationCurve))
      plotSolution(elevationAxis, obs, outRenderer,  o => new NightlyFunction(nights, n => SolutionProvider(ctx).value(MaxElevation, n, o)))
  }

  /**
   * Adds a set of curves to the plot.
   * All curves use the given axis as their range axis; the axis is added to the plot if needed.
   * The renderer finally assigns the colors to each curve (i.e. for each data series in the series collection).
   *
   * @param axis
   * @param data
   * @param renderer
   */
  def plotCurves(axis: ValueAxis, data: XYSeriesCollection, renderer: XYLineAndShapeRenderer): Unit = {
    val ix = plot.getDatasetCount
    val existingAxis = findAxis(plot, axis)
    val axisIndex =
      if (existingAxis.isDefined) existingAxis.get
      else { plot.setRangeAxis(ix, axis); ix }

    plot.setDataset(ix, data)
    plot.setRenderer(ix, renderer)
    plot.mapDatasetToRangeAxis(ix, axisIndex)
  }

  private def intervalFor(nights: Seq[Night]): Interval = {
    require(nights.size > 0)
    Interval(nights.head.start, nights.last.end)
  }

  // === Sampling rates

  private def regularSampling: Vector[Long] = {
    val rate = (range.end - range.start) / 200
    val times = for (i <- 0 to 200) yield range.start + (i * rate)
    times.toVector
  }

  private def  midNightTimeSampling: Vector[Long] = {
    require(nights.size > 0)
    val times =
      if (nights.size > 600) nights.sliding(1,4).flatten.map(_.middleNightTime).toVector
      else if (nights.size > 300) nights.sliding(1,3).flatten.map(_.middleNightTime).toVector
      else if (nights.size > 150) nights.sliding(1,2).flatten.map(_.middleNightTime).toVector
      else nights.map(_.middleNightTime).toVector
    times
  }

  private def overMidNightTimeSampling: Vector[Long] = {
    require(nights.size > 0)
    val allNights = SemesterData.nights(ctx.site, ctx.range)
    val times = (allNights.head.middleNightTime - 1.day.toMillis) +:
      allNights.map(_.middleNightTime) :+
      (allNights.last.middleNightTime + 1.day.toMillis)
    times.toVector
  }

}

object XYPlotter {

  def lineRenderer(color: Color, stroke: Stroke, count: Int = 1) = new XYLineAndShapeRenderer() {
    setBaseShapesVisible(false)
    for (ix <- 0 to count-1) {
      setSeriesPaint(ix, color)
      setSeriesStroke(ix, stroke)
    }
  }

  def splineRenderer(color: Color, stroke: Stroke, count: Int = 1) = new XYSplineRenderer() {
    setBaseShapesVisible(false)
    for (ix <- 0 to count-1) {
      setSeriesPaint(ix, color)
      setSeriesStroke(ix, stroke)
    }
  }


  // Target Calc cache!!
  case class CalcKey(site: Site, asterism: Asterism, sampling: Vector[Long])
  case class TimedKey(t: Long, key: CalcKey) extends Ordered[TimedKey] {
    def compare(that: TimedKey): Int = (this.t - that.t).toInt
  }
  private val calcCache = concurrent.TrieMap[CalcKey, TargetCalculator]()
  private val calcAge = mutable.SortedSet[TimedKey]()

  def getCalculator(site: Site, asterism: Asterism, sampling: Vector[Long]): TargetCalculator = {
    val key = CalcKey(site, asterism, sampling)
    calcCache.getOrElseUpdate(key, {
      if (calcCache.size >= 500) {
        val oldest = calcAge.head
        calcAge.remove(oldest)
        calcCache.remove(oldest.key)
      }
      val tc = TargetCalculator(site, (t: Long) => asterism.basePosition(Some(Instant.ofEpochMilli(t))).getOrElse(Coordinates.zero), sampling)
      val ts = System.currentTimeMillis()
      calcAge.add(new TimedKey(ts, key))
      calcCache.put(key, tc)
      tc
    })

  }
}
