package edu.gemini.shared.gui.textComponent

import java.awt.event.{FocusEvent, FocusListener}
import scala.swing.TextComponent

trait SelectOnFocus { this: TextComponent =>
  peer.addFocusListener(new FocusListener {
    def focusLost(fe: FocusEvent) {
      peer.setSelectionStart(0)
      peer.setSelectionEnd(0)
    }
    def focusGained(fe: FocusEvent) {
      javax.swing.SwingUtilities.invokeLater(new Runnable {
        def run() {
          selectAll()
        }
      })
    }
  })

}
