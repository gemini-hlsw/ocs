package edu.gemini.dbTools.timingwindowcheck

import org.osgi.framework.BundleContext
import java.io.File
import java.security.Principal
import java.time.Instant
import java.util.logging.{Level, Logger}

import scalaz._

object TimingWindowCheckCron {

  case class ActionLogger(logger: Logger) {
    def log(level: Level, msg: String): Action[Unit] =
      Action.delay(logger.log(level, msg))
  }

  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator. */
  def run(ctx: BundleContext)(tmpDir: File, logger: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {

    val action = for {
      env  <- TimingWindowCheckEnv.fromBundleContext(ctx)
      now  <- Action.delay(Instant.now)
      pids <- TimingWindowFunctor.query(env.odb, user)
      // _ <- sendEmails...
    } yield ()

    action.run.unsafePerformIO() match {
      case -\/(ex) => logger.log(Level.WARNING, "Error executing TimingWindowCheckCron", ex)
      case \/-(_)  => logger.info("TimingWindowCheckCron complete")
    }
  }

}
