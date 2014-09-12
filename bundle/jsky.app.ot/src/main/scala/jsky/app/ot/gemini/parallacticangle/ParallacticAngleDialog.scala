package jsky.app.ot.gemini.parallacticangle

import edu.gemini.shared.gui.textComponent.{NumberField, SelectOnFocus, TimeOfDayText}
import edu.gemini.shared.gui.monthview.{DateSelectionMode, MonthView}
import edu.gemini.shared.gui.monthview.MonthView.Ymd
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.obs.{SPObservation, SchedulingBlock}

import java.awt.Color
import java.util.{Date, TimeZone}
import javax.swing.BorderFactory

import scala.swing._
import scala.swing.Swing._
import scala.swing.event.{FocusLost, ButtonClicked}
import edu.gemini.spModel.inst.{ParallacticAngleDurationMode, ParallacticAngleDuration}
import edu.gemini.pot.sp.ISPObservation
import jsky.app.ot.util.TimeZonePreference

// Dialog to set the settings needed for the parallactic angle computation.

// This is horrific, but it seems that Scala Windows and Java Windows are not compatible.
class ParallacticAngleDialog(owner: java.awt.Window, observation: ISPObservation,
                             var duration: ParallacticAngleDuration, site: Site) extends Dialog {

  // Scheduling block.
  val schedulingBlock = observation.getDataObject.asInstanceOf[SPObservation].getSchedulingBlock.getOrElse(new SchedulingBlock(new Date().getTime, 0))

  // The start date and time managed by this widget.
  var startTime: Option[Long] = None
  private var _timeZone = TimeZonePreference.get

  title     = "Parallactic Angle Calculation"
  modal     = true
  resizable = false

  // We create a GridBagPanel that has 5 columns and 6 rows.
  lazy val ui = new GridBagPanel {
    import scala.swing.GridBagPanel.Fill._

    border = EmptyBorder(10, 10, 10, 10)

    // The instructional label.
    private val instructionLabel = new Label("Select the time and duration for the average parallactic angle calculation.")
    layout(instructionLabel) = new Constraints() {
      anchor     = GridBagPanel.Anchor.NorthWest
      gridx      = 0
      gridy      = 0
      gridwidth  = 5
    }

    // The date label and calendar.
    private val dateLabel = new Label("Date")
    layout(dateLabel) = new Constraints {
      anchor = GridBagPanel.Anchor.NorthWest
      gridx  = 0
      gridy  = 1
      insets = new Insets(12, 0, 0, 0)
    }

    val monthView = new MonthView(DateSelectionMode.Single, numMonthsToShow = 1)
    layout(monthView) = new Constraints() {
      anchor = GridBagPanel.Anchor.West
      gridx      = 1
      gridy      = 1
      gridheight = 3
      insets     = new Insets(10, 10, 0, 0)
    }

    // The time label and widget.
    private val timeLabel = new Label("Time")
    layout(timeLabel) = new Constraints() {
      anchor = GridBagPanel.Anchor.NorthWest
      gridx  = 0
      gridy  = 4
      insets = new Insets(12, 0, 0, 0)
    }

    val timeEditor = new TextField(8) with SelectOnFocus with TimeOfDayText

    object localButton extends RadioButton(s"Local (${site.abbreviation})") {
      focusable = false
    }
    listenTo(localButton)

    object utcButton extends RadioButton("UTC") {
      focusable = false
    }
    listenTo(utcButton)

    new ButtonGroup(localButton, utcButton) {
      select(if (_timeZone == TimeZone.getTimeZone("UTC")) utcButton else localButton)
    }

    reactions += {
      case ButtonClicked(`localButton`) => timeZone = site.timezone
      case ButtonClicked(`utcButton`)   => timeZone = TimeZone.getTimeZone("UTC")
    }

    val timePanel = new GridBagPanel {
      layout(timeEditor) = new Constraints() {
        gridx = 0
      }

      layout(localButton) = new Constraints() {
        gridx  = 1
        insets = new Insets(0, 10, 0, 0)
      }

      layout(utcButton) = new Constraints() {
        gridx = 2
       insets = new Insets(0, 10, 0, 0)
      }

      layout(HGlue) = new Constraints() {
        gridx = 3
        fill = Horizontal
        weightx = 1.0
      }
    }

    layout(timePanel) = new Constraints() {
      anchor = GridBagPanel.Anchor.West
      gridx  = 1
      gridy  = 4
      insets = new Insets(10, 10, 0, 0)
    }


    // The duration field information.
    private val durationLabel = new Label("Duration")
    layout(durationLabel) = new Constraints() {
      anchor     = GridBagPanel.Anchor.NorthWest
      gridx      = 2
      gridy      = 1
      gridwidth  = 3
      insets     = new Insets(12, 20, 0, 0)
    }

    object remainingTimeButton extends RadioButton {
      val remainingTime = ParallacticAngleDuration.calculateRemainingTime(observation).toDouble / 1000 / 60
      text = f"Use Remaining Execution Time Estimate ($remainingTime%.1f min)"
    }
    layout(remainingTimeButton) = new Constraints() {
      anchor     = GridBagPanel.Anchor.NorthWest
      gridx      = 2
      gridy      = 2
      gridwidth  = 3
      insets     = new Insets(10, 20, 0, 0)
    }

    object setToButton extends RadioButton("Set To:")
    layout(setToButton) = new Constraints() {
      anchor     = GridBagPanel.Anchor.NorthWest
      gridx      = 2
      gridy      = 3
      insets     = new Insets(10, 20, 0, 0)
    }

    listenTo(remainingTimeButton, setToButton)
    reactions += {
      case ButtonClicked(`remainingTimeButton`) => durationField.enabled = false
      case ButtonClicked(`setToButton`)         => durationField.enabled = true
    }

    // Set the number of minutes of duration, converting from ms.
    val durationField = new NumberField(Some(duration.getExplicitDuration / 60000.0)) {
      peer.setColumns(5)
      enabled = duration.getParallacticAngleDurationMode == ParallacticAngleDurationMode.EXPLICITLY_SET
    }

    // Reset duration to 0.0 if nonsense is typed in and the focus is lost.
    listenTo(durationField)
    reactions += {
      case FocusLost(`durationField`,_,_) => if (!durationField.valid) durationField.text = "0"
    }

    layout(durationField) = new Constraints() {
      anchor     = GridBagPanel.Anchor.NorthWest
      gridx      = 3
      gridy      = 3
      insets     = new Insets(10, 10, 0, 0)
    }

    private val minLabel = new Label("min") {
      foreground = Color.black
    }
    layout(minLabel) = new Constraints() {
      anchor     = GridBagPanel.Anchor.NorthWest
      gridx      = 4
      gridy      = 3
      insets     = new Insets(12, 10, 0, 0)
    }

    new ButtonGroup(remainingTimeButton, setToButton) {
      if (duration.getParallacticAngleDurationMode == ParallacticAngleDurationMode.REMAINING_TIME) select(remainingTimeButton)
      else select(setToButton)
    }

    // Create the OK and Cancel buttons.
    object cancelButton extends Button("Cancel") {
      focusable = false
    }
    object okButton extends Button("Ok") {
      focusable = false
    }

    object buttonPanel extends GridBagPanel {
      border = BorderFactory.createCompoundBorder(
                 BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                 BorderFactory.createEmptyBorder(10, 0, 0, 0))

      layout(Swing.HGlue) = new Constraints() {
        fill    = Horizontal
        weightx = 1.0
      }
      layout(okButton)  = new Constraints() {
        gridx   = 1
        insets  = new Insets(0, 0, 0, 5)
      }
      layout(cancelButton)  = new Constraints() {
        gridx   = 2
      }
    }

    layout(buttonPanel) = new Constraints() {
      gridx     = 0
      gridy     = 5
      fill      = Horizontal
      weightx   = 1.0
      gridwidth = 5
      insets    = new Insets(10, 0, 0, 0)
    }

    def selection(tz: TimeZone): Long =
      monthView.selection.timeAtMidnight(tz) + timeEditor.hms.milliSec

    def displayTime(time: Long, tz: TimeZone): Unit = {
      val ymd = Ymd(time, tz)
      val hms = TimeOfDayText.Hms(time, tz)

      monthView.selection = ymd
      timeEditor.hms      = hms
    }

    // The duration managed by this widget.
    def fetchDuration: ParallacticAngleDuration = {
      val mode = {
        if (remainingTimeButton.selected) ParallacticAngleDurationMode.REMAINING_TIME
        else ParallacticAngleDurationMode.EXPLICITLY_SET
      }
      ParallacticAngleDuration.getInstance(mode, (durationField.text.toDouble * 60 * 1000).toLong)
    }

    listenTo(okButton, cancelButton)
    reactions += {
      case ButtonClicked(`okButton`)     =>
        startTime = Some(selection(_timeZone))
        duration = fetchDuration

        TimeZonePreference.set(_timeZone)
        close()
        dispose()
      case ButtonClicked(`cancelButton`) =>
        close()
        dispose()
    }

    displayTime(schedulingBlock.start, _timeZone)
  }

  def timeZone: TimeZone = _timeZone

  def timeZone_=(tz: TimeZone): Unit = {
    if (tz != _timeZone) {
      ui.displayTime(ui.selection(_timeZone), tz)
      _timeZone = tz
    }
  }

  contents = ui

  // Now center on the Java window.
  pack()
  location = owner.getLocationOnScreen
}
