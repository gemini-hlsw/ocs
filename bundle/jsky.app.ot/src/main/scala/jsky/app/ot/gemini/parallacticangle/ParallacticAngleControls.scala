package jsky.app.ot.gemini.parallacticangle

import java.awt.{Color, Insets}
import java.text.{Format, SimpleDateFormat}
import java.util.Date
import javax.swing.BorderFactory
import javax.swing.border.EtchedBorder

import edu.gemini.skycalc.Angle
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.inst.ParallacticAngleSupport
import edu.gemini.spModel.obs.{ObsTargetCalculatorService, SPObservation, SchedulingBlock}
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.shared.util.immutable.{ Option => JOption }
import jsky.app.ot.editor.OtItemEditor
import jsky.app.ot.util.TimeZonePreference

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing._
import scala.swing.event.{ButtonClicked, Event}

/**
 * This class encompasses all of the logic required to manage the average parallactic angle information associated
 * with an instrument configuration.
 */
class ParallacticAngleControls(showParallacticAngleFeedback: Boolean) extends GridBagPanel with Publisher {
  private var editor:    Option[OtItemEditor[_, _]] = None
  private var site:      Option[Site]   = None
  private var formatter: Option[Format] = None

  object ui {
    object relativeTimeMenu extends Menu("Set To:") {
      private val incrementsInMinutes = List(10, 20, 30, 45, 60)

      private case class RelativeTime(desc: String, timeInMs: Long) extends MenuItem(desc) {
        action = Action(desc) {
          updateSchedulingBlock(SchedulingBlock(System.currentTimeMillis, timeInMs))
        }
      }

      horizontalTextPosition = Alignment.Left
      horizontalAlignment    = Alignment.Left
      iconTextGap            = 10
      icon                   = Resources.getIcon("eclipse/menu-trimmed.gif")
      margin                 = new Insets(-1, -10, -1, -5)

      def rebuild(): Unit = {
        contents.clear()

        // menu items that don't depend on the context
        val fixedItems = RelativeTime("Now", 0) :: incrementsInMinutes.map(m => RelativeTime(s"Now + $m min", m * 60000))

        // menu items that require an observation and instrument to compute
        val instItems = for {
          e    <- editor
          obs  <- Option(e.getContextObservation)
          inst <- Option(e.getContextInstrumentDataObject)
        } yield {
          // For some ridiculous reason, setup and reacq time is provided as
          // floating point seconds by the instrument implementations :/
          val setupTimeMs = math.round(inst.getSetupTime(obs) * 1000)
          val reacqTimeMs = math.round(inst.getReacquisitionTime(obs) * 1000)
          def formatMin(ms: Long): String = s"(${math.round(ms/60000.0)} min)"

          List(
            RelativeTime(s"Now + Setup ${formatMin(setupTimeMs)}",  setupTimeMs),
            RelativeTime(s"Now + Reacq. ${formatMin(reacqTimeMs)}", reacqTimeMs)
          )
        }

        contents ++= instItems.getOrElse(Nil) ++ fixedItems
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
  def init(e: OtItemEditor[_, _], s: Option[Site], f: Format): Unit = {
    editor    = Some(e)
    site      = s
    formatter = Some(f)
    ui.relativeTimeMenu.rebuild()
    resetComponents()
  }

  def init(e: OtItemEditor[_, _], s: Site, f: Format): Unit =
    init(e, Some(s), f)

  def init(e: OtItemEditor[_, _], s: JOption[Site], f: Format): Unit =
    init(e, s.asScalaOpt, f)

  /**
   * Call this whenever the state of the parallactic controls change
   */
  private def updateSchedulingBlock(sb: SchedulingBlock): Unit =
    for {
      e      <- editor
      ispObs <- Option(e.getContextObservation)
    } {
      val spObs = ispObs.getDataObject.asInstanceOf[SPObservation]

      // Set the scheduling block.
      spObs.setSchedulingBlock(Some(sb).asGeminiOpt)
      ispObs.setDataObject(spObs)

      // Update the components to reflect the change.
      resetComponents()
    }


  /**
   * Triggered when the date time button is clicked. Shows the ParallacticAngleDialog to allow the user to
   * explicitly set a date and duration for the parallactic angle calculation.
   */
  private def displayParallacticAngleDialog(): Unit =
    for {
      e <- editor
      o <- Option(e.getContextObservation)
    } {
      val dialog = new ParallacticAngleDialog(
        e.getViewer.getParentFrame,
        o,
        o.getDataObject.asInstanceOf[SPObservation].getSchedulingBlock.asScalaOpt,
        site.map(_.timezone))
      dialog.pack()
      dialog.visible = true
      updateSchedulingBlock(dialog.schedulingBlock)
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
      ispObservation <- Option(e.getContextObservation)
      spObservation  = ispObservation.getDataObject.asInstanceOf[SPObservation]
      sb             <- spObservation.getSchedulingBlock.asScalaOpt
      fmt            <- formatter
    } {
      //object dateFormat extends SimpleDateFormat("MM/dd/yy 'at' HH:mm:ss z") {
      object dateFormat extends SimpleDateFormat("MM/dd/yy HH:mm:ss z") {
        setTimeZone(TimeZonePreference.get)
      }
      val dateTimeStr = dateFormat.format(new Date(sb.start))

      // Include tenths of a minute if not even.
      val duration = sb.duration.getOrElse(ParallacticAngleDialog.calculateRemainingTime(ispObservation)) / 60000.0
      val durationFmt = if (Math.round(duration * 10) == (Math.floor(duration) * 10).toLong) "%.0f" else "%.1f"
      val when = s"$dateTimeStr, ${durationFmt.format(duration)}m"

      ui.parallacticAngleFeedback.text =
        e.getDataObject match {
          case p: ParallacticAngleSupport if showParallacticAngleFeedback =>
            parallacticAngle.fold(
              s"Target not visible ($when)")(angle =>
              s"${fmt.format(ParallacticAngleControls.angleToDegrees(angle))}\u00b0 ($when)")
          case _ =>
            s"$when"
        }

      publish(ParallacticAngleControls.ParallacticAngleChangedEvent)
    }
  }


  /**
   * The parallactic angle calculation, if it can be calculated
   */
  def parallacticAngle: Option[Angle] =
    for {
      e <- editor
      o <- Option(e.getContextObservation)
      a <- e.getDataObject match {
        case p: ParallacticAngleSupport => p.calculateParallacticAngle(o).asScalaOpt
        case _                          => None
      }
    } yield a
}

object ParallacticAngleControls {
  case object ParallacticAngleChangedEvent extends Event

  def angleToDegrees(a: Angle): Double = a.toPositive.toDegrees.getMagnitude
}