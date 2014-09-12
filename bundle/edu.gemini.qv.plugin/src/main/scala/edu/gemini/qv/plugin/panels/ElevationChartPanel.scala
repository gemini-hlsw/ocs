package edu.gemini.qv.plugin.panels

import edu.gemini.qv.plugin.chart.ui.{VisChartEditor, JFChartComponent}
import edu.gemini.qv.plugin.charts.ElevationChart
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.selector.OptionsSelector._
import edu.gemini.qv.plugin.selector.{OptionsSelector, TimeRangeSelector}
import edu.gemini.qv.plugin.util.SemesterData
import java.util.logging.Logger

object ElevationChartPanel {
  val Log = Logger.getLogger(classOf[ElevationChartPanel].getName)
}

/**
 * A component that displays a bar chart and all UI elements needed to interact with it.
 */
class ElevationChartPanel(val ctx: QvContext) extends VisibilityChartPanel {

  import ElevationChartPanel.Log

  private val showOptions = Group("Show", "Show additional details.", mutex = false, Now, Twilights, MoonElevation, MoonPhases, AirmassRuler, InsideMarkers, OutsideMarkers, Schedule)
  private val plotOptions = Group("Plot", "Plot additional curves.", mutex = false, MinElevationCurve, MaxElevationCurve, ParallacticAngleCurve, SkyBrightnessCurve, LunarDistanceCurve, HourAngleCurve)

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
    if (visible) {
      val t = System.currentTimeMillis()

      hideScrollbar()

      val nights = SemesterData.nights(ctx.site, timeControl.start, timeControl.end)
      chartPanel.updateChart(new ElevationChart(ctx, nights, ctx.selectedFoldedObs, activeGroups, inactiveGroups, timeControl, constraints, details, editor.colorCoding).createChart)

      Log.fine(s"updated visible elevation chart in ${System.currentTimeMillis() - t}ms")
    }
  }

}
