package edu.gemini.pit.ui.util.gface

import edu.gemini.pit.ui.util.ScrollPanes
import edu.gemini.ui.workspace.util.Factory
import scala.swing.ScrollPane
import javax.swing.{BorderFactory, SwingConstants, JLabel, Icon}
import java.awt.Color
import edu.gemini.ui.gface._
import edu.gemini.pit.ui.binding._

// TODO: factor out commonality with SimpleListViewer
abstract class MultiSelectSimpleListViewer[A, B, C <: Object](implicit ev: Null <:< B, ev2: Null <:< C)
  extends ScrollPane
  with Bound[A, B] {

  // Bound
  override def refresh(m: Option[B]) {
    viewer.setModel(m.orNull)
  }

  // General refresh
  def refresh() {
    viewer.refresh()
  }

  // Viewer Delegates
  def selection = viewer.selection
  def selection_=(cs: List[C]) {
    val gs = new GSelection[C](cs: _*)
    viewer.setSelection(gs)
  }

  def onDoubleClick(f: List[C] => Unit) {
    viewer.onDoubleClick(f)
  }
  def onSelectionChanged(f: List[C] => Unit) {
    viewer.onSelectionChanged(f)
  }

  protected val columns: Enumeration
  type Column = columns.Value

  def columnWidth: PartialFunction[Column, (Int, Int)]
  def text(c: C): PartialFunction[Column, String]
  def icon(c: C): PartialFunction[Column, Icon]

  def alignment(c: C): PartialFunction[Column, Int] = {
    case _ => SwingConstants.LEFT
  }

  def tooltip(c: C): PartialFunction[Column, String] = {
    case _ => null
  }

  def indent(s: C): PartialFunction[Column, Int] = {
    case _ => 0
  }

  def foreground(s: C): PartialFunction[Column, Color] = {
    case _ => Color.BLACK
  }

  def size(b: B): Int
  def elementAt(b: B, i: Int): C

  override lazy val peer = {
    val p = Factory.createStrippedScrollPane(viewer.getTable)
    ScrollPanes.setViewportWidth(p)
    ScrollPanes.setViewportHeight(p, 5)
    p
  }

  protected object viewer extends GTableViewer[B, C, Column](controller) with MultiSelectViewerHelpers[C] {
    setDecorator(decorator)
    setColumns(columns.values.toSeq: _*)
    for {
      c <- columns.values
      (i, j) <- columnWidth.lift(c)
    } setColumnSize(c, i, j)
  }

  private object controller extends GTableController[B, C, Column] {
    def modelChanged(v: GViewer[B, C], old: B, m: B): Unit = {}
    def getElementAt(i: Int): C = model.map(elementAt(_, i)).orNull
    def getElementCount: Int = model.map(size).getOrElse(0)
    def getSubElement(c: C, col: Column): AnyRef = c
  }

  object decorator extends GSubElementDecorator[B, C, Column] {
    def modelChanged(v: GViewer[B, C], old: B, m: B): Unit = {}
    def decorate(label: JLabel, c: C, col: Column, value: AnyRef): Unit = {
      label.setIcon(icon(c).lift(col).orNull)
      label.setText(text(c).lift(col).orNull)
      label.setHorizontalAlignment(alignment(c).lift(col).getOrElse(SwingConstants.LEFT))
      label.setBorder(BorderFactory.createEmptyBorder(2, indent(c).lift(col).getOrElse(0) * 20, 2, 1))
      label.setForeground(foreground(c).lift(col).getOrElse(Color.BLACK))
      label.setToolTipText(tooltip(c).lift(col).getOrElse(""))
    }
  }

  def setFilter(f: C => Boolean): Unit = {
    viewer.setFilter(new GFilter[B, C] {
      def accept(c: C): Boolean = f(c)
      def modelChanged(v: GViewer[B, C], old: B, m: B): Unit = {}
    })
  }

}

