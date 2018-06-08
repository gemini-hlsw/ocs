package edu.gemini.dbTools.maskcheck

import edu.gemini.auxfile.api.AuxFile
import edu.gemini.auxfile.server.AuxFileServer
import edu.gemini.dbTools.mailer.ProgramAddresses
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.SPProgramID
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
    l: MCLogger,
    o: IDBDatabaseService,
    m: MaskCheckMailer,
    ps: List[(SPProgramID, List[AuxFile])]
  ): MC[Unit] =

    ps.traverseU { case (pid, pending) =>
      MC.catchLeft(ProgramAddresses.fromProgramId(o, pid)).flatMap {
        case None               =>
          l.log(Level.INFO, s"Could not get email addresses for $pid because it was not found in ODB")
        case Some(Failure(msg)) =>
          l.log(Level.INFO, s"Could not send mask check nag email because some addresses are not valid for $pid: $msg")
        case Some(Success(pa))  =>
          m.notifyPendingCheck(pid, pa, pending)
      }
    }.void

  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator. */
  def run(ctx: BundleContext)(tmpDir: File, logger: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {

    val action = for {
      env  <- MaskCheckEnv.fromBundleContext(ctx)
      now  <- MC.delay(Instant.now)
      pids <- ActiveScienceProgramFunctor.query(env.odb, user)
      ps   <- allPending(env.auxFileServer, pids, now, env.nagDelay)
      _    <- sendEmails(MCLogger(logger), env.odb, env.mailer, ps)
    } yield ()

    action.run.unsafePerformIO() match {
      case -\/(ex) => logger.log(Level.WARNING, "Error executing MaskCheckCron", ex)
      case \/-(_)  => logger.info("MaskCheckCron complete")
    }
  }

}
