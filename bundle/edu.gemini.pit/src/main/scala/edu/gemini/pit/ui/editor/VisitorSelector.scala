package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.immutable._
import javax.swing.BorderFactory

import edu.gemini.pit.ui.util.StdModalEditor
import edu.gemini.shared.Platform

import swing._
import Swing._
import BorderPanel.Position._

object VisitorSelector {
  def open(is:List[(Investigator, Boolean)], canEdit: Boolean, parent:UIElement) = new VisitorSelector(is, canEdit).open(parent)
}

class VisitorSelector private (is:List[(Investigator, Boolean)], canEdit: Boolean) extends StdModalEditor[List[Investigator]]("Select Visitors") {

  override def header = new Label("Select site visitors:") {
    border = BorderFactory.createEmptyBorder(0, 0, 4, 0)
    horizontalAlignment = Alignment.Left
  }

  object editor extends BorderPanel {

    add(new ScrollPane(list), Center)
    add(footer, South)

    object list extends ListView[Investigator](is.map(_._1)) {
      enabled = canEdit

      preferredSize = (200, 200)
      is.zipWithIndex.filter(_._1._2).map(_._2).foreach(selection.indices += _)
    }

    object footer extends Label {
      val mod = if (Platform.IS_MAC) "Command" else "Ctrl"
      text = mod + "+Click to select/deselect."
      border = BorderFactory.createEmptyBorder(4, 0, 0, 0)
      horizontalAlignment = Alignment.Left
    }

  }

  def value = editor.list.selection.items.toList

}
