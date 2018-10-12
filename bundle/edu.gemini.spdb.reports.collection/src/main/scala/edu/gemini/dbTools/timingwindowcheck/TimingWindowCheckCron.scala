package edu.gemini.dbTools.timingwindowcheck

import edu.gemini.spdb.cron.CronStorage

import edu.gemini.dbTools.mailer.ProgramAddresses
import edu.gemini.pot.sp.SPObservationID
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.skycalc.Interval

import org.osgi.framework.BundleContext
import java.io.File
import java.security.Principal
import java.time.{Duration, Instant}
import java.util.logging.{Level, Logger}

import scalaz._
import Scalaz._
import scalaz.effect.IO

/**
 * The TimingWindowCheckCron finds all active observations that have a timing
 * window which expired since the last time the cron job ran and sends a
 * notification email to the PI, NGO, CS and appropriate QA exploder.
 * Multiple observations from the same program are grouped into a single email.
 */
object TimingWindowCheckCron {

  private implicit class LoggerOps(logger: Logger) {
    def ioLog(level: Level, msg: String): IO[Unit] =
      IO(logger.log(level, msg))
  }

  private def sendEmails(
    log:  Logger,
    odb:  IDBDatabaseService,
    twcm: TimingWindowCheckMailer,
    all:  Vector[SPObservationID]
  ): IO[Unit] =

    all.groupBy(_.getProgramID).toList.traverseU {
      case (pid, ov) =>
        ProgramAddresses.fromProgramId(odb, pid).flatMap {
          case None =>
            log.ioLog(Level.INFO, s"Could not get email addresses for $pid because it was not found in ODB")

          case Some(Failure(msg)) =>
            log.ioLog(Level.INFO, s"Could not send timing window nag email because some addresses are not valid for $pid: $msg")

          case Some(Success(pa)) =>
            ov.toNel.fold(IO.ioUnit)(on => twcm.notifyExpiredWindows(pid, pa, on))
        }
    }.void

  // Last time the cron job ran (or `now` if it hasn't run before).  Using the
  // current time if not executed before prevents sending a flood of emails the
  // first time that we execute the cron job.
  private def lastRun(dir: File, now: Instant): IO[Instant] =
    TimingWindowProps.load(dir).map(_.fold(now)(_.when))

  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator. */
  def run(ctx: BundleContext)(store: CronStorage, logger: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {

    val action: IO[Unit] =
      for {
        env <- TimingWindowCheckEnv.fromBundleContext(ctx)
        now <- IO(Instant.now)
        lst <- lastRun(store.permDir, now)
        obs <- TimingWindowFunctor.query(new Interval(lst, now), env.odb, user)
        _   <- logger.ioLog(Level.INFO, obs.mkString("Found expired timing windows in these active obs: {", ",", "}"))
        _   <- sendEmails(logger, env.odb, env.mailer, obs)
        _   <- TimingWindowProps(now).store(store.permDir)
      } yield ()

    action.attempt.unsafePerformIO() match {
      case -\/(ex) => logger.log(Level.WARNING, "Error executing TimingWindowCheckCron", ex)
      case \/-(_)  => logger.info("TimingWindowCheckCron complete")
    }
  }

}
