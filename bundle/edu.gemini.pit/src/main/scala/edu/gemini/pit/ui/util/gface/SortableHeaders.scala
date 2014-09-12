package edu.gemini.pit.ui.util.gface

import java.awt.event._
import javax.swing.table._
import edu.gemini.ui.gface.{GViewer, GComparator}
import scalaz._
import Scalaz._

trait SortableHeaders[B, C <: Object] {
  this: MultiSelectSimpleListViewer[_, B, C] =>

  viewer.getTable.getTableHeader.addMouseListener(
    new MouseAdapter() {
      override def mouseClicked(e: MouseEvent) {
        val h = e.getSource.asInstanceOf[JTableHeader]
        h.columnAtPoint(e.getPoint) match {
          case -1 => // nop
          case n =>
            val comp = viewer.getComparator match {
              case Comparator(m, f) if n == m => Comparator(n, !f)
              case Comparator(_, f) => Comparator(n, f)
              case _ => Comparator(n)
            }
            viewer.setComparator(comp)
        }
      }
    }
  )

  case class Comparator(nCol: Int, asc: Boolean = true) extends GComparator[B, C] {
    private val col = columns.values.toList(nCol)
    def modelChanged(viewer: GViewer[B, C], oldModel: B, newModel: B) {}
    def compare(o1: C, o2: C): Int = {
      val n = SortableHeaders.this.compare(o1, o2, col)
      asc ? n | -n
    }
    def str(c: C) = text(c).lift(col).getOrElse("")
  }

  def compare(c1: C, c2: C, col: Column): Int


}
