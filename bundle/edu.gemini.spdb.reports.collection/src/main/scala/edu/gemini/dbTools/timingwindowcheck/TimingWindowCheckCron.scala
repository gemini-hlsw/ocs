package edu.gemini.dbTools.timingwindowcheck

import edu.gemini.spdb.cron.Storage.{Temp, Perm}
import org.osgi.framework.BundleContext
import java.security.Principal
import java.time.{Duration, Instant}
import java.util.logging.{Level, Logger}

import edu.gemini.dbTools.mailer.ProgramAddresses
import edu.gemini.pot.sp.SPObservationID
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.skycalc.Interval

import scalaz._
import Scalaz._
import scalaz.effect.IO


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
            ov.toNel.fold(IO.ioUnit)(on => twcm.notifyPendingCheck(pid, pa, on))
        }
    }.void

  private def lastRun(perm: Perm, now: Instant): IO[Instant] =
    TimingWindowProps.load(perm).map(_.fold(now)(_.when))

  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator. */
  def run(ctx: BundleContext)(temp: Temp, perm: Perm, logger: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {

    def checkInterval(lst: Instant, now: Instant): Interval =
      new Interval(now.minus(Duration.ofHours(24)), now)

    val action: IO[Unit] =
      for {
        env <- TimingWindowCheckEnv.fromBundleContext(ctx)
        now <- IO(Instant.now)
        lst <- lastRun(perm, now)
        obs <- TimingWindowFunctor.query(new Interval(lst, now), env.odb, user)
        _   <- sendEmails(logger, env.odb, env.mailer, obs)
        _   <- TimingWindowProps(now).store(perm)
      } yield ()

    action.attempt.unsafePerformIO() match {
      case -\/(ex) => logger.log(Level.WARNING, "Error executing TimingWindowCheckCron", ex)
      case \/-(_)  => logger.info("TimingWindowCheckCron complete")
    }
  }

}
