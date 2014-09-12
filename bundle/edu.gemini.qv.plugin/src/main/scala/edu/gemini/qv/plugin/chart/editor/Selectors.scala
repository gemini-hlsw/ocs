package edu.gemini.qv.plugin.chart.editor

import edu.gemini.qv.plugin.chart.Axis
import edu.gemini.qv.plugin.chart.Chart.Calculation
import edu.gemini.qv.plugin.chart.ui._
import edu.gemini.qv.plugin.data.DataChanged
import edu.gemini.qv.plugin.filter.core.FilterSet
import edu.gemini.qv.plugin.filter.ui.MainFilter
import edu.gemini.qv.plugin.QvStore._
import edu.gemini.qv.plugin.{QvContext, QvStore}
import edu.gemini.qv.plugin.table.renderer.CellRenderer
import edu.gemini.qv.plugin.ui.QvGui
import scala.swing.ListView.Renderer
import scala.swing.event.Event
import scala.swing.event.SelectionChanged
import scala.swing.{Publisher, Action, Button, ComboBox}

/**
 * Selector for changing "categorized" axes, i.e. axes which are divided into categories of observations.
 * Note that the axis selectors need to reflect to changes in the QvStore when axes are added or removed.
 */
case class AxisSelector(ctx: QvContext, label: String, axes: () => Seq[Axis] = () => QvStore.axes) extends Selector[Axis] {
  val changedEvent = AxesChanged
  def editor = AxisEditor(ctx, element.label, element)
  def elements = axes
  def axis = element
  def axis_=(label: String) = { element = label }
}

case class FilterSelector(label: String, main: MainFilter) extends Selector[FilterSet] {
  val changedEvent = DataChanged
  def editor = new FilterEditor(element.label, main)
  def elements = () =>  QvStore.filters
}

case class FunctionSelector(label: String) extends Selector[Calculation] {
  val changedEvent = ChartsChanged
  def editor = null
  def elements = () =>  QvStore.functions
}

case class CellSelector(label: String) extends Selector[CellRenderer] {
  val changedEvent = ChartsChanged
  def editor = null
  def elements = () => QvStore.renderers
}

case class ChartSelector(label: String, xAxisSelector: AxisSelector, yAxisSelector: AxisSelector, functionSelector: FunctionSelector) extends Selector[Histogram] {
  val changedEvent = ChartsChanged
  def editor = new ChartEditor(element.label, xAxisSelector.axis, yAxisSelector.axis, functionSelector.element)
  def elements = () => QvStore.histograms
}

case class TableSelector(label: String, xAxisSelector: AxisSelector, yAxisSelector: AxisSelector, cellSelector: CellSelector) extends Selector[Table] {
  val changedEvent = TablesChanged
  def editor = new TableEditor(element.label, xAxisSelector.axis, yAxisSelector.axis, cellSelector.element)
  def elements = () => QvStore.tables
}

case class BarChartSelector(label: String, yAxisSelector: AxisSelector, colorCodingSelector: AxisSelector) extends Selector[BarChart] {
  val changedEvent = ChartsChanged
  def editor = new BarChartEditor2(element.label, yAxisSelector.axis, colorCodingSelector.axis)
  def elements = () => QvStore.visCharts
}

trait Selector[A <: NamedElement] extends Publisher {

  val label: String                 // the label shown in the UI for this selector (e.g. "x-Axis")
  val changedEvent: Event
  def editor: ElementEditor
  def elements: () => Seq[A]

  val selector   = createSelector
  val editButton = createEditButton

  def element    = selector.selection.item

  def element_=(label: String) {
    val index = elements().indexWhere(_.label == label)
    selector.selection.index = if (index >= 0) index else 0
  }

  // if axes are added to or removed from the QvStore we need to reflect these changes in the combo box
  listenTo(QvStore, selector.selection)
  deafTo(this) // avoid cycles
  reactions += {
    case `changedEvent` => {
      val curSelection = selector.selection.item.label
      selector.peer.setModel(ComboBox.newConstantModel(elements()))
      element = curSelection
    }
    case SelectionChanged(s) => publish(SelectionChanged(selector))
  }

  /** Creates a combo box that lists all available axes. */
  private def createSelector: ComboBox[A] = new ComboBox[A](elements()) {
    renderer = Renderer(_.label)
  }

  /** Creates an edit button that allows to edit/add/delete axes. */
  private def createEditButton = new Button() {
    action = new Action("") {
      toolTip = "Customize this element."
      icon = QvGui.EditIcon
      def apply() = {
        val e = editor
        e.location = locationOnScreen
        e.open()
        if (!e.isCancelled) element = e.elementName
      }
    }
  }


}
