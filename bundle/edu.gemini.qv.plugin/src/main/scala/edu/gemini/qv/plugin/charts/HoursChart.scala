package edu.gemini.qv.plugin.charts

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.chart.Axis
import edu.gemini.qv.plugin.charts.util._
import edu.gemini.qv.plugin.filter.core.Filter
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.selector.OptionsSelector.MoonHours
import edu.gemini.qv.plugin.selector.{ConstraintsSelector, OptionsSelector, TimeRangeSelector}
import edu.gemini.qv.plugin.ui.QvGui
import edu.gemini.qv.plugin.util.SolutionProvider
import edu.gemini.util.skycalc.Night
import edu.gemini.util.skycalc.calc.Solution
import java.awt.{BasicStroke, Color}

import edu.gemini.shared.util.DateTimeUtils
import org.jfree.chart._
import org.jfree.chart.axis.DateAxis
import org.jfree.chart.plot.XYPlot

import scala.concurrent.duration._
import scalaz._
import Scalaz._

/**
 * Bar charts are essentially one form of visibility plots which show when an observation (or a group of observations)
 * is observable (visible) taking all restrictions into account like elevation limits, timing windows etc.
 */
case class HoursChart(ctx: QvContext, nights: Seq[Night], observations: Set[Obs], activeFilters: Set[Filter], inactiveFilters: Set[Filter], timeControl: TimeRangeSelector, constraints: ConstraintsSelector, details: OptionsSelector, colorCodingAxis: Axis) extends VisibilityXYChart {


  val site = ctx.site


  def createChart: JFreeChart = {

    val chart = QvChartFactory.createTimelineChart(title("Hours for"))
    val plot = chart.getPlot.asInstanceOf[XYPlot]
    plot.setRangeAxis(MainHourAxis)
    initDateAxis(plot.getDomainAxis.asInstanceOf[DateAxis], nights, timeControl.selectedZone)

    val orderedObs = observations.toSeq
    val selectedObs = observations.filter(o => activeFilters.exists(f => f.predicate(o, ctx))).toSeq
    val colorCoding = ColorCoding(activeFilters, inactiveFilters)

    val plotter = new XYPlotter(ctx, nights, constraints, details, plot)

    // main curve: hours between nautical twilights (science time)
    val renderer1 = XYPlotter.lineRenderer(Color.gray, new BasicStroke(6))
    plotter.plotFunction(MainHourAxis, renderer1, new NightlyFunction(nights, n => n.scienceTime.duration.milliseconds.toHours))
    val riseInRenderer = colorCoding.lineRenderer(ctx, orderedObs, SolidThickStroke)
    val riseOutRenderer = colorCoding.lineRenderer(ctx, orderedObs, SolidThinStroke, Some(Color.gray))
    val funcs = orderedObs.map(o => {
      val s = SolutionProvider(ctx).solution(nights, constraints.selected, Set(o))
      o -> new NightlyOptionalFunction(nights, site, hours(o, s))
    }).toMap
    plotter.plotSolution(MainHourAxis, orderedObs, riseInRenderer, funcs)
    plotter.plotFunction(MainHourAxis, orderedObs, riseOutRenderer, funcs)

    // add hour plot specific details
    plotHoursDetails(plotter)

    // additional curves as currently selected by user
    val curvesInRenderer = colorCoding.lineRenderer(ctx, observations.toSeq, DashedThinStroke)
    val curvesOutRenderer = colorCoding.lineRenderer(ctx, observations.toSeq, DashedThinStroke, Some(Color.gray))
    plotter.plotCurves(selectedObs, details.selected, curvesInRenderer, curvesOutRenderer)
    plotter.plotOptions(selectedObs, details.selected, curvesInRenderer, curvesOutRenderer)


    addDetails(plot, selectedObs.toSet, timeControl.daysShowing)

    addLegend(plot, observations, colorCoding)

    chart
  }

  /** Hours plot specific details. */
  private def plotHoursDetails(plotter: XYPlotter): Unit =
    details.selected.foreach {
      case MoonHours =>
        val renderer = XYPlotter.lineRenderer(QvGui.MoonColor, SolidThickStroke)
        plotter.plotFunction(MainHourAxis, renderer, new NightlyOptionalFunction(nights, site, moonHours))
      case _ => // Ignore
    }

  private def moonHours(n: Night): Option[Double] =
    (n.moonAboveHorizon.restrictTo(n.scienceTime).duration.toDouble / DateTimeUtils.MillisecondsPerHour).some // moon hours during science time

  private def hours(o: Obs, s: Solution)(n: Night): Option[Double] =
    (s.restrictTo(n.interval).duration.toDouble / DateTimeUtils.MillisecondsPerHour).some
}
