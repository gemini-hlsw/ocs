package edu.gemini.dbTools.ephemeris

import edu.gemini.dbTools.ephemeris.ExportError.OdbError
import edu.gemini.dbTools.ephemeris.FileStatus._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.skycalc.TwilightBoundType
import edu.gemini.skycalc.TwilightBoundType.OFFICIAL
import edu.gemini.skycalc.{Night, TwilightBoundedNight}
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.osgi.SiteProperty
import org.osgi.framework.BundleContext

import java.io.File
import java.nio.file.Path
import java.security.Principal
import java.time.{Duration, Instant}
import java.time.temporal.ChronoUnit.MINUTES
import java.util.logging.{Level, Logger}
import javax.mail.internet.InternetAddress

import scala.collection.JavaConverters._
import scalaz.Scalaz._
import scalaz._
import scalaz.effect.IO

/** TCS ephemeris export cron job. */
object TcsEphemerisCron {

  /** If specified, this property identifies the directory where ephemeris
    * files will be written.
    */
  val DirectoryProp  = "edu.gemini.dbTools.tcs.ephemeris.directory"

  /** A config property that lists email recipients for error reports. */
  val RecipientsProp = "edu.gemini.dbTools.tcs.ephemeris.recipients"

  /** An optional config property that specifies the twilight bounds to use.
    * Valid values are: official, civil, nautical, or astronomical.  If not
    * specified the time range will end at the next sunrise local time and
    * start 24 hours before.
    */
  val NightProp      = "edu.gemini.dbTools.tcs.ephemeris.night"

  /** A config property that specifies the SMTP server to use. */
  val SmtpProp       = "cron.odbMail.SITE_SMTP_SERVER"

  import TcsEphemerisExport.FileUpdate  // (FileStatus, ExportError \/ Path)

  // Creates a mailer for sending error reports, extracting smtp and recipients
  // from bundle properties.
  private def reportMailer(ctx: BundleContext, site: Site, log: Logger): ReportMailer = {
    def parseRecipients(s: String): String \/ List[InternetAddress] =
      \/.fromTryCatchNonFatal {
        InternetAddress.parse(s, false).toList
      }.leftMap(_ => s"Could not parse address list '$s'").ensure("Empty address list.") { _.nonEmpty }

    val mailer = for {
      rprop <- Option(ctx.getProperty(RecipientsProp)) \/> s"Missing $RecipientsProp"
      rs    <- parseRecipients(rprop)
      host  <- Option(ctx.getProperty(SmtpProp))       \/> s"Missing $SmtpProp"
    } yield ReportMailer(site, host, rs)

    mailer match {
      case -\/(msg) =>
        // If anything is missing use a test mailer.
        log.warning(s"TcsEphemerisCron using test mailer ($msg).")
        ReportMailer.forTesting(site)
      case \/-(m)   =>
        log.info(s"TcsEphemerisCron using host '${m.mailer.smtpHost}' and recipients '${m.recipients.mkString(",")}'")
        m
    }
  }

  private implicit class InstantOps(i: Instant) {
    /** Returns this instant truncated to minutes (ie, with no seconds,
      * milliseconds, etc.)
      */
    def roundDown: Instant =
      i.truncatedTo(MINUTES)

    /** Returns this instant rounded up to the next whole number of minutes
      * with no seconds, milliseconds, etc.)
      */
    def roundUp: Instant = {
      val rnd = i.roundDown
      (rnd == i) ? rnd | rnd.plus(Duration.ofMinutes(1))
    }
  }

  /** A Night implementation for a specific start and end time. */
  private class SpecificNight(site: Site, s: Instant, e: Instant) extends Night {
    val start = s.toEpochMilli
    val end   = e.toEpochMilli

    override def getSite: Site      = site
    override def getStartTime: Long = start
    override def getEndTime: Long   = end
    override def getTotalTime: Long = end - start

    override def includes(time: Long): Boolean =
      (start <= time) && (time < end)
  }

