package edu.gemini.qv.plugin.table.renderer

import scala.swing.Component
import javax.swing.{BorderFactory, JTable}
import javax.swing.table.TableCellRenderer
import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.chart.Chart.ChartFunction
import java.awt.Color

// CELL AND CELL RENDERER

object CellRenderer {
  val Renderers = CalculationRenderer.Renderers ++ Seq(ObservationsRenderer, ProgramsRenderer, EncodedObservationsRenderer)
}

trait Cell extends Component {
  val selectedBorder = BorderFactory.createLineBorder(Color.red)

  opaque = true
  background = Color.white
  foreground = Color.black

  def update(table: JTable, rowIsSelected: Boolean, cellIsSelected: Boolean) {
    border = if (cellIsSelected) selectedBorder else null
  }
}

// abstract cell renderer
trait CellRenderer extends TableCellRenderer with ChartFunction {
  def reset() {}
  def createCell(obs: Set[Obs]): Cell
  override def getTableCellRendererComponent(table: JTable, o: Object, isSelected: Boolean, hasFocus: Boolean, row: Int, col: Int): java.awt.Component = {
    o match {
      // if this is a cell, render it
      case cell: Cell =>
        cell.update(table, isSelected, hasFocus)
        val curHeight = table.getRowHeight(row)
        val newHeight = cell.preferredSize.getHeight.toInt
        if (newHeight > curHeight) table.setRowHeight(row, newHeight)
        cell.peer
      // any other component is just forwarded (row headers)
      case component: Component =>
        component.peer
    }
  }
}
