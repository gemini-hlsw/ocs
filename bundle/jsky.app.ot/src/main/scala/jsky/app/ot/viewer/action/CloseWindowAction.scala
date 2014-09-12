package jsky.app.ot.viewer.action

import jsky.app.ot.viewer.SPViewer

import javax.swing._
import java.awt.event.{ActionEvent, InputEvent, KeyEvent}


/**
 * Close the current program.
 */
class CloseWindowAction(viewer: SPViewer) extends AbstractViewerAction(viewer, "Close Window") {
  putValue(AbstractViewerAction.SHORT_NAME, "Close Window")
  putValue(Action.SHORT_DESCRIPTION, "Close the science program viewer and all open programs that it contains.")
  putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.SHIFT_DOWN_MASK | AbstractViewerAction.platformEventMask()))
  setEnabled(true)

  // Should always be enabled, as only appears in a window that CAN be closed.
  override def computeEnabledState() = true

  override def actionPerformed(e: ActionEvent) {
    viewer.closeWindow()
  }
}
