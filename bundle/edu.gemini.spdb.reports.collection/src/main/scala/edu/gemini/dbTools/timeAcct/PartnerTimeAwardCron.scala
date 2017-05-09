package edu.gemini.dbTools.timeAcct

import edu.gemini.pot.spdb.IDBDatabaseService
import org.osgi.framework.BundleContext

import java.io.File
import java.security.Principal
import java.util.logging.{Level, Logger}

/** A cron job needed during the transition to the new time accounting model.
  * It will look for pre-2017B programs that have new partner execution time
  * and replace their fake partner time award to match.  This cron job will not
  * be necessary once there are no active pre-2017B programs in the queue.
  */
object PartnerTimeAwardCron {

  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator. */
  def run(ctx: BundleContext)(tmpDir: File, logger: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {
    val odbRef = ctx.getServiceReference(classOf[IDBDatabaseService])
    val odb    = ctx.getService(odbRef)

    logger.log(Level.INFO, "Start PartnerTimeAwardCron")
    PartnerTimeAwardFunctor.query(odb, user)
    logger.log(Level.INFO, "End PartnerTimeAwardCrong")
  }
}
