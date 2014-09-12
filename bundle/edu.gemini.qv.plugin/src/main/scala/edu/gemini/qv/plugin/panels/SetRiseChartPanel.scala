package edu.gemini.qv.plugin.panels

import edu.gemini.qv.plugin.chart.ui.{JFChartComponent, VisChartEditor}
import edu.gemini.qv.plugin.charts.SetRiseChart
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.selector.OptionsSelector._
import edu.gemini.qv.plugin.selector.{TimeRangeSelector, OptionsSelector}
import edu.gemini.qv.plugin.util.SemesterData
import java.util.logging.Logger

object SetRiseChartPanel {
  val Log = Logger.getLogger(classOf[SetRiseChartPanel].getName)
}

class SetRiseChartPanel(val ctx: QvContext) extends VisibilityChartPanel {

  import SetRiseChartPanel.Log

  private val showOptions = Group("Show", "Show additional details.", mutex = false, Now, Twilights, MoonSetRise, MoonPhases, InsideMarkers, OutsideMarkers, Schedule)
  private val plotOptions = Group("Plot", "Plot additional curves.", mutex = false, ElevationCurve, MinElevationCurve, MaxElevationCurve, ParallacticAngleCurve, SkyBrightnessCurve, LunarDistanceCurve, HourAngleCurve)
  private val riseSetOptions = Group("Set/Rise Time", "Choose between rise or set times.", mutex = true, ShowSetTimeOption, ShowRiseTimeOption)

  protected val editor = new VisChartEditor(ctx)
  protected val timeControl = new TimeRangeSelector(ctx, Seq())
  protected val details = new OptionsSelector(showOptions, plotOptions, riseSetOptions)
  protected val chartPanel = new JFChartComponent

  // init
  activeGroups = editor.colorCoding.groups.toSet
  // init

  doLayout()
  updateChart()

  def updateChart(): Unit = {

    val time = System.currentTimeMillis()

    hideScrollbar()

    val nights = SemesterData.nights(ctx.site, timeControl.start, timeControl.end)
    chartPanel.updateChart(new SetRiseChart(ctx, nights, ctx.selectedFoldedObs, activeGroups, inactiveGroups, timeControl, constraints, details).createChart)

    Log.fine(s"updated visible rise/set chart in ${System.currentTimeMillis() - time}ms")
  }

}
