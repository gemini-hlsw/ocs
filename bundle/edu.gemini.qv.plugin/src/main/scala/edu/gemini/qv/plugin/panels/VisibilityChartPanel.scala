package edu.gemini.qv.plugin.panels

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.chart.ui.CategoriesEditor.AxisChanged
import edu.gemini.qv.plugin.chart.ui.CategoriesEditor.ColorCodingChanged
import edu.gemini.qv.plugin.chart.ui._
import edu.gemini.qv.plugin.data._
import edu.gemini.qv.plugin.filter.core.Filter
import edu.gemini.qv.plugin._
import edu.gemini.qv.plugin.selector._
import edu.gemini.qv.plugin.ui.SideBar
import java.awt.event.{AdjustmentEvent, AdjustmentListener}
import scala.swing._

trait VisibilityChartPanel extends GridBagPanel {

  protected val ctx: QvContext

  protected val editor: CategoriesEditor
  protected val chartPanel: JFChartComponent
  protected val timeControl: TimeRangeSelector
  protected val details: OptionsSelector
  protected def updateChart(): Unit


  protected object chartScroll extends CategoriesScrollBar
  protected object constraints extends ConstraintsSelector(ctx)
  protected object theSideBar extends SideBar(details)

  protected var activeGroups: Set[Filter] = Set()
  protected var inactiveGroups: Set[Filter] = Set()

  protected def doLayout(): Unit = {
    import GridBagPanel.Fill._

    val constraintsPanel = new GridBagPanel {
      layout(constraints.mainConstraints)     = new Constraints() { gridx=0; gridy=0; weightx=0.5; fill = Horizontal }
      layout(constraints.scheduleConstraints) = new Constraints() { gridx=1; gridy=0; weightx=0.5; fill = Horizontal }
    }

    layout(editor) = new Constraints() {
      gridx = 0
      gridy = 0
      gridwidth = 4
      weightx = 1
      fill = Horizontal
    }
    layout(chartPanel) = new Constraints() {
      gridx = 0
      gridy = 1
      weightx = 1
      weighty = 1
      fill = Both
    }
    layout(chartScroll) = new Constraints() {
      gridx = 1
      gridy = 1
      weighty = 1
      fill = Vertical
    }
    layout(theSideBar) = new Constraints() {
      gridx = 2
      gridy = 1
      gridheight = 3
      weighty = 1
      fill = Vertical
    }
    layout(timeControl) = new Constraints() {
      gridx = 0
      gridy = 2
      weightx = 1
      fill = Horizontal
    }
    layout(constraintsPanel) = new Constraints() {
      gridx = 0
      gridy = 3
      weightx = 1
      fill = Horizontal
    }

    listenTo(ctx, ctx.selectionFilterProvider, editor, chartScroll, constraints, details, chartPanel)
    reactions += {
      case AxisChanged => updateIfVisible()
      case ColorCodingChanged(axis) =>
        inactiveGroups = axis.label match {
          case "Observations" => ctx.foldedFilters
          case "Programs" => Filter.Program.forPrograms(ctx.filtered).toSet
          case _ => axis.groups.toSet
        }
        activeGroups = Set()
        ctx.selected.foreach(doSelect)
        updateIfVisible()
      case DataChanged => updateIfVisible()
      case ForceRepaint => updateIfVisible()
      case FilterChanged(_, added, removed) =>
        // freshly selected observations (added to the selection) are by default active
        // for all other observations we want to keep the current active/inactive state
        added.foreach(doSelect)
        updateIfVisible()
      case TimeRangeChanged => updateIfVisible()
      case TimeValueChanged => updateIfVisible()
      case TimeZoneChanged => updateIfVisible()
      case OptionsChanged => updateIfVisible()
      case ConstraintsChanged => updateIfVisible()
      case ObservationSelected(o) =>
        // activate/deactivate selected groups based on an observation the user clicked on
        val a = activeGroups.find(_.predicate(o, ctx))
        val i = inactiveGroups.find(_.predicate(o, ctx))
        a.foreach { g => activeGroups -= g; inactiveGroups += g }
        i.foreach { g => activeGroups += g; inactiveGroups -= g }
        updateIfVisible()
      case ObservationsSelected(f) =>
        // activate/deactivate selected groups
        if (activeGroups.contains(f)) activeGroups -= f else activeGroups += f
        if (inactiveGroups.contains(f)) inactiveGroups -= f else inactiveGroups += f
        updateIfVisible()
    }
  }

  protected def doSelect(o: Obs) = {
    // activate/deactivate selected groups based on an observation the user clicked on
    val i = inactiveGroups.find(_.predicate(o, ctx))
    i.foreach { g => activeGroups += g; inactiveGroups -= g }
  }

  protected def hideScrollbar() = chartScroll.visible = false

  protected def updateScrollbar(data: CategorizedYObservations): Unit = {
    // set new maximum value in vertical scroll bar
    val cnt = data.activeYGroups.filter(data.observationsFor(_).size > 0).size
    chartScroll.maximum = cnt
    chartScroll.visibleAmount = 15
    chartScroll.blockIncrement = 15
    chartScroll.unitIncrement = 1
    chartScroll.visible = chartScroll.visibleAmount < cnt
  }

  private def updateIfVisible(): Unit = if (visible) updateChart()

  class CategoriesScrollBar extends ScrollBar with AdjustmentListener {
    peer.addAdjustmentListener(this)
    def adjustmentValueChanged(e: AdjustmentEvent): Unit = publish(DataChanged)
  }

}
