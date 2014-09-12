package jsky.app.ot.visitlog

import edu.gemini.shared.gui.monthview.MonthView

import scala.swing._
import scala.swing.Swing.EmptyBorder
import scala.swing.GridBagPanel.Fill.{Both, Horizontal}
import scala.swing.event.ButtonClicked

class CalendarDialog(owner: Window) extends Dialog(owner) {
  var selectedDate: Option[Long] = None

  title     = "Select Observing Night"
  modal     = true
  resizable = false

  private val calendar = new MonthView()

  object contentsPanel extends GridBagPanel {
    border = EmptyBorder(10,10,10,10)

    layout(calendar) = new Constraints() {
      gridy   = 1
      fill    = Both
      weightx = 1.0
      weighty = 1.0
    }

    object cancelButton extends Button("Cancel") {
      focusable = false
    }

    object okButton extends Button("Ok") {
      focusable = false
    }

    object buttonPanel extends GridBagPanel {
      layout(Swing.HGlue) = new Constraints() {
        fill = Horizontal
        weightx = 1.0
      }
      layout(okButton)  = new Constraints() {
        gridx  = 1
        insets = new Insets(0,0, 0, 5)
      }
      layout(cancelButton)  = new Constraints() {
        gridx  = 2
      }
    }

    layout(buttonPanel) = new Constraints() {
      gridy   = 2
      fill    = Horizontal
      weightx = 1.0
      insets  = new Insets(10,0,0,0)
    }

  }

  import contentsPanel.{cancelButton, okButton}
  listenTo(cancelButton, okButton)
  reactions += {
    case ButtonClicked(`okButton`)     =>
      val ymd = calendar.selection
      selectedDate = Some(ymd.timeAtMidnight)
      close()
      dispose()
    case ButtonClicked(`cancelButton`) =>
      close()
      dispose()
  }

  contents = contentsPanel
}
