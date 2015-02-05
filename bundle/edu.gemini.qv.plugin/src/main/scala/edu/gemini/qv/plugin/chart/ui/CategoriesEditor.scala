package edu.gemini.qv.plugin.chart.ui

import scala.swing._
import scala.swing.event._
import edu.gemini.qv.plugin.chart.Chart._
import edu.gemini.qv.plugin.chart.ui.CategoriesEditor._
import edu.gemini.qv.plugin.table.renderer.{CalculationRenderer, CellRenderer}
import edu.gemini.qv.plugin.data.ObservationProvider
import scala.swing.Swing.EmptyBorder
import edu.gemini.qv.plugin.chart.editor._
import edu.gemini.qv.plugin.chart.editor.AxisSelector
import scala.swing.event.SelectionChanged
import edu.gemini.qv.plugin.chart.Chart.Calculation
import edu.gemini.qv.plugin.{QvContext, QvStore}
import edu.gemini.qv.plugin.chart.Axis
import edu.gemini.qv.plugin.filter.ui.MainFilter

object CategoriesEditor {

  /** Events raised by categories editors. */
  case object AxisChanged extends Event
  case class ColorCodingChanged(axis: Axis) extends Event

}

class FilterCategoriesEditor(main: MainFilter) extends CategoriesEditor {
  val f = FilterSelector("Filter", main)
  val selectors = Seq(f)

  f.element = QvStore.DefaultFilterName

  layoutSelectors()

  override def doUpdate(source: Component) {
    main.init(f.element.filters)
    publish(AxisChanged)
  }
}

/** Editor for chart categories. Charts only support calculations that result in a double value. */
class ChartCategoriesEditor(ctx: QvContext) extends CategoriesEditor {
  val x = AxisSelector(ctx, "x-Axis")
  val y = AxisSelector(ctx, "y-Axis")
  val f = new FunctionSelector("Function")
  val c = new ChartSelector("Chart", x, y, f)
  val selectors = Seq(c, x, y, f)

  c.element = QvStore.DefaultHistogramName
  x.axis = c.element.xAxis.label
  y.axis = c.element.yAxis.label
  f.element = c.element.function.label

  def xAxis = x.axis
  def yAxis = y.axis
  def func = f.element

  layoutSelectors()

  override def doUpdate(source: Component) = source match {
    case c.selector =>
      x.element = c.element.xAxis.label
      y.element = c.element.yAxis.label
      f.element = c.element.function.label
    case _ =>
    publish(AxisChanged)
  }
}

/** Editor for table categories. Tables allow arbitrary views that display the selected observations. */
class TableCategoriesEditor(ctx: QvContext) extends CategoriesEditor {
  val x = AxisSelector(ctx, "x-Axis")
  val y = AxisSelector(ctx, "y-Axis")
  val f = new CellSelector("Function")
  val t = new TableSelector("Table", x, y, f)
  val selectors = Seq(t, x, y, f)

  t.element = QvStore.DefaultTableName
  x.axis = t.element.xAxis.label
  y.axis = t.element.yAxis.label
  f.element = t.element.function.label

  def xAxis = x.axis
  def yAxis = y.axis
  def func = f.element

  layoutSelectors()

  override def doUpdate(source: Component) {
    if (source == t.selector) {
      x.element = t.element.xAxis.label
      y.element = t.element.yAxis.label
      f.element = t.element.function.label
    }
    publish(AxisChanged)
  }
}

/** Editor for chart categories. Charts only support calculations that result in a double value. */
class BarChartCategoriesEditor(ctx: QvContext) extends CategoriesEditor {
  val y = AxisSelector(ctx, "y-Axis", () => QvStore.axes ++ Axis.Dynamics)
  val cc = AxisSelector(ctx, "Color Coding")
  val c = new BarChartSelector("Visibility", y, cc)
  val selectors = Seq(c, y, cc)

  y.axis = c.element.yAxis.label
  cc.element = c.element.colorCoding.label

  def yAxis = y.axis
  def colorCoding = cc.axis

  layoutSelectors()

  override def doUpdate(source: Component) = {
    if (source == c.selector) {
      cc.element = c.element.colorCoding.label
      y.element = c.element.yAxis.label
    }
    publish(AxisChanged)
  }
}

/** Editor for elevation and hours visibility plots. */
class VisChartEditor(ctx: QvContext) extends CategoriesEditor {
  val cc = AxisSelector(ctx, "Color Coding", () => QvStore.axes ++ Axis.Dynamics)
  val selectors = Seq(cc)

  def colorCoding = cc.axis

  layoutSelectors()

  override def doUpdate(source: Component) = {
    source match {
      case cc.selector => publish(ColorCodingChanged(cc.axis))
    }
  }
}

/**
 * The abstract base class for categories editors.
 */
trait CategoriesEditor extends GridBagPanel with Publisher {

  val selectors: Seq[Selector[_]]
  def doUpdate(source: Component) {
    publish(AxisChanged)
  }

  border = EmptyBorder(10,10,10,10)

  def layoutSelectors() {
    selectors.zipWithIndex.foreach({case (s, posY) => {
      // create label
      val label = new Label(s.label){horizontalAlignment = Alignment.Right}

      // layout all three elements, editor part is optional
      layout(label)           = new Constraints { gridx=0; gridy=posY; weightx=0.05 }
      layout(s.selector)      = new Constraints { gridx=1; gridy=posY; weightx=0.90; fill = GridBagPanel.Fill.Horizontal}
      if (s.editor != null)
        layout(s.editButton)  = new Constraints { gridx=2; gridy=posY; weightx=0.05 }
    }})
    listenTo(selectors:_*)
    reactions += {
      case SelectionChanged(source) => source match {
        case s => doUpdate(source)
      }
    }
  }


}
