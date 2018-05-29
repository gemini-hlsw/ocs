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

  def action(
    c: java.awt.Component,
    sb: SchedulingBlock,
    os: List[ISPObservation]
  ): IO[Unit] =
    actionWithCallback(c, sb, os, NoOp)

  def actionWithCallback(
    c: java.awt.Component,
    sb: SchedulingBlock,
    os: List[ISPObservation],
    callback: Runnable
  ): IO[Unit] = {

    def sameNight(o: ISPObservation): Boolean =
      o.getDataObject
       .asInstanceOf[SPObservation]
       .getSchedulingBlock
       .asScalaOpt
       .exists(_.sameObservingNightAs(sb))

    val (sames, diffs) = os.partition(sameNight)

    def updateBlockAction(o: ISPObservation): IO[Unit] = {
      val spObs = o.getDataObject.asInstanceOf[SPObservation]
      for {
        _ <- IO(spObs.setSchedulingBlock(ImOption.apply(sb)))
        _ <- IO(o.setDataObject(spObs))
      } yield ()
    }

    def updateSameNight(os: List[ISPObservation]): IO[Unit] =
      os.traverseU { o => o.silentAndLocked(updateBlockAction(o)) }.void

    def updateDiffNight(os: List[ISPObservation]): IO[Unit] = {
      def up(o: ISPObservation, m: Map[(HorizonsDesignation, Site), Ephemeris]): IO[Unit] =
        EphemerisUpdater.setEphemerides(o, m) *> updateBlockAction(o)

      for {
        m <- time(EphemerisUpdater.bulkLookup(os, sb.start, c))("fetch ephemerides")
        _ <- time(os.traverseU { o => o.silentAndLocked(up(o, m)) }.void)("perform model update without events, holding lock")
      } yield ()
    }

    updateSameNight(sames) *>
    updateDiffNight(diffs) *>
    edt(callback.run())
  }

  def run(
    c: java.awt.Component,
    sb: SchedulingBlock,
    os: List[ISPObservation]
  ): Unit =
    runWithCallback(c, sb, os, NoOp)

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
