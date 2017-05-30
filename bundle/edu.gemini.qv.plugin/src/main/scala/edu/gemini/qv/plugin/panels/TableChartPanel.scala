package edu.gemini.qv.plugin.panels

import edu.gemini.qv.plugin.chart.ui.CategoriesEditor.AxisChanged
import edu.gemini.qv.plugin.chart.ui.{CategorySubSelected, CategorySelected, TableCategoriesEditor}
import edu.gemini.qv.plugin.charts.TableChart
import edu.gemini.qv.plugin.data._
import edu.gemini.qv.plugin.QvContext.TableChartType
import edu.gemini.qv.plugin.{ReferenceDateChanged, QvContext}
import scala.swing.GridBagPanel
import scala.swing.GridBagPanel.Fill._

/**
 * Table representation of categorized data.
 */
class TableChartPanel(ctx: QvContext) extends GridBagPanel {

  private object editor extends TableCategoriesEditor(ctx)
  private object table extends TableChart(ctx, categorizedData, editor.func)

  layout(editor) = new Constraints() {
    gridy = 0
    weightx = 1
    fill = Horizontal
  }
  layout(table) = new Constraints() {
    gridy = 1
    weightx = 1
    weighty = 1
    fill = Both
  }

  val ctxdata = ctx.mainFilterProvider
  val ctxfilter = ctx.tableFilterProvider

  listenTo(editor, table, ctx, ctx.mainFilterProvider, ctx.tableFilterProvider)
  reactions += {
    case ReferenceDateChanged       => update()
    case AxisChanged                => update()
    case DataChanged                => update()
    case FilterChanged(`ctxdata`, _, _)     =>
        update()
    case FilterChanged(`ctxfilter`, _, _)     =>
      if (ctx.subselectionOrigin != TableChartType) {
        update()
      }
    case CategorySelected(f) =>
      ctx.selectionFilter = f
    case CategorySubSelected(f) =>
      if (ctx.subselectionOrigin != TableChartType) {
        ctx.subselectionOrigin = TableChartType
        ctx.tableFilter = f
        update()
      } else {
        ctx.tableFilter = f
      }
  }

  private def update() = {
    table.update(categorizedData, editor.func)
  }

  private def categorizedData = {
    val obsSource = if (ctx.subselectionOrigin == TableChartType) ctx.mainFilterProvider else ctx.tableFilterProvider
    new CategorizedXYObservations(ctx, editor.xAxis.groups, editor.yAxis.groups, obsSource.observations)
  }

}