  /** Calculates the bounds for the ephemeris data. */
  private def calcNight(ctx: BundleContext, site: Site, logger: Logger): Night = {
    /** Produces a "night" that ends on the nearest whole minute after the
      * next sunrise local time and starts exactly 24 hours before.
      */
    def allDay: Night = {
      val tonight = TwilightBoundedNight.forTime(OFFICIAL, Instant.now.toEpochMilli, site)
      val end     = Instant.ofEpochMilli(tonight.getEndTime).roundUp
      val start   = end.minus(Duration.ofHours(24))
      new SpecificNight(site, start, end)
    }

    val boundsTypeS = Option(ctx.getProperty(NightProp))
    val boundType   = boundsTypeS.fold(Option.empty[TwilightBoundType]) { s =>
      TwilightBoundType.values().find(_.getName.equalsIgnoreCase(s))
    }

    val night = boundType.fold(allDay) { b =>
      val boundNight = TwilightBoundedNight.forTime(b, Instant.now.toEpochMilli, site)
      val start      = Instant.ofEpochMilli(boundNight.getStartTime).roundDown
      val end        = Instant.ofEpochMilli(boundNight.getEndTime).roundUp
      new SpecificNight(site, start, end)
    }

    logger.info(s"""
        |TcsEphemerisCron using twilight bounds: ${boundType.map(_.getName).getOrElse("none")}
        |
        |  start time = ${Instant.ofEpochMilli(night.getStartTime)}
        |  end time   = ${Instant.ofEpochMilli(night.getEndTime)}
      """.stripMargin)

    night
  }

  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator. */
  def run(ctx: BundleContext)(tmpDir: File, logger: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {
    val site   = Option(SiteProperty.get(ctx)) | sys.error(s"Property `${SiteProperty.NAME}` not specified.")
    val mailer = reportMailer(ctx, site, logger)

    val exportDir = Option(ctx.getProperty(DirectoryProp)).fold(tmpDir) { pathString =>
      new File(pathString)
    }

    val odbRef = ctx.getServiceReference(classOf[IDBDatabaseService])
    val odb    = ctx.getService(odbRef)
    val night  = calcNight(ctx, site, logger)

    // Extract horizons ids for all non-sidereal observations in the database.
    val nonSid: TryExport[ISet[HorizonsDesignation]] =
      TryExport.fromTryCatch(OdbError) {
        ISet.fromList(NonSiderealTargetRefFunctor.query(odb, user).map(_.hid))
      }

    // Updates the TCS ephemeris file directory to contain only ephemeris files
    // corresponding to non-sidereal observations of interest in the ODB.  Each
    // file is updated for the current observing night.
    val action: TryExport[HorizonsDesignation ==>> FileUpdate] =
      for {
        hid <- nonSid
        res <- TcsEphemerisExport(exportDir.toPath, night, site).update(hid)
      } yield res

    def log(level: Level, msg: String, ex: Option[Throwable] = None): IO[Unit] =
      IO { logger.log(level, msg, ex.orNull) }

    def logAndMail(results: ExportError \/ (HorizonsDesignation ==>> FileUpdate)): IO[Unit] =
      results match {
        case -\/(err)     =>
          val msg = "Could not refresh ephemeris data: " + err.message
          for {
            _ <- log(Level.WARNING, msg, err.exception)
            _ <- mailer.notifyError(msg)
          } yield ()
        case \/-(updates) =>
          val (em, sm) = splitResults(updates)
          val sr       = (!sm.isEmpty) option successReport(sm)
          val er       = (!em.isEmpty) option errorReport(em)
          for {
            _ <- sr.traverse_(log(Level.INFO, _))
            _ <- er.traverse_(log(Level.WARNING, _))
            _ <- er.traverse_(mailer.notifyError)
          } yield ()
      }

    try {
      (for {
        _   <- log(Level.INFO, s"Starting ephemeris lookup for $site, writing into $exportDir")
        res <- action.run
        _   <- logAndMail(res)
        _   <- log(Level.INFO, "Finish ephemeris lookup.")
      } yield ()).unsafePerformIO()
    } finally {
      ctx.ungetService(odbRef)
    }
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
    }.mkString("\n", s"\n${"-" * 80}\n", "")

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


