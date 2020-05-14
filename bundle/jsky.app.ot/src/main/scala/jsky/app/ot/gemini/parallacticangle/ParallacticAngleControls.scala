package jsky.app.ot.gemini.parallacticangle

import java.awt.{Color, Insets}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.text.Format
import java.util.logging.Logger
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

import javax.swing.{BorderFactory, Icon}
import javax.swing.border.EtchedBorder
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.inst.ParallacticAngleSupport
import edu.gemini.spModel.obs.{ObsTargetCalculatorService, SPObservation, SchedulingBlock}
import edu.gemini.spModel.obs.SchedulingBlock.Duration
import edu.gemini.spModel.obs.SchedulingBlock.Duration._
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.shared.util.immutable.{Option => JOption}
import edu.gemini.spModel.obscomp.SPInstObsComp
import jsky.app.ot.editor.OtItemEditor
import jsky.app.ot.gemini.schedulingBlock.{SchedulingBlockDialog, SchedulingBlockUpdate}
import jsky.app.ot.util.TimeZonePreference
import jsky.util.Resources

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing._
import scala.swing.event.{ButtonClicked, Event}
import scalaz._
import Scalaz._

/**
  * This class encompasses all of the logic required to manage the average parallactic angle information associated
  * with an instrument configuration.
  *
  * @param isPaUi should be true for PA controls, false for Scheduling Block controls
  */
class ParallacticAngleControls(isPaUi: Boolean) extends GridBagPanel with Publisher {
  import ParallacticAngleControls._

  val Nop: Runnable = new Runnable {
    override def run(): Unit = ()
  }

  private var editor:    Option[OtItemEditor[_, _]] = None
  private var site:      Option[Site]   = None
  private var formatter: Option[Format] = None
  private var callback:  Runnable = Nop

  def getIcon(name: String): Icon =
    Resources.getIcon(name, classOf[ParallacticAngleControls])

  object ui {
    object relativeTimeMenu extends Menu("Set To:") {
      private val incrementsInMinutes = List(5, 10, 20, 30, 45, 60)

      private case class RelativeTime(desc: String, timeInMs: Long) extends MenuItem(desc) {
        action = Action(desc) {

          // Our start time, based on current clock time plus an offset
          val start = System.currentTimeMillis + timeInMs

          // New duration based on remaining time, if any
          lazy val remainingDuration = remainingTime.fold[Duration](Unstated)(Computed(_))

          // Current duration
          val currentDuration = schedulingBlock.fold[Duration](Unstated)(_.duration)

          // New Scheduling block with updated start time, and updated duration (if not explicit).
          val sb = SchedulingBlock(start, currentDuration.fold(remainingDuration)(Explicit(_))(_ => remainingDuration))

          updateSchedulingBlock(sb)
        }
      }

      horizontalTextPosition = Alignment.Left
      horizontalAlignment    = Alignment.Left
      iconTextGap            = 10
      icon                   = getIcon("eclipse/menu-trimmed.gif")
      margin                 = new Insets(-1, -10, -1, -5)

