package edu.gemini.shared.gui

import scala.swing.Table

trait SortableTable { this: Table =>
  /**
   * Sorting is not supported in scala.swing.Table (Scala 2.10).
   */
  // See: http://stackoverflow.com/questions/9588765/using-tablerowsorter-with-scala-swing-table
  override def apply(row: Int, column: Int): Any = model.getValueAt(viewToModelRow(row), viewToModelColumn(column))

  def viewToModelRow(idx: Int) = peer.convertRowIndexToModel(idx)

  def modelToViewRow(idx: Int) = peer.convertRowIndexToView(idx)
}