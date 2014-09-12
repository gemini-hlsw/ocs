package edu.gemini.pit.ui.util
import scala.swing.event.ButtonClicked
import scala.swing.BorderPanel
import scala.swing.Button
import scala.swing.Dialog
import scala.swing.FlowPanel

object
OkFooter {
  def apply(dialog:Dialog)(f : => Unit) = new OkFooter(dialog)(() => f)
}

class OkFooter private (dialog: Dialog)(ok: () => Unit) extends BorderPanel {
  add(new FlowPanel {
    contents += OkButton
  }, BorderPanel.Position.East)
  
  object OkButton extends Button("Ok") {
    reactions += {
      case ButtonClicked(_) => ok(); dialog.close()
    }
    dialog.peer.getRootPane.setDefaultButton(peer)
  }
  
}
