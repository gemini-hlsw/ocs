package edu.gemini.dbTools.timingwindowcheck

import org.osgi.framework.BundleContext
import java.io.File
import java.security.Principal
import java.time.{Duration, Instant}
import java.util.logging.{Level, Logger}

import edu.gemini.dbTools.mailer.ProgramAddresses
import edu.gemini.pot.sp.SPObservationID
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.skycalc.Interval

import scalaz._
import Scalaz._

object TimingWindowCheckCron {

  case class ActionLogger(logger: Logger) {
    def log(level: Level, msg: String): Action[Unit] =
      Action.delay(logger.log(level, msg))
  }

  private def sendEmails(
    log:  ActionLogger,
    odb:  IDBDatabaseService,
    twcm: TimingWindowCheckMailer,
    all:  Vector[SPObservationID]
  ): Action[Unit] =

    all.groupBy(_.getProgramID).toList.traverseU {
      case (pid, ov) =>
        EitherT(ProgramAddresses.fromProgramId(odb, pid).catchLeft).flatMap {
          case None =>
            log.log(Level.INFO, s"Could not get email addresses for $pid because it was not found in ODB")

          case Some(Failure(msg)) =>
            log.log(Level.INFO, s"Could not send timing window nag email because some addresses are not valid for $pid: $msg")

          case Some(Success(pa)) =>
            ov.toNel.fold(Action.unit)(on => twcm.notifyPendingCheck(pid, pa, on))
        }

      case _ =>
        Action.unit

    }.void

  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator. */
  def run(ctx: BundleContext)(tmpDir: File, logger: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {

    def checkInterval(now: Instant): Interval =
      new Interval(now.minus(Duration.ofHours(24)), now)

    val action = for {
      env <- TimingWindowCheckEnv.fromBundleContext(ctx)
      now <- Action.delay(Instant.now)
      obs <- TimingWindowFunctor.query(checkInterval(now), env.odb, user)
      _   <- sendEmails(ActionLogger(logger), env.odb, env.mailer, obs)
    } yield ()

    action.run.unsafePerformIO() match {
      case -\/(ex) => logger.log(Level.WARNING, "Error executing TimingWindowCheckCron", ex)
      case \/-(_)  => logger.info("TimingWindowCheckCron complete")
    }
  }

}
