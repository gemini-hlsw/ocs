package edu.gemini.pit.ui.util

import javax.swing.JLabel
import swing.{ComboBox, ListView}


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

