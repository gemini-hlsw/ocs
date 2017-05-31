package edu.gemini.qv.plugin.panels

import edu.gemini.qv.plugin.chart.ui.BarChartCategoriesEditor
import edu.gemini.qv.plugin.chart.ui.CategorySelected
import edu.gemini.qv.plugin.chart.ui.CategorySubSelected
import edu.gemini.qv.plugin.chart.ui.JFChartComponent
import edu.gemini.qv.plugin.charts.BarChart
import edu.gemini.qv.plugin.data.{FilterChanged, CategorizedYObservations}
import edu.gemini.qv.plugin.filter.core.Filter
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.QvContext.BarChartType
import edu.gemini.qv.plugin.selector.OptionsSelector._
import edu.gemini.qv.plugin.selector.{OptionsSelector, TimeRangeSelector}
import edu.gemini.qv.plugin.util.SemesterData
import java.util.logging.Logger

object BarChartPanel {
  val Log = Logger.getLogger(classOf[BarChartPanel].getName)
}

/**
 * A component that displays a bar chart and all UI elements needed to interact with it.
 */
case class BarChartPanel(ctx: QvContext) extends VisibilityChartPanel {

  import BarChartPanel.Log

  private val showOptions = Group("Show", "Show additional details.", mutex=false, Now, Twilights, IgnoreDaytime, InsideMarkers, OutsideMarkers, Schedule)

  protected val timeControl = new TimeRangeSelector(ctx, Seq())
  protected val details = new OptionsSelector(showOptions)
  protected val editor = new BarChartCategoriesEditor(ctx)
  protected val chartPanel = new JFChartComponent

  doLayout()

  listenTo(chartPanel, ctx.mainFilterProvider, ctx.tableFilterProvider)
  reactions += {
    case FilterChanged(_, _, _) => if (visible) updateChart()
    case CategorySelected(f) =>
      ctx.selectionFilter = f
    case CategorySubSelected(f) =>
      ctx.subselectionOrigin = BarChartType
      ctx.tableFilter = f
      updateChart()
  }

  protected def updateChart(): Unit = {
    val time = System.currentTimeMillis()

    // create obs and progs filter on the fly with most current data
    val groups = editor.yAxis.label match {
      case "Observations" => Filter.Observation.forObservations(ctx.observations)
      case "Programs" => Filter.Program.forPrograms(ctx.observations)
      case _ => editor.yAxis.groups
    }

    val obsSource = if (ctx.subselectionOrigin == BarChartType) ctx.observations else ctx.filtered
    val newData = new CategorizedYObservations(ctx, groups, obsSource)
    updateScrollbar(newData)
    val nights = SemesterData.nights(ctx.site, timeControl.start, timeControl.end)
    chartPanel.updateChart(new BarChart(ctx, ctx.site, newData, nights, timeControl.selectedZone, constraints, details, editor.colorCoding, chartScroll.value, chartScroll.visibleAmount).createChart)

    Log.fine(s"updated visible elevation chart in ${System.currentTimeMillis() - time}ms")
  }


}
