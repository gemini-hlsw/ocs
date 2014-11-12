package jsky.app.ot.gemini.parallacticangle

import java.awt.{Color, Insets}
import java.text.{Format, SimpleDateFormat}
import java.util.Date
import javax.swing.BorderFactory
import javax.swing.border.EtchedBorder

import edu.gemini.mask.Resources
import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.skycalc.Angle
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.inst.{ParallacticAngleDuration, ParallacticAngleDurationMode, ParallacticAngleSupport}
import edu.gemini.spModel.obs.{ObsTargetCalculatorService, SPObservation, SchedulingBlock}
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.rich.shared.immutable._
import jsky.app.ot.editor.OtItemEditor
import jsky.app.ot.util.TimeZonePreference

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing._
import scala.swing.event.{ButtonClicked, Event}

/**
 * This class encompasses all of the logic required to manage the average parallactic angle information associated
 * with an instrument configuration.
 */
class ParallacticAngleControls extends GridBagPanel with Publisher {
  private var editor:    Option[ParallacticAngleControls.Editor] = None
  private var site:      Option[Site]   = None
  private var formatter: Option[Format] = None

  object ui {
    object relativeTimeMenu extends Menu("Set To:") {
      private val incrementsInMinutes = List(10, 20, 30, 45, 60)

      private case class RelativeTime(desc: String, timeInMinutes: Double) extends MenuItem(desc) {
        private val timeInMs = (timeInMinutes * 60 * 1000).toLong
        action = Action(desc) {
          updateSchedulingBlock(new Date().getTime + timeInMs)
        }
      }

      horizontalTextPosition = Alignment.Left
      horizontalAlignment    = Alignment.Left
      iconTextGap            = 10
      icon                   = Resources.getIcon("eclipse/menu-trimmed.gif")
      margin                 = new Insets(-1, -10, -1, -5)

      def rebuild(): Unit = {
        contents.clear()
        editor.foreach(e => {
          val obs                        = e.getContextObservation
          val inst                       = e.getContextInstrumentDataObject
          val setupTimeInMinutes         = inst.getSetupTime(obs) / 60
          val reacquisitionTimeInMinutes = inst.getReacquisitionTime(obs) / 60;

          contents ++= List(
            RelativeTime(s"Now + Setup (${setupTimeInMinutes.toLong} min)", setupTimeInMinutes),
            RelativeTime(s"Now + Reacq. (${reacquisitionTimeInMinutes.toLong}} min)", reacquisitionTimeInMinutes),
            RelativeTime("Now", 0)
          ) ++ incrementsInMinutes.map(m => RelativeTime(s"Now + $m min", m))
        })
      }
    }

    private object relativeTimeMenuBar extends MenuBar {
      contents    += relativeTimeMenu
      border      =  BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)
      minimumSize =  preferredSize
      tooltip     = "Select a duration for the average parallactic angle calculation from the current time."
    }
    layout(relativeTimeMenuBar) = new Constraints() {
      anchor = Anchor.West
    }

    private object dateTimeButton extends Button {
      icon    = Resources.getIcon("dates.gif")
      tooltip = "Select the time and duration for the average parallactic angle calculation."
    }
    layout(dateTimeButton) = new Constraints() {
      gridx  = 1
      anchor = Anchor.West
      insets = new Insets(0, 10, 0, 0)
    }
    listenTo(dateTimeButton)
    reactions += {
      case ButtonClicked(`dateTimeButton`) => displayParallacticAngleDialog()
    }

    object parallacticAngleFeedback extends Label {
      foreground             = Color.black
      horizontalAlignment    = Alignment.Left
      iconTextGap            = iconTextGap - 2

