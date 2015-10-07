package edu.gemini.catalog.ui

import javax.swing.table.TableColumn
import scala.swing.Table

import scalaz._
import Scalaz._

/**
 * Support calculating column widths and can adjust them to the outer width
 */
trait TableColumnsAdjuster { this: Table =>
  autoResizeMode = Table.AutoResizeMode.Off

  val minSpacing: Int = 20

  def adjustColumns(containerWidth: Int): Unit = {
    import scala.math.max

    def updateTableColumn(column: TableColumn, width: Int):Unit = {
      this.peer.getTableHeader.setResizingColumn(column)
      column <| {_.setPreferredWidth(width)} <| {_.setWidth(width)}
    }

    def calculateColumnWidth(column: TableColumn): Int = {

      def calculateColumnHeaderWidth: Int = {
        val value = column.getHeaderValue
        val renderer = Option(column.getHeaderRenderer).getOrElse(this.peer.getTableHeader.getDefaultRenderer)

        renderer.getTableCellRendererComponent(this.peer, value, false, false, -1, column.getModelIndex).getPreferredSize.width
      }

      def cellDataWidth(row: Int, col: Int): Int = {
        val cellRenderer = this.peer.getCellRenderer(row, col)
        val c = this.peer.prepareRenderer(cellRenderer, row, col)

        c.getPreferredSize.width + this.peer.getIntercellSpacing.width
      }

      def calculateColumnDataWidth: Int = {
        (0 until this.model.getRowCount).foldLeft(0) { (currMax, i) =>
          max(currMax, cellDataWidth(i, column.getModelIndex))
        }
      }

      val columnHeaderWidth = calculateColumnHeaderWidth
      val columnDataWidth = calculateColumnDataWidth

      max(columnHeaderWidth, columnDataWidth)
    }

    val tcm = this.peer.getColumnModel

    // Calculate the width
    val cols = for {
        i <- 0 until tcm.getColumnCount
        if tcm.getColumn(i).getResizable
      } yield (tcm.getColumn(i), calculateColumnWidth(tcm.getColumn(i)))
    val initialWidth = cols.map(_._2).sum

    val nonResizableCols = for {
        i <- 0 until tcm.getColumnCount
        if !tcm.getColumn(i).getResizable
      } yield (tcm.getColumn(i), calculateColumnWidth(tcm.getColumn(i)))
    val nonResizableWidth = nonResizableCols.map(_._2).sum

    val resizableColumnsLength = cols.length

    // Adjust space to fit on the outer width
    val spacing = max(minSpacing, (containerWidth - initialWidth - nonResizableWidth) / resizableColumnsLength)
    // Add the rounding error to the first col
    val initialOffset = max(0, containerWidth - cols.map(_._2 + spacing).sum - nonResizableWidth)
    // Set width + spacing
    cols.zipWithIndex.foreach {
      case ((c, w), i) if i == 0 => updateTableColumn(c, w + spacing + initialOffset) // Add rounding error to the first col
      case ((c, w), _)           => updateTableColumn(c, w + spacing)
    }

    nonResizableCols.foreach {
      case (c, w) => updateTableColumn(c, w)
    }
  }
}
