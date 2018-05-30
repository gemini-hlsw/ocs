package jsky.app.ot.gemini.schedulingBlock

import java.util.logging.Logger

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.{ Ephemeris, HorizonsDesignation, Site }
import edu.gemini.spModel.obs.{ SchedulingBlock, SPObservation }
import edu.gemini.spModel.syntax.sp.node._
import jsky.app.ot.gemini.editor.EphemerisUpdater
import jsky.util.gui.DialogUtil

import scala.swing.Swing
import scalaz._
import Scalaz._
import scalaz.effect.IO

/**
 * Utility to replace the scheduling block in one or more observations, updating
 * ephemerides as necessary.
 */
object SchedulingBlockUpdate {
  private val Log = Logger.getLogger(getClass.getName)

  private val NoOp = new Runnable {
    override def run() = ()
  }

  /**
   * Creates an IO action that updates the scheduling block in the given
   * observations, notifying the user of the lookup progress in a glass pane
   * over the window of the given `Component`.
   */
  def action(
    c: java.awt.Component,
    sb: SchedulingBlock,
    os: List[ISPObservation]
  ): IO[Unit] =
    actionWithCallback(c, sb, os, NoOp)

  /**
   * Creates an IO action that updates the scheduling block in the given
   * observations, notifying the user of the lookup progress in a glass pane
   * over the window of the given `Component`, calling the given `callback`
   * when done.
   */
  def actionWithCallback(
    c: java.awt.Component,
    sb: SchedulingBlock,
    os: List[ISPObservation],
    callback: Runnable
  ): IO[Unit] = {

    // Partition into observation's whose existing scheduling block (if any)
    // falls on the same night and those whose block falls on a different night
    val (sames, diffs) = os.partition {
      _.getDataObject
       .asInstanceOf[SPObservation]
       .getSchedulingBlock
       .asScalaOpt
       .exists(_.sameObservingNightAs(sb))
    }

    // Creates an action that will update the observation's scheduling block.
    def updateBlock(o: ISPObservation): IO[Unit] = {
      val spObs = o.getDataObject.asInstanceOf[SPObservation]
      for {
        _ <- IO(spObs.setSchedulingBlock(ImOption.apply(sb)))
        _ <- IO(o.setDataObject(spObs))
      } yield ()
    }

    // Creates an action that will perform the given update per observation
    // while holding a lock and without throwing events.
    def updateAll(os: List[ISPObservation])(up: ISPObservation => IO[Unit]): IO[Unit] =
      time(os.traverseU { o => o.silentAndLocked(up(o)) }.void)("perform model update without events, holding lock")

    // Fetches the ephemerides required by all the given observations and then
    // updates the scheduling block and ephemerides.
    def updateDiffNight(os: List[ISPObservation]): IO[Unit] =
      for {
        f <- time(EphemerisUpdater.refreshAll(os, sb.start, c))("fetch ephemerides")
        _ <- updateAll(os) { o => f(o) *> updateBlock(o) }
      } yield ()

    updateAll(sames)(updateBlock) *>
    updateDiffNight(diffs) *>
    edt(callback.run())
  }

  /**
   * Executes an IO action that updates the scheduling block in the given
   * observations, notifying the user of the lookup progress in a glass pane
   * over the window of the given `Component`.
   */
  def run(
    c: java.awt.Component,
    sb: SchedulingBlock,
    os: List[ISPObservation]
  ): Unit =
    runWithCallback(c, sb, os, NoOp)

  /**
   * Executes an IO action that updates the scheduling block in the given
   * observations, notifying the user of the lookup progress in a glass pane
   * over the window of the given `Component`, calling the given `callback`
   * when done.
   */
  def runWithCallback(
    c: java.awt.Component,
    sb: SchedulingBlock,
    os: List[ISPObservation],
    callback: Runnable
  ): Unit = {

    // ... with an exception handler
    val safe: IO[Unit] =
      actionWithCallback(c, sb, os, callback) except { t =>
        edt(DialogUtil.error(c, t))
      }

    // Run it on a short-lived worker
    new Thread(new Runnable() {
      override def run() = safe.unsafePerformIO
    }, s"Ephemeris Update Worker for ${os.map(_.getObservationID).mkString(", ")}").start()

  }

  /** Construct an IO action that runs on the EDT. */
  private def edt[A](a: => Unit): IO[Unit] =
    IO(Swing.onEDT(a))

  /** Wrap an IO action with a logging timer. */
  private def time[A](io: IO[A])(msg: String): IO[A] =
    for {
      start <- IO(System.currentTimeMillis())
      a     <- io
      end   <- IO(System.currentTimeMillis())
      _     <- IO(Log.info(s"$msg: ${end - start}ms"))
    } yield a

}
