package edu.gemini.dbTools.ephemeris

import edu.gemini.dbTools.ephemeris.ExportError.OdbError
import edu.gemini.dbTools.ephemeris.FileStatus._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.skycalc.TwilightBoundType.NAUTICAL
import edu.gemini.skycalc.TwilightBoundedNight
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.osgi.SiteProperty
import org.osgi.framework.BundleContext

import java.io.File
import java.nio.file.Path
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

  import TcsEphemerisExport.FileUpdate  // (FileStatus, ExportError \/ Path)

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
    val action: TryExport[HorizonsDesignation ==>> FileUpdate] =
      for {
        hid <- nonSid
        res <- TcsEphemerisExport(exportDir.toPath, night, site).update(hid)
      } yield res

    log.info(s"Starting ephemeris lookup for $site, writing into $exportDir")
    try {
      action.run.unsafePerformIO() match {
        case -\/(err)     =>
          log.log(Level.WARNING, "Could not refresh ephemeris data: " + err.message, err.exception.orNull)

        case \/-(updates) =>
          val (errors, successes) = splitResults(updates)
          if (!successes.isEmpty) log.log(Level.INFO, successReport(successes)) // hmm, no nonEmpty?
          if (!errors.isEmpty) log.log(Level.WARNING, errorReport(errors))
      }
    } finally {
      ctx.ungetService(odbRef)
    }
    log.info("Finish ephemeris lookup.")
  }

  // **** Report Formatting ****

  private implicit class ThrowableOps(t: Throwable) {
    // Produces a String describing the stack trace but ignoring the error
    // message itself.  The idea is that stack traces that are identical except
    // for details mentioned in the error message will all map to the same
    // String.
    def stackString: String = {
      val buf = new StringBuilder(s"${t.getClass.getName}\n")
      t.getStackTrace.foreach { e =>
        buf ++= s"\tat ${e.getClassName}.${e.getMethodName}(${e.getFileName}:${e.getLineNumber})\n"
      }

      Option(t.getCause).foreach { c =>
        buf ++= "Caused By:\n"
        buf ++= c.stackString
      }

      buf.toString()
    }
  }

  // Errors keyed by stack trace String in order to group similar stack traces.
  type ErrorMap   = ==>>[String,      NonEmptyList[(HorizonsDesignation, FileStatus, ExportError)]]

  // Success cases grouped by the status of the file before update, so we can
  // simply list the files that were deleted, created, updated, or skipped.
  type SuccessMap = ==>>[FileStatus,  NonEmptyList[(HorizonsDesignation, Path)                   ]]

  // Take the map of results from TcsEphemerisExport and process it into a
  // map of errors and a map of successes.
  private def splitResults(res: HorizonsDesignation ==>> FileUpdate): (ErrorMap, SuccessMap) =
    res.foldrWithKey((==>>.empty: ErrorMap, ==>>.empty: SuccessMap)) { case (hid, (fileStatus, errorOrPath), (errorMap, successMap)) =>
      errorOrPath match {
        case -\/(err)  =>
          val key       = err.exception.map(_.stackString) | ""
          val head      = (hid, fileStatus, err)
          val errorMap2 = errorMap.alter(key, o => Some(o.fold(NonEmptyList(head)) { nel =>
            NonEmptyList.nel(head, nel.toIList)
          }))
          (errorMap2, successMap)

        case \/-(path) =>
          val head        = (hid, path)
          val successMap2 = successMap.alter(fileStatus, o => Some(o.fold(NonEmptyList(head)) { nel =>
            NonEmptyList.nel(head, nel.toIList)
          }))
          (errorMap, successMap2)
      }
    }

  // Maps the list to Strings where each entry is as wide as the widest entry,
  // right padding with space as necessary.
  private def pad[A](as: List[A]): List[String] = {
    val s = as.map(_.toString)
    val m = s.maxBy(_.length).length
    val f = s"%-${m}s"
    s.map { s0 => f.format(s0) }
  }

  // The error report will list all the cases that generated the same stack
  // trace followed by "Caused By" and then the stack trace. Further, we try
  // to combine all the cases with the same user-oriented error message and
  // then just list the corresponding horizons ids below.
  private def errorReport(em: ErrorMap): String =
    em.toList.map { case (stack, cases) =>
      // Group the cases by ExportError message.
      val empty    = ==>>.empty[String, List[(HorizonsDesignation, FileStatus)]]
      val messages = cases.foldRight(empty) { case ((h, f, e), m) =>
        m.alter(e.message, lo => Some((h, f) :: (lo | Nil)))
      }

      // Show the message followed by a formatted list of horizons ids
      val header   = messages.toList.map { case (msg, lst) =>
        val (hs, fs) = lst.unzip
        pad(hs).zip(fs).map { case (h, s) => s"\t$h ($s)" }.mkString(s"$msg\n", "\n", "\n")
      }.mkString("\n")

      // After the header, add the stack trace if any.
      val causedBy = (stack == "") ? "" | s"Caused By:\n$stack"
      s"$header\n$causedBy"
    }.mkString(s"\n${"-" * 80}\n", "\n", "")

  // The success report will group the files by what happened, indicating
  // whether they were updated, deleted, created or skipped.
  private def successReport(sm: SuccessMap): String =
    sm.toList.map { case (fs, updates) =>
      val title = fs match {
        case Expired  => "* Updated"
        case Extra    => "* Deleted"
        case Missing  => "* Created"
        case UpToDate => "* Skipped (already up-to-date)"
      }
      val (hids, paths) = updates.toList.unzip
      val hidsS         = pad(hids)
      val lines         = hidsS.zip(paths).map { case (h,p) => s"\t$h => $p" }
      s"${lines.mkString(s"$title\n", "\n", "\n")}"
    }.mkString("\n", "\n", "")

}


