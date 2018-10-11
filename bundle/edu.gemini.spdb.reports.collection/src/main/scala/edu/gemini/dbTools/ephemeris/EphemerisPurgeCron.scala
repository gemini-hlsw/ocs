package edu.gemini.dbTools.ephemeris

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spdb.cron.Storage.{Perm, Temp}
import org.osgi.framework.BundleContext

import java.security.Principal
import java.util.logging.{Level, Logger}

/** A cron job that will purge ephemeris data in executed observations. */
object EphemerisPurgeCron {
  val Log = Logger.getLogger(EphemerisPurgeCron.getClass.getName)


  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator. */
  def run(ctx: BundleContext)(temp: Temp, perm: Perm, logger: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {
    val odbRef = ctx.getServiceReference(classOf[IDBDatabaseService])
    val odb    = ctx.getService(odbRef)

    Log.log(Level.INFO, "Start EphemerisPurgeCron")
    EphemerisPurgeFunctor.query(odb, user)
    Log.log(Level.INFO, "End EphemerisPurgeCron")
  }

}
