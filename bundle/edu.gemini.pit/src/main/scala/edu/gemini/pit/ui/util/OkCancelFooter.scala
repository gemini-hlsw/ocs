package edu.gemini.pit.ui.util
import scala.swing.event.ButtonClicked
import scala.swing.BorderPanel
import scala.swing.Button
import scala.swing.Dialog
import scala.swing.FlowPanel

object OkCancelFooter {
  def apply(dialog:Dialog)(f : => Unit) = new OkCancelFooter(dialog)(() => f)
}

class OkCancelFooter private (dialog: Dialog)(ok: () => Unit) extends BorderPanel {
  add(new FlowPanel {
    contents += OkButton
    contents += new Button("Cancel") {
      reactions += {
        case ButtonClicked(_) => dialog.close()
      }
    }
  }, BorderPanel.Position.East)
  
  object OkButton extends Button("Ok") {
    reactions += {
      case ButtonClicked(_) => ok(); dialog.close()
    }
    dialog.peer.getRootPane.setDefaultButton(peer)
  }
  
}
