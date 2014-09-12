package edu.gemini.qv.plugin.ui

import scala.swing._
import scala.swing.Swing.EmptyBorder
import scala.swing.GridBagPanel.Fill.{Both, Horizontal}
import scala.swing.event.ButtonClicked
import edu.gemini.shared.gui.monthview.{DateSelectionMode, MonthView}
import edu.gemini.shared.gui.monthview.MonthView.Ymd

class CalendarDialog(label: String, start: Long = System.currentTimeMillis(), end: Long = System.currentTimeMillis(), mode: DateSelectionMode = DateSelectionMode.Single, numMonthsToShow: Int = 2, rows: Int = 1) extends Dialog() {
  var selectedDate: Option[Long] = None
  var startDate: Option[Long] = None
  var endDate: Option[Long] = None

  title     = label
  modal     = true
  resizable = false

  private val calendar = new MonthView(mode, numMonthsToShow, rows)
  mode match {
    case DateSelectionMode.Single => calendar.selection = Ymd(start)
    case DateSelectionMode.Interval => calendar.selectionInterval = (Ymd(start), Ymd(end))
  }

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
      selectedDate = Some(calendar.selection.timeAtMidnight)
      startDate = Some(calendar.selectionInterval._1.timeAtMidnight)
      endDate = Some(calendar.selectionInterval._2.timeAtMidnight)
      close()
      dispose()
    case ButtonClicked(`cancelButton`) =>
      close()
      dispose()
  }

  contents = contentsPanel
}
