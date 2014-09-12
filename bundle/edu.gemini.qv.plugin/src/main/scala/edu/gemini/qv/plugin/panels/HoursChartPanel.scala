package edu.gemini.qv.plugin.panels

import edu.gemini.qv.plugin.chart.ui._
import edu.gemini.qv.plugin.charts.HoursChart
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.selector.OptionsSelector._
import edu.gemini.qv.plugin.selector.{OptionsSelector, TimeRangeSelector}
import edu.gemini.qv.plugin.util.SemesterData
import java.util.logging.Logger

object HoursChartPanel {
  val Log = Logger.getLogger(classOf[HoursChartPanel].getName)
}

/**
 * A component that displays a bar chart and all UI elements needed to interact with it.
 */
class HoursChartPanel(val ctx: QvContext) extends VisibilityChartPanel {

  import HoursChartPanel.Log

  private val showOptions = Group("Show", "Show additional details.", mutex = false, Now, Twilights, MoonHours, MoonPhases, InsideMarkers, OutsideMarkers, Schedule)
  private val plotOptions = Group("Plot", "Plot additional curves.", mutex = false, ElevationCurve, MinElevationCurve, MaxElevationCurve, ParallacticAngleCurve, SkyBrightnessCurve, LunarDistanceCurve, HourAngleCurve)

  protected val editor = new VisChartEditor(ctx)
  protected val timeControl = new TimeRangeSelector(ctx, Seq())
  protected val details = new OptionsSelector(showOptions, plotOptions)
  protected val chartPanel = new JFChartComponent

  // init
  activeGroups = editor.colorCoding.groups.toSet
  // init

  doLayout()
  updateChart()

  protected def updateChart(): Unit = {
    val t = System.currentTimeMillis()

    hideScrollbar()
    val nights = SemesterData.nights(ctx.site, timeControl.start, timeControl.end)
    chartPanel.updateChart(new HoursChart(ctx, nights, ctx.selectedFoldedObs, activeGroups, inactiveGroups, timeControl, constraints, details, editor.colorCoding).createChart)

    Log.fine(s"updated visible hours chart in ${System.currentTimeMillis() - t}ms")
  }

}
