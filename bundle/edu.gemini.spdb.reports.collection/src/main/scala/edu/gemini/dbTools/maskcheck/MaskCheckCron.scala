package edu.gemini.dbTools.maskcheck

import edu.gemini.auxfile.api.AuxFile
import edu.gemini.auxfile.server.AuxFileServer
import edu.gemini.dbTools.mailer.ProgramAddresses
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.shared.util.immutable.ScalaConverters._
import org.osgi.framework.BundleContext

import java.io.File
import java.security.Principal
import java.time.{ Duration, Instant }
import java.util.logging.{Level, Logger}

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._
import scalaz.effect.IO

/**
 * MaskCheckCron sends a nagging email for all active programs which have
 * unchecked mask files that have been ready for a configurable amount of time
 * (one week in production at the time of writing).
 *
 * At start up it will look for the AuxFileService and the IDBDatabaseService
 * as well as a handful of bundle properties:
 *
 * 1) cron.odbMail.SITE_SMTP_SERVER
 *    The shared email smtp server host.
 *
 * 2) cron.odbMail.mailer.type
 *    "development", "production", or "test".  The "development" version just
 *    prints the email that would have been sent.  The "test" version sends a
 *    modified email that indicates it is a test to @gemini.edu emails only.
 *    Finally "production" sends the email to NGO + contact scientists.
 *
 * 3) edu.gemini.dbTools.maskcheck.nagdelay
 *    How much time may pass before a nag email is sent.  The value must be
 *    parseable by java.time.Duration.parse.  For example: "P7D" for one week.
 */
object MaskCheckCron {

  private def pending(
    afs: AuxFileServer,
    pid: SPProgramID,
    now: Instant,
    nag: Duration
  ): MC[List[AuxFile]] =

    MC.catchLeft(afs.listAll(pid)).map(_.asScala.toList.filter { f =>
      val lastMod   = Instant.ofEpochMilli(f.getLastModified)
      val nagAt     = lastMod.plus(nag)
      val lastEmail = f.getLastEmailed.asScalaOpt.getOrElse(Instant.MIN)

      f.getName.endsWith(".odf") &&   // Only ODF Files
      !f.isChecked               &&   // that haven't been checked
      lastMod.isAfter(lastEmail) &&   // that have been modified more recently than the last nagging email (if any)
      now.isAfter(nagAt)              // that haven't been checked in at least a week
    })

  private def allPending(
    afs:  AuxFileServer,
    pids: List[SPProgramID],
    now:  Instant,
    nag:  Duration
  ): MC[List[(SPProgramID, List[AuxFile])]] =

    pids.traverseU { pid => pending(afs, pid, now, nag).strengthL(pid) }

  case class MCLogger(logger: Logger) {
    def log(level: Level, msg: String): MC[Unit] =
      MC.delay(logger.log(level, msg))
  }

  private def sendEmails(
    log: MCLogger,
    afs: AuxFileServer,
    now: Instant,
    odb: IDBDatabaseService,
    mcm: MaskCheckMailer,
    ps:  List[(SPProgramID, List[AuxFile])]
  ): MC[Unit] =

    ps.traverseU {
      case (_, Nil)       =>
        MC.mcUnit

      case (pid, pending) =>
        EitherT(ProgramAddresses.fromProgramId(odb, pid).catchLeft).flatMap {
          case None               =>
            log.log(Level.INFO, s"Could not get email addresses for $pid because it was not found in ODB")

          case Some(Failure(msg)) =>
            log.log(Level.INFO, s"Could not send mask check nag email because some addresses are not valid for $pid: $msg")

          case Some(Success(pa))  =>
            mcm.notifyPendingCheck(pid, pa, pending) *>
              MC.catchLeft(afs.setLastEmailed(pid, pending.map(_.getName).asJava, ImOption.apply(now)))
        }
    }.void

  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator. */
  def run(ctx: BundleContext)(tmpDir: File, logger: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {

    val action = for {
      env  <- MaskCheckEnv.fromBundleContext(ctx)
      now  <- MC.delay(Instant.now)
      pids <- ActiveScienceProgramFunctor.query(env.odb, user)
      ps   <- allPending(env.auxFileServer, pids, now, env.nagDelay)
      _    <- sendEmails(MCLogger(logger), env.auxFileServer, now, env.odb, env.mailer, ps)
    } yield ()

    action.run.unsafePerformIO() match {
      case -\/(ex) => logger.log(Level.WARNING, "Error executing MaskCheckCron", ex)
      case \/-(_)  => logger.info("MaskCheckCron complete")
    }
  }

}
