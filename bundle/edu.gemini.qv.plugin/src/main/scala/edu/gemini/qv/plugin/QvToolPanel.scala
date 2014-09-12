package edu.gemini.qv.plugin

import edu.gemini.qv.plugin.chart.ui.FilterCategoriesEditor
import edu.gemini.qv.plugin.data._
import edu.gemini.qv.plugin.filter.ui.MainFilter
import edu.gemini.qv.plugin.panels._
import edu.gemini.qv.plugin.table.ObservationTable
import scala.swing.GridBagPanel.Fill._
import scala.swing.Swing._
import scala.swing._
import scala.swing.event.SelectionChanged

/**
 * QV main panel with the filter on the left and the chart view and observation table on the right.
 * @param ctx
 */
class QvToolPanel(ctx: QvContext) extends GridBagPanel {

  object histogramPage  extends TabbedPane.Page("Histogram", new HistogramChartPanel(ctx))
  object tablePage      extends TabbedPane.Page("Table", new TableChartPanel(ctx))
  object barPage        extends TabbedPane.Page("Vis Bar", new BarChartPanel(ctx))
  object elevationPage  extends TabbedPane.Page("Vis Elevation", new ElevationChartPanel(ctx))
  object hoursPage      extends TabbedPane.Page("Vis Hours", new HoursChartPanel(ctx))
  object riseSetPage    extends TabbedPane.Page("Vis Set/Rise", new SetRiseChartPanel(ctx))

  object charts extends TabbedPane
  charts.pages += histogramPage
  charts.pages += tablePage
  charts.pages += barPage
  charts.pages += elevationPage
  charts.pages += hoursPage
  charts.pages += riseSetPage

  // This is purely for efficiency reasons: Updates are slow if every time all charts (including currently hidden ones)
  // are redrawn - which is particularly time consuming for many observations and usually pretty useless. In order to
  // avoid this overhead, charts should only update/redraw if they are visible; however as soon as they are selected and
  // are shown to the user we must first make sure they reflect the latest user selection which is done by publishing
  // a fake event to them (i.e. they are updated when made visible to catch up with any changes they might have
  // "missed" while being hidden).
  listenTo(charts.selection)
  reactions += {
    case SelectionChanged(_) => charts.selection.page match {
      case `barPage`        => ctx.publish(ForceRepaint)
      case `elevationPage`  => ctx.publish(ForceRepaint)
      case `hoursPage`      => ctx.publish(ForceRepaint)
      case `riseSetPage`    => ctx.publish(ForceRepaint)
      case _ => // Ignore
    }
  }

  object obsTable extends ObservationTable(ctx)
  object horizSplitPane extends SplitPane(Orientation.Horizontal, charts, obsTable) {
    border = EmptyBorder(5, 5, 5, 5)
  }
  object filter extends MainFilter(ctx, QvStore.defaultFilter.filters)
  object filterSelector extends FilterCategoriesEditor(filter)
  object filterPanel extends GridBagPanel {
    border = EmptyBorder(5, 5, 5, 5)
    layout(filterSelector) = new Constraints {
      gridy = 0
      weightx = 1
      fill = Horizontal
    }
    layout(filter)= new Constraints {
      gridy = 1
      weighty = 1
      fill = Both
    }
  }

  object vertSplitPane extends SplitPane(Orientation.Vertical, filterPanel, horizSplitPane)
  object statusPanel extends StatusPanel(ctx)
  layout(vertSplitPane) = new Constraints {
    gridx = 0
    gridy = 0
    weightx = 1
    weighty = 1
    fill = Both
  }
  layout(statusPanel) = new Constraints {
    gridx = 0
    gridy = 1
    weightx = 0
    weighty = 0
    fill = Horizontal
  }

}