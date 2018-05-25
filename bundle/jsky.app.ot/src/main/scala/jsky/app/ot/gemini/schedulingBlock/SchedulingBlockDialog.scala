package jsky.app.ot.gemini.schedulingBlock

import java.awt.Color
import java.util.TimeZone
import javax.swing.BorderFactory

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.shared.gui.monthview.MonthView.Ymd
import edu.gemini.shared.gui.monthview.{DateSelectionMode, MonthView}
import edu.gemini.shared.gui.textComponent.{NumberField, SelectOnFocus, TimeOfDayText}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.obs.{ ObsTargetCalculatorService, SchedulingBlock, SPObservation }
import edu.gemini.spModel.obs.SchedulingBlock.Duration
import edu.gemini.spModel.obs.SchedulingBlock.Duration._
import jsky.app.ot.util.TimeZonePreference

import scala.swing._
import scala.swing.Swing._
import scala.swing.GridBagPanel.Anchor._
import scala.swing.event.{ButtonClicked, FocusLost, ValueChanged}

import SchedulingBlockDialog._

/**
 * Dialog for selecting the time and (optionally) the duration of a scheduling
 * block.
 */
final class SchedulingBlockDialog(
  observation: ISPObservation,
  site:        Option[Site],
  mode:        Mode
) extends Dialog {

  private object state {
    var schedulingBlock: Option[SchedulingBlock] =
      observation.getDataObject.asInstanceOf[SPObservation].getSchedulingBlock.asScalaOpt

    var timeZone: TimeZone =
      site.map(_.timezone).filter(_ == TimeZonePreference.get).getOrElse(Utc)
  }

  modal     = true
  resizable = false

  // We create a GridBagPanel that has 5 columns and 6 rows.
  private object ui extends GridBagPanel {
    import scala.swing.GridBagPanel.Fill._

    border = EmptyBorder(10, 10, 10, 10)

    // The instructional label.
    object instructionLabel extends Label {
      visible = false
    }
    layout(instructionLabel) = new Constraints() {
      anchor     = NorthWest
      gridx      = 0
      gridy      = 0
      gridwidth  = 3
    }

    // The date label and calendar.
    val dateLabel = new Label("Date")
    layout(dateLabel) = new Constraints {
      anchor = NorthWest
      gridx  = 0
      gridy  = 1
      insets = new Insets(12, 0, 0, 0)
    }

    val monthView = new MonthView(DateSelectionMode.Single, numMonthsToShow = 1)
    layout(monthView) = new Constraints() {
      anchor     = West
      gridx      = 1
      gridy      = 1
      gridheight = 3
      insets     = new Insets(10, 10, 0, 0)
    }

    // The time label and widget.
    val timeLabel = new Label("Time")
    layout(timeLabel) = new Constraints() {
      anchor = NorthWest
      gridx  = 0
      gridy  = 4
      insets = new Insets(12, 0, 0, 0)
    }

    def tzPanel(s: Site): Panel =
      new FlowPanel {
        hGap = 0

        object localButton extends RadioButton(s"Local (${s.timezone.getDisplayName(false, TimeZone.SHORT)})") {
          focusable = false
        }
        listenTo(localButton)

        object utcButton extends RadioButton(Utc.getDisplayName(false, TimeZone.SHORT)) {
          focusable = false
        }
        listenTo(utcButton)

        new ButtonGroup(localButton, utcButton) {
          select(if (state.timeZone == Utc) utcButton else localButton)
        }

        reactions += {
          case ButtonClicked(`localButton`) => updateTimeZone(s.timezone)
          case ButtonClicked(`utcButton`)   => updateTimeZone(Utc)
        }

        contents += localButton
        contents += Swing.HStrut(10)
        contents += utcButton
      }

    val timeEditor = new TextField(8) with SelectOnFocus with TimeOfDayText

    object timePanel extends GridBagPanel {
      layout(timeEditor) = new Constraints() {
        gridx = 0
      }

      val tz = site.fold(new Label("UTC"): Component)(tzPanel)

      layout(tz) = new Constraints() {
        gridx  = 1
        insets = new Insets(0, 10, 0, 0)
      }

      layout(HGlue) = new Constraints() {
        gridx   = 2
        fill    = Horizontal
        weightx = 1.0
      }
    }

    layout(timePanel) = new Constraints() {
      anchor = West
      gridx  = 1
      gridy  = 4
      insets = new Insets(10, 10, 0, 0)
    }


    object durationPanel extends GridBagPanel {

      val durationLabel = new Label("Duration")
      layout(durationLabel) = new Constraints() {
        anchor     = West
        gridx      = 0
        gridy      = 0
      }

      object remainingTimeButton extends RadioButton {
        val remainingTime = ObsTargetCalculatorService.calculateRemainingTime(observation).toDouble / 1000 / 60
        text = f"Use Remaining Execution Time Estimate ($remainingTime%.1f min)"
      }
      layout(remainingTimeButton) = new Constraints() {
        anchor     = West
        gridx      = 0
        gridy      = 1
        insets     = new Insets(10, 0, 0, 0)
      }

      object setToPanel extends FlowPanel {

        hGap = 0

        object button extends RadioButton("Set To:")

        // Set the number of minutes of duration, converting from ms.
        val initialMs     = state.schedulingBlock.flatMap(_.duration.toOption).getOrElse(0L)
        val durationField = new NumberField(Some(initialMs / 60000.0), allowEmpty = false) {
          peer.setColumns(5)
          enabled = state.schedulingBlock.exists(_.duration.isExplicit)
        }

        object minLabel extends Label {
          text       = "min"
          foreground = Color.black
        }

        contents += button
        contents += Swing.HStrut(10)
        contents += durationField
        contents += Swing.HStrut(10)
        contents += minLabel
      }

      layout(setToPanel) = new Constraints() {
        anchor = West
        gridx  = 0
        gridy  = 2
        insets = new Insets(10, 0, 0, 0)
      }

      listenTo(remainingTimeButton, setToPanel.button)
      reactions += {
        case ButtonClicked(`remainingTimeButton`) =>
          setToPanel.durationField.enabled = false
          okButton.enabled                 = true
        case ButtonClicked(`setToPanel`.button)   =>
          setToPanel.durationField.enabled = true
          okButton.enabled                 = setToPanel.durationField.valid
      }

      // Reset duration to 0.0 if nonsense is typed in and the focus is lost.
      listenTo(setToPanel.durationField)
      reactions += {
        case FocusLost(`setToPanel`.durationField, _, _) =>
          okButton.enabled = setToPanel.durationField.valid
        case ValueChanged(_) =>
          okButton.enabled = setToPanel.durationField.valid
      }

      new ButtonGroup(remainingTimeButton, setToPanel.button) {
        if (state.schedulingBlock.exists(_.duration.isExplicit)) select(setToPanel.button)
        else select(remainingTimeButton)
      }
    }

    if (mode == DateTimeAndDuration) {
      layout(durationPanel) = new Constraints() {
        gridx = 2
        gridy = 1
        insets = new Insets(12, 20, 0, 0)
      }
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
      gridwidth = 3
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
    def fetchDuration: Duration =
      if (durationPanel.setToPanel.button.selected)
        Explicit((durationPanel.setToPanel.durationField.text.toDouble * 60 * 1000).toLong)
      else
        Computed(ObsTargetCalculatorService.calculateRemainingTime(observation))

    listenTo(okButton, cancelButton)
    reactions += {
      case ButtonClicked(`okButton`)     =>
        state.schedulingBlock = Some(SchedulingBlock(selection(state.timeZone), fetchDuration))
        TimeZonePreference.set(state.timeZone)
        close()
        dispose()
      case ButtonClicked(`cancelButton`) =>
        state.schedulingBlock = Option.empty
        close()
        dispose()
    }

    displayTime(state.schedulingBlock.map(_.start).getOrElse(System.currentTimeMillis), state.timeZone)
  }

  def instructions: String =
    ui.instructionLabel.text

  def instructions_=(text: String): Unit = {
    ui.instructionLabel.text    = text
    ui.instructionLabel.visible = true
  }

  def updateTimeZone(tz: TimeZone): Unit = {
    if (tz != state.timeZone) {
      ui.displayTime(ui.selection(state.timeZone), tz)
      state.timeZone = tz
    }
  }

  def schedulingBlock: Option[SchedulingBlock] =
    state.schedulingBlock

  contents = ui

}

object SchedulingBlockDialog {

  val Utc = TimeZone.getTimeZone("UTC")

  sealed trait Mode extends Product with Serializable
  case object DateTimeOnly        extends Mode
  case object DateTimeAndDuration extends Mode

  def prompt(
    title:        String,
    instructions: Option[String],
    owner:        java.awt.Component,
    observation:  ISPObservation,
    site:         Option[Site],
    mode:         Mode
  ): Option[SchedulingBlock] = {
    val dialog = new SchedulingBlockDialog(observation, site, mode)

    dialog.location = owner.getLocationOnScreen
    dialog.title    = title
    instructions.foreach(text => dialog.instructions = text)
    dialog.pack()
    dialog.visible  = true

    dialog.schedulingBlock
  }
}