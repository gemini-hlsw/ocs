package edu.gemini.dbTools.timingwindowcheck

import org.osgi.framework.BundleContext
import java.io.File
import java.security.Principal
import java.time.Instant
import java.util.logging.{Level, Logger}

import edu.gemini.skycalc.{Interval, TwilightBoundType, TwilightBoundedNight, Union}
import scalaz._
import Scalaz._
import edu.gemini.dbTools.mailer.ProgramAddresses
import edu.gemini.pot.sp.SPObservationID
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.{SPProgramID, Site}

object TimingWindowCheckCron {

  case class ActionLogger(logger: Logger) {
    def log(level: Level, msg: String): Action[Unit] =
      Action.delay(logger.log(level, msg))
  }

  private def sendEmails(
    log: ActionLogger,
    now: Instant,
    odb: IDBDatabaseService,
    twcm: TimingWindowCheckMailer,
    ps:  List[(SPProgramID, List[SPObservationID])]
  ): Action[Unit] =

    ps.traverseU {
      case (pid, l) =>
        l.toNel.fold(Action.unit) { os =>
          EitherT(ProgramAddresses.fromProgramId(odb, pid).catchLeft).flatMap {
            case None =>
              log.log(Level.INFO, s"Could not get email addresses for $pid because it was not found in ODB")

            case Some(Failure(msg)) =>
              log.log(Level.INFO, s"Could not send mask check nag email because some addresses are not valid for $pid: $msg")

            case Some(Success(pa)) =>
              twcm.notifyPendingCheck(pid, pa, os)
          }
        }
    }.void

  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator. */
  def run(ctx: BundleContext)(tmpDir: File, logger: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {

    def getCheckUnion(now: Instant, site: Site): Union[Interval] = {

      val night = TwilightBoundedNight.forInstant(TwilightBoundType.NAUTICAL, now, site)

      var u = new Union[Interval]
      u.add(new Interval(night.getStartInstant, night.next.getStartInstant))
      if (now.isAfter(night.getEndInstant)) u else { u.remove(new Interval(now, night.getEndInstant)); u }
    }

    val action = for {
      env <- TimingWindowCheckEnv.fromBundleContext(ctx)
      now <- Action.delay(Instant.now)
      union = getCheckUnion(now, env.site)
      all <- TimingWindowFunctor.query(env.odb, user)
      ps = all.flatMap {
        case (pid, otws) => {
          val ftws = otws.filter { case (oid, tw) => union.contains(tw) }
          if (ftws.nonEmpty) List((pid, ftws.map(_._1))) else Nil
        }
      }
      _  <- sendEmails(ActionLogger(logger), now, env.odb, env.mailer, ps)
    } yield ()

    action.run.unsafePerformIO() match {
      case -\/(ex) => logger.log(Level.WARNING, "Error executing TimingWindowCheckCron", ex)
      case \/-(_)  => logger.info("TimingWindowCheckCron complete")
    }
  }

}
