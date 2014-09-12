package edu.gemini.pit.ui.util

import swing.TextField
import java.awt.event.FocusAdapter
import swing.event.{FocusGained, FocusLost}
import java.awt.Color
import javax.swing.BorderFactory
import java.awt

class PlaceholderTextField(val placeholder:String) extends TextField { tf =>
  private var isPlaceholder = true
  private var actualForeground = foreground

  columns = 15
  border = BorderFactory.createCompoundBorder(
    BorderFactory.createLineBorder(awt.Color.LIGHT_GRAY, 1),
    BorderFactory.createEmptyBorder(0, 2, 0, 2))

  override def foreground_=(c:Color) {
    actualForeground = c
    if (!isPlaceholder)
      super.foreground = c
  }

  peer.addFocusListener(new FocusAdapter {
    override def focusLost(e:java.awt.event.FocusEvent) {
      publish(FocusLost(tf, None, e.isTemporary))
    }
    override def focusGained(e:java.awt.event.FocusEvent) {
      publish(FocusGained(tf, None, e.isTemporary))
    }
  })

  reactions += {

    case FocusLost(_, _, _) if (text == null || text == "") =>
      super.foreground = peer.getDisabledTextColor
      text = placeholder

    case FocusGained(_, _, _) if (text == placeholder) =>
      super.foreground = actualForeground
      text = ""

  }

  publish(FocusLost(tf, None, false))

}
