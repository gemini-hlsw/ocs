package edu.gemini.pit.ui.util

import scala.swing.Dialog

import javax.swing.KeyStroke
import javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
import java.awt.event.{ ActionEvent, KeyEvent, ActionListener }

/**
 * Mixin for dialogs that close on Esc.
 */
trait CloseOnEsc { this: Dialog =>
  peer.getRootPane.registerKeyboardAction(new ActionListener {
    def actionPerformed(e: ActionEvent) {
      close()
    }
  }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), WHEN_IN_FOCUSED_WINDOW);
}