      def rebuild(): Unit = Swing.onEDT {
        contents.clear()

        // menu items that don't depend on the context
        val fixedItems = RelativeTime("Now", 0) :: incrementsInMinutes.map(m => RelativeTime(s"Now + $m min", m * 60000))

        // menu items that require an observation and instrument to compute
        val instItems = for {
          e    <- editor if isPaUi // we don't want these for scheduling block ui
          obs  <- Option(e.getContextObservation)
          inst <- Option(e.getContextInstrumentDataObject)
        } yield {
          val setupTimeMs = inst.getSetupTime(obs).toMillis
          val reacqTimeMs = inst.getReacquisitionTime(obs).toMillis
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
      tooltip     =
        if (isPaUi) "Select a duration for the average parallactic angle calculation from the current time."
        else        "Time of slew for non-sidereal targets."
    }
    layout(relativeTimeMenuBar) = new Constraints() {
      anchor = Anchor.West
    }

    object dateTimeButton extends Button {
      icon    = getIcon("dates.gif")
      tooltip =
        if (isPaUi) "Select the time and duration for the average parallactic angle calculation."
        else        "Time of slew for non-sidereal targets."
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
      foreground          = Color.black
      horizontalAlignment = Alignment.Left
      icon                = getIcon("eclipse/blank.gif")
      iconTextGap         = iconTextGap - 2

      /**
        * This should be called whenever the position angle or parallactic angle changes.
        * A warning icon and tooltip are displayed if the two are different.
        */
      def warningState(): Unit =
        for {
          e    <- editor
          fmt  <- formatter
          inst <- Option(e.getContextInstrumentDataObject).collect { case s: SPInstObsComp => s }
        } warningStateFromPAString(inst.getPosAngleDegrees.toString)

      /**
        * Compares the parallactic angle to the supplied position angle.
        * We need to use the fmt on the paStr's supposed double to avoid floating point issues, e.g.
        * fa = 123.00, paStr = "123.0000000001".
        */
      def warningStateFromPAString(paStr: String): Unit = Swing.onEDT {
        for {
          e   <- editor
          fmt <- formatter
          fpaStr <- \/.fromTryCatchNonFatal(fmt.format(paStr.toDouble))
        } {
          val warningFlag = parallacticAngle.exists { angle =>
            val fa = fmt.format(angle.toDegrees)
            val faFlip = fmt.format(angle.flip.toDegrees)
            !fa.equals(fpaStr) && !faFlip.equals(fpaStr)
          }

          if (warningFlag) {
            icon = getIcon("eclipse/alert.gif")
            tooltip = "The PA is not at the average parallactic value."
          } else {
            icon = getIcon("eclipse/blank.gif")
            tooltip = ""
          }
        }
      }

      // This is kind of horrible, but we want to recalculate the warning state whenever the text of the label
      // changes, so this is the most robust way to ensure that this will happen no matter where the label changes.
      peer.addPropertyChangeListener("text", new PropertyChangeListener {
        override def propertyChange(evt: PropertyChangeEvent): Unit = {
          warningState()
        }
      })
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
    * The `Runnable` is a callback that will be invoked on the EDT after the target is updated.
    */
  def init(e: OtItemEditor[_, _], s: Option[Site], f: Format, c: Runnable): Unit = Swing.onEDT {
    editor    = Some(e)
    site      = s
    formatter = Some(f)
    callback  = c
    ui.relativeTimeMenu.rebuild()
    resetComponents()
  }

  def init(e: OtItemEditor[_, _], s: Site, f: Format): Unit =
    init(e, Some(s), f, Nop)

  def init(e: OtItemEditor[_, _], s: JOption[Site], f: Format): Unit =
    init(e, s.asScalaOpt, f, Nop)

  def init(e: OtItemEditor[_, _], s: JOption[Site], f: Format, callback: Runnable): Unit =
    init(e, s.asScalaOpt, f, callback)

  /** Current scheduling block, if any. */
  private def schedulingBlock: Option[SchedulingBlock] =
    for {
      e   <- editor
      obs <- Option(e.getContextObservation)
      sb  <- obs.getDataObject.asInstanceOf[SPObservation].getSchedulingBlock.asScalaOpt
    } yield sb

  /** Remaining time for context observation, if any. */
  private def remainingTime: Option[Long] =
    for {
      e   <- editor
      obs <- Option(e.getContextObservation)
    } yield ObsTargetCalculatorService.calculateRemainingTime(obs)

  /** Replace the scheduling block. */
  private def updateSchedulingBlock(sb: SchedulingBlock): Unit =
    for {
      e      <- editor
      ispObs <- Option(e.getContextObservation)
    } SchedulingBlockUpdate.runWithCallback(
        e.getWindow,
        sb,
        List(ispObs),
        new Runnable() {
          override def run(): Unit = {
            callback.run()
            resetComponents()
          }
        }
      )

  /**
    * Triggered when the date time button is clicked. Shows the ParallacticAngleDialog to allow the user to
    * explicitly set a date and duration for the parallactic angle calculation.
    */
  private def displayParallacticAngleDialog(): Unit = Swing.onEDT {
    for {
      e <- editor
      o <- editor.map(_.getContextObservation)
    } {
      val (title, instructions, mode) =
        if (isPaUi) (
         "Parallactic Angle Calculation",
          Some("Select the time and duration for the average parallactic angle calculation."),
          SchedulingBlockDialog.DateTimeAndDuration
        ) else (
          "Observation Scheduling",
          None,
          SchedulingBlockDialog.DateTimeOnly
        )

      val sb = SchedulingBlockDialog.prompt(
        title,
        instructions,
        e.getViewer.getParentFrame,
        o,
        site,
        mode)

      sb.foreach(updateSchedulingBlock)
    }
  }

  /**
    * This should be called whenever the position angle changes to compare it to the parallactic angle.
    */
  def positionAngleChanged(positionAngleText: String): Unit =
    ui.parallacticAngleFeedback.warningStateFromPAString(positionAngleText)


  /**
    * This should be called whenever the parallactic angle components need to be reinitialized, and at initialization.
    */
  def resetComponents(): Unit = Swing.onEDT {
    ui.parallacticAngleFeedback.text = ""
    for {
      sb  <- editor.flatMap(_.getContextObservation.getDataObject.asInstanceOf[SPObservation].getSchedulingBlock.asScalaOpt)
      fmt <- formatter
    } {
      // Scheduling block date and time
      val dateTimeStr = DateTimeFormatter
        .ofPattern("yyyy-MMM-dd HH:mm:ss z")
        .withZone(ZoneId.of(TimeZonePreference.get.getID))
        .format(Instant.ofEpochMilli(sb.start))

      // Scheduling block duration, in minutes
      val durStr = sb.duration.toOption.map(_ / 60000.0).foldMap(n => f", $n%2.1f min")

      // Parallactic angle. If it can be calculated, display its value and its 180 flip value.
      val paStr = parallacticAngle
        .map(a => List(a.toDegrees, a.flip.toDegrees).sorted.map(a0 => s"${fmt.format(a0)}°").mkString(" / "))
        .fold(", not visible")(a => s", $a")

      ui.parallacticAngleFeedback.text =
        if (isPaUi) dateTimeStr + durStr + paStr
        else dateTimeStr

      if (didParallacticAngleChange)
        publish(ParallacticAngleControls.ParallacticAngleChangedEvent)
    }
  }

  /**
    * Check if the parallactic angle changed from what is currently recorded for the instrument.
    * The angle is considered to have remained constant if it is within 0.005
    */
  def didParallacticAngleChange: Boolean =
    editor.exists { e =>
      parallacticAngle.forall { newAngle =>
        val angleDiff = {
          val oldAngleDegrees = e.getContextInstrumentDataObject.getPosAngleDegrees
          Math.abs(oldAngleDegrees - newAngle.toDegrees)
        }
        angleDiff >= Precision && Math.abs(angleDiff - 180) >= Precision
      }
    }

  /**
    * The parallactic angle calculation, if it can be calculated
    */
  def parallacticAngle: Option[Angle] =
    for {
      e <- editor
      o =  e.getContextObservation
      a <- e.getDataObject match {
        case p: ParallacticAngleSupport => p.calculateParallacticAngle(o).asScalaOpt
        case _                          => None
      }
    } yield a

  override def enabled_=(b: Boolean): Unit = {
    super.enabled_=(b)
    ui.relativeTimeMenu.enabled = b
    ui.dateTimeButton.enabled = b
  }

}

object ParallacticAngleControls {
  val Log: Logger = Logger.getLogger(getClass.getName)
  case object ParallacticAngleChangedEvent extends Event

  // Precision limit for which two parallactic angles are considered equivalent.
  val Precision = 0.005
}
