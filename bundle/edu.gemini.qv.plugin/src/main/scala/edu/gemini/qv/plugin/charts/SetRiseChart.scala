package edu.gemini.qv.plugin.charts

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.charts.util._
import edu.gemini.qv.plugin.filter.core.Filter
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.selector.OptionsSelector.{ShowRiseTimeOption, MoonSetRise}
import edu.gemini.qv.plugin.selector.{TimeRangeSelector, OptionsSelector, ConstraintsSelector}
import edu.gemini.qv.plugin.ui.QvGui
import edu.gemini.qv.plugin.util.ConstraintsCache.{Elevation, AboveHorizon}
import edu.gemini.qv.plugin.util.SolutionProvider
import edu.gemini.qv.plugin.util.SolutionProvider.ConstraintType
import edu.gemini.skycalc.TimeUtils
import edu.gemini.util.skycalc.Night
import edu.gemini.util.skycalc.calc.Solution
import java.awt.{BasicStroke, Color}
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.DateAxis
import org.jfree.chart.plot.XYPlot
import scala.Some

/**
 */
class SetRiseChart(val ctx: QvContext, val nights: Seq[Night], val observations: Set[Obs], activeFilters: Set[Filter], inactiveFilters: Set[Filter], timeControl: TimeRangeSelector, val constraints: ConstraintsSelector, val details: OptionsSelector) extends VisibilityXYChart {

  val site = ctx.site

  def createChart: JFreeChart = {

    // init
    val chart = QvChartFactory.createTimelineChart(title())
    val plot = chart.getPlot.asInstanceOf[XYPlot]
    initDateAxis(plot.getDomainAxis.asInstanceOf[DateAxis], nights, timeControl.selectedZone)

    // main curves and axis
    plot.setRangeAxis(mainAxis)
    val orderedObs = observations.toSeq
    val plotter = new XYPlotter(ctx, nights, constraints, details, plot)
    val colorCoding = ColorCoding(activeFilters, inactiveFilters)

    val renderer1 = XYPlotter.lineRenderer(Color.gray, new BasicStroke(6))
    plotter.plotFunction(mainAxis, renderer1, new NightlyFunction(nights, twiStartHour))
    plotter.plotFunction(mainAxis, renderer1, new NightlyFunction(nights, twiEndHour))

    val riseInRenderer = colorCoding.lineRenderer(ctx, orderedObs, SolidThickStroke)
    val riseOutRenderer = colorCoding.lineRenderer(ctx, orderedObs, SolidThinStroke, Some(Color.gray))
    val funcs = orderedObs.map(o => {
      val s =
        if (constraints.selected.contains(Elevation)) SolutionProvider(ctx).solution(nights, Set[ConstraintType](Elevation), o)
        else SolutionProvider(ctx).solution(nights, Set[ConstraintType](AboveHorizon), o)
        o -> new NightlyOptionalFunction(nights, site, riseSetTime(o, s))
    }).toMap
    plotter.plotSolution(mainAxis, orderedObs, riseInRenderer, funcs)
    plotter.plotFunction(mainAxis, orderedObs, riseOutRenderer, funcs)

    // add rise/set chart specific details
    plotSetRiseDetails(plotter)

    // add general functions and details
    val selectedObs = observations.filter(o => activeFilters.exists(f => f.predicate(o, ctx)))
    val curvesInRenderer = colorCoding.lineRenderer(ctx, orderedObs, DashedThinStroke)
    val curvesOutRenderer = colorCoding.lineRenderer(ctx, orderedObs, DashedThinStroke, Some(Color.gray))
    plotter.plotCurves(selectedObs.toSeq, details.selected, curvesInRenderer, curvesOutRenderer)
    plotter.plotOptions(selectedObs.toSeq, details.selected, curvesInRenderer, curvesOutRenderer)

    addDetails(plot, selectedObs, timeControl.daysShowing)

    addLegend(plot, observations, colorCoding)

    chart
  }

  protected def title(): String =
    if (showRiseTimes) super.title("Rise Times for") else super.title("Set Times for")

  private val mainAxis =
    if (showRiseTimes) MainRiseTimeAxis else MainSetTimeAxis

  private def plotSetRiseDetails(plotter: XYPlotter): Unit =
    details.selected.foreach {
      case MoonSetRise =>
        val renderer = XYPlotter.lineRenderer(QvGui.MoonColor, SolidThickStroke)
        plotter.plotFunction(mainAxis, renderer, new NightlyOptionalFunction(nights, site, moonSetRise))
      case _ => // Ignore
    }

  private def showRiseTimes = details.isSelected(ShowRiseTimeOption)

  private def boundByTwilight(n: Night, t: Long) = Math.max(Math.min(t, n.nauticalTwilightEnd), n.nauticalTwilightStart)
  private def twiStartHour(n: Night): Double = hourOfNight(n.nauticalTwilightStart)
  private def twiEndHour(n: Night): Double = hourOfNight(n.nauticalTwilightEnd)

  private def riseSetTime(o: Obs, s: Solution)(n: Night): Option[Double] = {
    val intervals = s.restrictTo(n.interval).intervals
    if (showRiseTimes) intervals.find(_.start > n.nauticalTwilightStart).map(i => hourOfNight(i.start))
    else intervals.find(_.end < n.nauticalTwilightEnd).map(i => hourOfNight(i.end))
  }

  private def moonSetRise(n: Night): Option[Double] =
    if (showRiseTimes) n.moonRise.map(r => hourOfNight(boundByTwilight(n, r)))
    else n.moonSet.map(s => hourOfNight(boundByTwilight(n, s)))

  private def hourOfNight(t: Long): Double = {
    val hourOfDay = TimeUtils.asHours(TimeUtils.millisecondOfDay(t, site.timezone()))
    if (hourOfDay < 14) hourOfDay + 24 else hourOfDay // map morning hours onto the same day as hours 24,25,26 etc
  }


}
