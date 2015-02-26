package edu.gemini.qv.plugin.panels

import edu.gemini.qv.plugin.chart.Axis
import edu.gemini.qv.plugin.chart.ui.CategoriesEditor.AxisChanged
import edu.gemini.qv.plugin.chart.ui.CategorySelected
import edu.gemini.qv.plugin.chart.ui.CategorySubSelected
import edu.gemini.qv.plugin.chart.ui.{JFChartComponent, ChartCategoriesEditor}
import edu.gemini.qv.plugin.charts.HistogramChart
import edu.gemini.qv.plugin.data._
import edu.gemini.qv.plugin.filter.core.Filter
import edu.gemini.qv.plugin.filter.core.Filter.{HasDummyTarget, RA}
import edu.gemini.qv.plugin.QvContext.HistogramChartType
import edu.gemini.qv.plugin.{ReferenceDateChanged, QvContext}
import edu.gemini.qv.plugin.selector.OptionsSelector._
import edu.gemini.qv.plugin.selector.{OptionsChanged, OptionsSelector}
import edu.gemini.qv.plugin.ui.SideBar
import scala.swing.GridBagPanel
import scala.swing.GridBagPanel.Fill._

/**
 * Versatile chart that can group observation data along both axis by user defined categories.
 */
class HistogramChartPanel(ctx: QvContext) extends GridBagPanel {

  private val showOptions = Group("Show", "Show additional details.", mutex=false, DarkHours, AvailableHours, EmptyCategories, RaAsLst)

  private object details extends OptionsSelector(showOptions)
  private object theSideBar extends SideBar(details)
  private object chartEditor extends ChartCategoriesEditor(ctx)
  private object chartPanel extends JFChartComponent

  layout(chartEditor) = new Constraints() {
    gridx = 0
    gridy = 0
    weightx = 1
    gridwidth = 2
    fill = Horizontal
  }
  layout(chartPanel) = new Constraints() {
    gridx = 0
    gridy = 1
    weightx = 1
    weighty = 1
    fill = Both
  }
  layout(theSideBar) = new Constraints() {
    gridx = 1
    gridy = 1
    weighty = 1
    fill = Vertical
  }

  listenTo(chartEditor, chartPanel, ctx, ctx.mainFilterProvider, ctx.tableFilterProvider, details)

  reactions += {
    case AxisChanged              => updateChart()
    case DataChanged              => updateChart()
    case FilterChanged(_, _, _)   => updateChart()
    case OptionsChanged           => updateChart()
    case ReferenceDateChanged     => updateChart()
    case CategorySelected(f) =>
      ctx.selectionFilter = f
    case CategorySubSelected(f) =>
      ctx.subselectionOrigin = HistogramChartType
      ctx.tableFilter = f
      updateChart()
  }

  private def updateChart() {
    val limitedAxis = limitTimeAxis(chartEditor.xAxis)
    val obsSource = if (ctx.subselectionOrigin == HistogramChartType) ctx.mainFilterProvider else ctx.tableFilterProvider
    val chart = HistogramChart(ctx, limitedAxis, chartEditor.yAxis, chartEditor.func, obsSource, ctx.range, details)
    chartPanel.updateChart(chart.createChart)
  }


  // limit the visible RA bins along the xAxis in case there is a RA filter on the data
  // this applies only on time axes (i.e. only RA filters along x-Axis)
  private def limitTimeAxis(axis: Axis) = {
    if (axis.isTime) {
      val min = minRa
      val max = maxRa
      val limitedGroups = axis.groups.filter {
        case HasDummyTarget(_)                => min == RA.MinValue && max == RA.MaxValue
        case RA(fmin, fmax)     if min <= max => fmax > min && fmin < max
        case RA(fmin, fmax)                   => fmin >= 0
      }
      Axis(axis.label, limitedGroups)
    } else axis
  }

  private def minRa = raOnly.map({case RA(fmin, _) => fmin}).reduceLeftOption(_ min _).getOrElse(RA.MinValue)

  private def maxRa = raOnly.map({case RA(_, fmax) => fmax}).reduceLeftOption(_ max _).getOrElse(RA.MaxValue)

  private def raOnly: Set[Filter] = ctx.mainFilter.map(f => f.elements.filter({
    case RA(_, _) => true
    case _ => false
  })).getOrElse(Set())

}
