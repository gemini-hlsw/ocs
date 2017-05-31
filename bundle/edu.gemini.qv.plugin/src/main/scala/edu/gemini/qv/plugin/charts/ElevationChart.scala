package edu.gemini.qv.plugin.charts

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.chart.Axis
import edu.gemini.qv.plugin.charts.util.{XYPlotter, QvChartFactory, ColorCoding}
import edu.gemini.qv.plugin.filter.core.Filter
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.selector.OptionsSelector._
import edu.gemini.qv.plugin.selector.{TimeRangeSelector, ConstraintsSelector, OptionsSelector}
import edu.gemini.util.skycalc.Night
import java.awt.Color
import org.jfree.chart._
import org.jfree.chart.axis.DateAxis
import org.jfree.chart.plot.XYPlot

/**
 * Bar charts are essentially one form of visibility plots which show when an observation (or a group of observations)
 * is observable (visible) taking all restrictions into account like elevation limits, timing windows etc.
 */
case class ElevationChart(ctx: QvContext, nights: Seq[Night], observations: Set[Obs], activeFilters: Set[Filter], inactiveFilters: Set[Filter], timeControl: TimeRangeSelector, constraints: ConstraintsSelector, details: OptionsSelector, colorCodingAxis: Axis) extends VisibilityXYChart {

  val site = ctx.site

  def createChart: JFreeChart = {

    val chart = QvChartFactory.createTimelineChart(title("Elevation for"))
    val plot = chart.getPlot.asInstanceOf[XYPlot]
    initDateAxis(plot.getDomainAxis.asInstanceOf[DateAxis], nights, timeControl.selectedZone)

    // order is relevant, make sure we use same order everywhere
    val selectedActive = observations.filter(o => activeFilters.exists(f => f.predicate(o, ctx)))
    val orderedActive = selectedActive.toSeq
    val colorCoding = ColorCoding(activeFilters, inactiveFilters)

    // main curve and axis
    val mainAxis =
      if (details.isSelected(AirmassRuler))
        if (nights.size > 8) MidNightAirmassAxis else AirmassAxis
      else
        if (nights.size > 8) MainMidNightElevationAxis else MainElevationAxis
    plot.setRangeAxis(mainAxis)
    val plotter = new XYPlotter(ctx, nights, constraints, details, plot)
    val elevInRenderer = colorCoding.lineRenderer(ctx, observations.toSeq, SolidThickStroke)
    val elevOutRenderer = colorCoding.lineRenderer(ctx, observations.toSeq, SolidThinStroke, Some(Color.gray))
    plotter.plotCurves(observations.toSeq, Set(ElevationCurve), elevInRenderer, elevOutRenderer)

    val curvesInRenderer = colorCoding.lineRenderer(ctx, observations.toSeq, DashedThinStroke)
    val curvesOutRenderer = colorCoding.lineRenderer(ctx, observations.toSeq, DashedThinStroke, Some(Color.gray))
    plotter.plotCurves(orderedActive, details.curves, curvesInRenderer, curvesOutRenderer)
    plotter.plotOptions(orderedActive, details.selected, curvesInRenderer, curvesOutRenderer)

    addDetails(plot, selectedActive, timeControl.daysShowing)

    addLegend(plot, observations, colorCoding)

    chart
  }


}
