package jsky.app.ot.gemini.parallacticangle

import java.awt.{Color, Insets}
import java.text.{Format, SimpleDateFormat}
import java.util.Date
import java.util.logging.Logger
import javax.swing.BorderFactory
import javax.swing.border.EtchedBorder

import edu.gemini.pot.sp.ISPNode
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.inst.ParallacticAngleSupport
import edu.gemini.spModel.obs.{ObsTargetCalculatorService, SPObservation, SchedulingBlock}
import edu.gemini.spModel.obs.SchedulingBlock.Duration
import edu.gemini.spModel.obs.SchedulingBlock.Duration._
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.shared.util.immutable.{Option => JOption, ImOption}
import jsky.app.ot.editor.OtItemEditor
import jsky.app.ot.gemini.editor.EphemerisUpdater
import jsky.app.ot.util.TimeZonePreference
import jsky.util.gui.DialogUtil

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing._
import scala.swing.event.{ButtonClicked, Event}

import scalaz._, Scalaz._, scalaz.effect.IO

/**
 * This class encompasses all of the logic required to manage the average parallactic angle information associated
 * with an instrument configuration.
 *
 * @param isPaUi should be true for PA controls, false for Scheduling Block controls
 */
class ParallacticAngleControls(isPaUi: Boolean) extends GridBagPanel with Publisher {
  import ParallacticAngleControls._

  val Nop = new Runnable {
    override def run() = ()
  }

  private var editor:    Option[OtItemEditor[_, _]] = None
  private var site:      Option[Site]   = None
  private var formatter: Option[Format] = None
  private var callback:  Runnable = Nop

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
      icon                   = Resources.getIcon("eclipse/menu-trimmed.gif")
      margin                 = new Insets(-1, -10, -1, -5)

