package edu.gemini.pit.ui.util

import scala.swing.TextComponent
import java.awt.event.{ FocusListener, FocusEvent }

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