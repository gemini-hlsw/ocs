package edu.gemini.dbTools.ephemeris

import edu.gemini.dbTools.ephemeris.ExportError.OdbError
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.skycalc.TwilightBoundType.NAUTICAL
import edu.gemini.skycalc.TwilightBoundedNight
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.osgi.SiteProperty
import org.osgi.framework.BundleContext

import java.io.File
import java.nio.file.{Files, Path}
import java.security.Principal
import java.time.Instant
import java.util.logging.{Level, Logger}

import scalaz.Scalaz._
import scalaz._

/** TCS ephemeris export cron job. */
object TcsEphemerisCron {

  /** If specified, this property identifies the directory where ephemeris
    * files will be written.
    */
  val DirectoryProp = "edu.gemini.dbTools.tcs.ephemeris.directory"

  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator. */
  def run(ctx: BundleContext)(tmpDir: File, log: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {
    val site = Option(SiteProperty.get(ctx)) | sys.error(s"Property `${SiteProperty.NAME}` not specified.")

    val exportDir = Option(ctx.getProperty(DirectoryProp)).fold(tmpDir) { pathString =>
      new File(pathString)
    }

    val odbRef = ctx.getServiceReference(classOf[IDBDatabaseService])
    val odb    = ctx.getService(odbRef)
    val night  = TwilightBoundedNight.forTime(NAUTICAL, Instant.now.toEpochMilli, site)

    // Extract horizons ids for all non-sidereal observations in the database.
    val nonSid: TryExport[ISet[HorizonsDesignation]] =
      TryExport.fromTryCatch(OdbError) {
        ISet.fromList(NonSiderealObservationFunctor.query(odb, user).map(_.hid))
      }

    // Updates the TCS ephemeris file directory to contain only ephemeris files
    // corresponding to non-sidereal observations of interest in the ODB.  Each
    // file is updated for the current observing night.
    val action: TryExport[HorizonsDesignation ==>> (ExportError \/ Path)] =
      for {
        obs <- nonSid
        res <- TcsEphemerisExport(exportDir.toPath, night, site).update(obs)
      } yield res

    log.info(s"Starting ephemeris lookup for $site, writing into $exportDir")
    try {
      action.run.unsafePerformIO() match {
        case -\/(err)     =>
          err.log(log, "Could not refresh ephemeris data: ")

        case \/-(updates) =>
          updates.toList.foreach { case (hid, res) =>
            res match {
              case -\/(err)  => err.log(log)
              case \/-(path) => log.log(Level.INFO, s"$hid: updated at ${Files.getLastModifiedTime(path)}")
            }
          }
      }
    } finally {
      ctx.ungetService(odbRef)
    }
    log.info("Finish ephemeris lookup.")
  }
}


