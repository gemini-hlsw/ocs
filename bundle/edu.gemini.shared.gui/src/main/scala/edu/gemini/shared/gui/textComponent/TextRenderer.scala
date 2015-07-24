package edu.gemini.shared.gui.textComponent

import javax.swing.JLabel

import scala.swing.{ComboBox, ListView}

trait TextRenderer[A] { this:ComboBox[A] =>
  renderer = new ListView.Renderer[A] {
    val delegate = renderer
    def componentFor(list: ListView[_ <: A], isSelected: Boolean, focused: Boolean, a: A, index: Int) = {
      val c = delegate.componentFor(list, isSelected, focused, a, index)
      c.peer.asInstanceOf[JLabel].setText(text(a))
      c
    }
  }
  def text(a:A):String
}