      def rebuild(): Unit = {
        contents.clear()

        // menu items that don't depend on the context
        val fixedItems = RelativeTime("Now", 0) :: incrementsInMinutes.map(m => RelativeTime(s"Now + $m min", m * 60000))

        // menu items that require an observation and instrument to compute
        val instItems = for {
          e    <- editor if isPaUi // we don't want these for scheduling block ui
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
      tooltip     =
        if (isPaUi) "Select a duration for the average parallactic angle calculation from the current time."
        else        "Time of slew for non-sidereal targets."
    }
    layout(relativeTimeMenuBar) = new Constraints() {
      anchor = Anchor.West
    }

    object dateTimeButton extends Button {
      icon    = Resources.getIcon("dates.gif")
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
   * The `Runnable` is a callback that will be invoked on the EDT after the target is updated.
   */
  def init(e: OtItemEditor[_, _], s: Option[Site], f: Format, c: Runnable): Unit = {
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
    } {
      val spObs = ispObs.getDataObject.asInstanceOf[SPObservation]
      val sameNight = spObs.getSchedulingBlock.asScalaOpt.exists(_.sameObservingNightAs(sb))

      // This is the action that will update the scheduling block
      val updateSchedBlock: IO[Unit] =
        for {
          _ <- IO(spObs.setSchedulingBlock(ImOption.apply(sb)))
          _ <- IO(ispObs.setDataObject(spObs))
        } yield ()

      // Ok, this is an IO action that goes and fetches the ephemerides and returns ANOTHER action
      // that will actually update the model and clear out the glass pane UI.
      val fetch: IO[IO[Unit]] =
        if (sameNight) IO(ispObs.silentAndLocked(updateSchedBlock))
        else EphemerisUpdater.refreshEphemerides(ispObs, sb.start, e.getWindow).map { completion =>

          // This is the action that will update the model
          val up: IO[Unit] =
            for {
              _ <- updateSchedBlock
              _ <- completion
            } yield ()

          // Here we bracket the update to disable/enable events and grab/release the program lock
          ispObs.silentAndLocked(up)

        }

      // Our final program
      val action: IO[Unit] =
        for {
          up <- time(fetch)("fetch ephemerides and construct completion")
          _  <- time(up)("perform model update without events, holding lock")
          _  <- edt(callback.run())
          _  <- edt(resetComponents())
        } yield ()

      // ... with an exception handler
      val safe: IO[Unit] =
        action except { t => edt(DialogUtil.error(peer, t)) }

      // Run it on a short-lived worker
      new Thread(new Runnable() {
        override def run() = safe.unsafePerformIO
      }, s"Ephemeris Update Worker for ${ispObs.getObservationID}").start()

    }


  /**
   * Triggered when the date time button is clicked. Shows the ParallacticAngleDialog to allow the user to
   * explicitly set a date and duration for the parallactic angle calculation.
   */
  private def displayParallacticAngleDialog(): Unit =
    for {
      e <- editor
      o <- editor.map(_.getContextObservation)
    } {
      val dialog = new ParallacticAngleDialog(
        e.getViewer.getParentFrame,
        o,
        o.getDataObject.asInstanceOf[SPObservation].getSchedulingBlock.asScalaOpt,
        site.map(_.timezone),
        isPaUi)
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
    } {
      val explicitlySet = !fmt.format(ParallacticAngleControls.angleToDegrees(angle)).equals(positionAngleText) &&
                          !fmt.format(ParallacticAngleControls.angleToDegrees(angle + Angle.fromDegrees(180))).equals(positionAngleText)
      ui.parallacticAngleFeedback.warningState(explicitlySet)
    }
  }


  /**
   * This should be called whenever the parallactic angle components need to be reinitialized, and at initialization.
   */
  def resetComponents(): Unit = {
    ui.parallacticAngleFeedback.text = ""
    for {
      sb  <- editor.flatMap(_.getContextObservation.getDataObject.asInstanceOf[SPObservation].getSchedulingBlock.asScalaOpt)
      fmt <- formatter
    } {
      // Scheduling block date and time
      val dateTimeStr = {
        val df = new SimpleDateFormat("MM/dd/yy HH:mm:ss z")
        df.setTimeZone(TimeZonePreference.get)
        df.format(new Date(sb.start))
      }

      // Scheduling block duration, in minutes
      val durStr = sb.duration.toOption.map(_ / 60000.0).foldMap(n => f", $n%2.1f min")

      // Parallactic Angle
      val paStr = parallacticAngle
        .map(ParallacticAngleControls.angleToDegrees)
        .fold(", not visible")(a => s", ${fmt.format(a)}°")

      ui.parallacticAngleFeedback.text =
        if (isPaUi) dateTimeStr + durStr + paStr
        else dateTimeStr

      if (didParllacticAngleChange) {
        publish(ParallacticAngleControls.ParallacticAngleChangedEvent)
      }
    }
  }

  /**
    * Check if the parallactic angle changed from what is currently recorded for the instrument.
    * The angle is considered to have remained constant if it is within 0.005
    */
  def didParllacticAngleChange: Boolean =
    editor.exists { e =>
      parallacticAngle.forall { newAngle =>
        val angleDiff = {
          val newAngleDegrees = angleToDegrees(newAngle)
          val oldAngleDegrees = e.getContextInstrumentDataObject.getPosAngleDegrees
          Math.abs(oldAngleDegrees - newAngleDegrees)
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
  val Log = Logger.getLogger(getClass.getName)
  case object ParallacticAngleChangedEvent extends Event

  // Precision limit for which two parallactic angles are considered equivalent.
  val Precision = 0.005

  def angleToDegrees(a: Angle): Double = a.toDegrees

  /** Wrap an IO action with a logging timer. */
  def time[A](io: IO[A])(msg: String): IO[A] =
    for {
      start <- IO(System.currentTimeMillis())
      a     <- io
      end   <- IO(System.currentTimeMillis())
      _     <- IO(ParallacticAngleControls.Log.info(s"$msg: ${end - start}ms"))
    } yield a

  /** Construct an IO action that runs on the EDT. */
  def edt[A](a: => Unit): IO[Unit] =
    IO(Swing.onEDT(a))

  /** Some useful operations for ISPNodes. */
  implicit class ParallacticAngleControlsISPNodeOps(n: ISPNode) {

    def silent[A](a: IO[A]): IO[A] =
      IO(n.setSendingEvents(false)) *> a ensuring IO(n.setSendingEvents(true))

    def locked[A](a: IO[A]): IO[A] =
      IO(n.getProgramWriteLock()) *> a ensuring IO(n.returnProgramWriteLock())

    def silentAndLocked[A](a: IO[A]): IO[A] =
      silent(locked(a))

  }

}