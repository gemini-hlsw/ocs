package edu.gemini.qv.plugin.table.renderer

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.chart.Chart
import edu.gemini.qv.plugin.chart.Chart.Calculation
import java.awt.Color
import javax.swing.JTable
import scala.swing.{Alignment, Label}
import edu.gemini.qv.plugin.charts.util.ColorCoding

object CalculationRenderer {
  val Renderers = Chart.Calculations.map(new CalculationRenderer(_))
}

/**
 */
class CalculationRenderer(function: Calculation) extends CellRenderer {
  var max = 0.0
  override def reset() = max = 0.0
  def label = function.label
  def createCell(obs: Set[Obs]) = {
    val v = function.value(obs)
    max = Math.max(max, v)
    new FunctionCell(v)
  }
  class FunctionCell(value: Double) extends Label(f"${value}%.2f", null, Alignment.Right) with Cell {
    override def update(table: JTable, rowIsSelected: Boolean, cellIsSelected: Boolean) {
      border = if (cellIsSelected) selectedBorder else null
      foreground = if (value > 0.6*max) Color.white else Color.black
      background = ColorCoding.sequentialColor(value, max)
    }
  }
}