      def warningState(warn: Boolean): Unit =
        icon = if (warn) Resources.getIcon("eclipse/alert.gif") else Resources.getIcon("eclipse/blank.gif")
    }
    layout(parallacticAngleFeedback) = new Constraints() {
      gridx   = 2
      weightx = 1.0
      anchor  = Anchor.West
      fill    = Fill.Horizontal
      insets  = new Insets(0, 10, 0, 0)
    }
  }


  /**
   * Initialize the UI and set the instrument editor to allow for the parallactic angle updates.
   */
  def init(e: ParallacticAngleControls.Editor, s: Site, f: Format): Unit = {
    editor    = Some(e)
    site      = Some(s)
    formatter = Some(f)
    ui.relativeTimeMenu.rebuild()
    resetComponents()
  }


  /**
   * Call this whenever the state of the parallactic controls change
   */
  private def updateSchedulingBlock(start: Long): Unit = {
    editor.foreach(e => {
      val ispObs = e.getContextObservation
      val spObs = ispObs.getDataObject.asInstanceOf[SPObservation]

      // Calculate the duration.
      // Create a new scheduling block with now as the start time, and the remaining exec time as the duration.
      val parallacticAngleDuration = e.getDataObject.getParallacticAngleDuration
      val duration =
        if (parallacticAngleDuration.getParallacticAngleDurationMode.equals(ParallacticAngleDurationMode.EXPLICITLY_SET))
          parallacticAngleDuration.getExplicitDuration
        else
          ParallacticAngleDuration.calculateRemainingTime(ispObs)

      // Set the scheduling block.
      spObs.setSchedulingBlock(Some(new SchedulingBlock(start, duration)).asGeminiOpt)
      ispObs.setDataObject(spObs)

      // Update the components to reflect the change.
      resetComponents()
    })
  }


  /**
   * Triggered when the date time button is clicked. Shows the ParallacticAngleDialog to allow the user to
   * explicitly set a date and duration for the parallactic angle calculation.
   */
  private def displayParallacticAngleDialog(): Unit =
    for {
      e <- editor
      s <- site
    } yield {
      val dialog = new ParallacticAngleDialog(
        e.getViewer.getParentFrame,
        e.getContextObservation,
        e.getDataObject.getParallacticAngleDuration,
        s)
      dialog.pack()
      dialog.visible = true

      // As the dialog is modal, this code only executes after it is dismissed. If the dialog was successfully
      // configured, update the scheduling block.
      dialog.startTime.foreach(st => {
        e.getDataObject.setParallacticAngleDuration(dialog.duration)
        updateSchedulingBlock(st)
      })
    }


  /**
   * This should be called whenever the position angle changes to compare it to the parallactic angle.
   * A warning icon is displayed if the two are different. This is a consequence of allowing the user to
   * set the PA to something other than the parallactic angle, even when it is selected.
   */
  def positionAngleChanged(positionAngleText: String): Unit = {
    // We only do this if the parallactic angle can be calculated, and is different from the PA.
    for {
      e     <- editor
      angle <- parallacticAngle
      fmt   <- formatter
    } yield {
      val explicitlySet = !fmt.format(ParallacticAngleControls.angleToDegrees(angle)).equals(positionAngleText) &&
                          !fmt.format(ParallacticAngleControls.angleToDegrees(angle.add(Angle.ANGLE_PI))).equals(positionAngleText)
      ui.parallacticAngleFeedback.warningState(explicitlySet)
    }
  }


  /**
   * This should be called whenever the parallactic angle components need to be reinitialized, and at initialization.
   */
  def resetComponents(): Unit = {
    ui.parallacticAngleFeedback.text = ""
    for {
      e              <- editor
      ispObservation = e.getContextObservation
      if ObsTargetCalculatorService.targetCalculation(ispObservation).isDefined
      spObservation  = ispObservation.getDataObject.asInstanceOf[SPObservation]
      sb             <- spObservation.getSchedulingBlock.asScalaOpt
      fmt            <- formatter
    } yield {
      object dateFormat extends SimpleDateFormat("MM/dd/yy 'at' HH:mm:ss z") {
        setTimeZone(TimeZonePreference.get)
      }
      val dateTimeStr = dateFormat.format(new Date(sb.start))

      // Include tenths of a minute if not even.
      val duration = sb.duration / 60000.0
      val durationFmt = if (Math.round(duration * 10) == (Math.floor(duration) * 10).toLong) "%.0f" else "%.1f"
      val durationStr = durationFmt.format(duration)
      val pluralOrNot = if ("1".equals(durationStr)) "" else "s"
      val when = s"($dateTimeStr, for $durationStr min$pluralOrNot)"

      ui.parallacticAngleFeedback.text = parallacticAngle.fold(s"Target not visible $when")(angle =>
        s"${fmt.format(ParallacticAngleControls.angleToDegrees(angle))}\u00b0 $when")

      publish(ParallacticAngleControls.ParallacticAngleChangedEvent)
    }
  }


  /**
   * The parallactic angle calculation, if it can be calculated
   */
  def parallacticAngle: Option[Angle] =
    editor.map(e => e.getDataObject.calculateParallacticAngle(e.getContextObservation).asScalaOpt).flatten
}

object ParallacticAngleControls {
  type Editor = OtItemEditor[ISPObsComponent, SPInstObsComp with ParallacticAngleSupport]

  case object ParallacticAngleChangedEvent extends Event

  def angleToDegrees(a: Angle): Double = a.toPositive.toDegrees.getMagnitude
}