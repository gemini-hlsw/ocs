package edu.gemini.pit.ui.binding

import scala.swing.ComboBox
import scala.swing.TextComponent
import scala.swing.event.ValueChanged
import scala.swing.event.SelectionChanged
import edu.gemini.pit.ui.binding._

// TODO: this stuff is very ugly and should be revisited.
object BoundControls {

  // A trait for bound text components
  trait BoundText[A] extends Bound[A, String] {
    this: TextComponent =>

    def boundView: BoundView[_]

    private var updating = false;
    // TODO: should be thread-local
    override def refresh(s: Option[String]) {
      if (!updating) try {
        updating = true
        enabled = s.isDefined && boundView.canEdit
        text = s.orNull
        peer.setSelectionStart(0)
        peer.setSelectionEnd(0)
      } finally {
        updating = false
      }
    }
    reactions += {
      case ValueChanged(_) => model.foreach {
        s =>
          if (s != text && !updating) try {
            updating = true
            model = Some(text)
          } finally {
            updating = false
          }
      }
    }
  }

  // A trait for bound combo boxes
  trait BoundCombo[B, A] extends Bound[B, A] {
    this: ComboBox[A] =>

    def boundView: BoundView[_]
    private var updating = false

    override def refresh(c: Option[A]) {
      enabled = c.isDefined && boundView.canEdit
      c.foreach {
        a =>
          try {
            updating = true
            selection.item = a
          } finally {
            updating = false
          }
      }
      selection.reactions += {
        case SelectionChanged(_) if !updating => model = Some(selection.item)
      }
    }

  }

}