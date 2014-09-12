package edu.gemini.qv.plugin.selector

import edu.gemini.qv.plugin.{ReferenceDateChanged, QvContext}
import edu.gemini.qv.plugin.ui.QvGui.ActionButton
import edu.gemini.qv.plugin.ui.{QvGui, CalendarDialog}
import java.text.SimpleDateFormat
import java.util.Date
import scala.swing._

/**
 * UI component that allows to interact with the reference data in a shared time selection model object.
 */
class ReferenceDateSelector(ctx: QvContext) extends FlowPanel {

  private val DateFormat = new SimpleDateFormat("MMM dd yyyy, HH:mm")

  private val refDateLabel = new Label(refDateToLabel)

  private val calendarButton = ActionButton(
    "",
    "Select a reference date for non-sidereal targets.",
    (button: Button) => {
      val cd = new CalendarDialog("Select Reference Date for Non-Sidereal Targets", ctx.referenceDate, ctx.referenceDate)
      cd.selectedDate = Some(ctx.referenceDate)
      cd.pack()
      cd.setLocationRelativeTo(button)
      cd.visible = true
      cd.selectedDate.foreach {
        time => ctx.referenceDate = time
      }
    },
    QvGui.CalendarIcon
  )

  private val todayButton = ActionButton(
    "Today",
    "Set reference date for non-sidereal targets to today's middle night time.",
    () => {
      ctx.referenceDate = System.currentTimeMillis()
    }
  )

  contents += refDateLabel
  contents += Swing.HStrut(5)
  contents += calendarButton
  contents += todayButton

  listenTo(ctx)
  reactions += {
    case ReferenceDateChanged =>
      refDateLabel.text = refDateToLabel
  }

  private def refDateToLabel =
    "Reference date for non-sidereal targets: " +
    DateFormat.format(new Date(ctx.referenceDate)) +
    " (middle night time)"

}
