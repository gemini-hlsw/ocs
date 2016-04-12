package edu.gemini.dbTools.ephemeris

import edu.gemini.horizons.api._
import edu.gemini.horizons.server.backend.HorizonsService2
import edu.gemini.horizons.server.backend.HorizonsService2.HS2Error
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.skycalc.{JulianDate, TwilightBoundedNight, Night}
import edu.gemini.skycalc.TwilightBoundType.NAUTICAL
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.osgi.SiteProperty
import org.osgi.framework.BundleContext

import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Paths}
import java.security.Principal
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.logging.{Level, Logger}
import java.util.{TimeZone, Date}
import java.util.Locale.US

import scalaz._
import Scalaz._
import scalaz.effect._

/** TCS ephemeris export cron job.
  */
object TcsEphemerisExport {

  // If specified, ephemeris files will be written here.
  val DirectoryProp = "edu.gemini.dbTools.tcs.ephemeris.directory"

  // We will request this many elements from horizons, though the actual number
  // provided may differ.  The TCS maximum is 1440, but horizons may return a
  // few more than requested.
  val ElementCount  = 1430

  // Text date format
  val DateFormatString = "yyyy-MMM-dd HH:mm"

  type EphemerisElement = (Coordinates, Double, Double)
  type EphemerisMap     = Long ==>> EphemerisElement

  /** Errors that may occur while exporting ephemeris data. */
  sealed trait ExportError

  /** An error that happens when working with the ODB to extract the non-
    * sidereal observation references with their horizons ids.
    */
  case class OdbError(ex: Throwable) extends ExportError

  /** An error that happens while working with the horizons service itself. */
  case class HorizonsError(hid: HorizonsDesignation, e: HorizonsService2.HS2Error) extends ExportError

  /** An error that happens when writing out ephemeris data for the TCS. */
  case class WriteError(hid: HorizonsDesignation, ex: Throwable) extends ExportError

  object ExportError {
    private def reportH2Error(hid: HorizonsDesignation, h2: HS2Error): (String, Option[Throwable]) = h2 match {
      case HorizonsService2.HorizonsError(e)   =>
        (s"$hid: Error communicating with horizons service", Some(e))

      case HorizonsService2.ParseError(_, msg) =>
        (s"$hid: Could not parse response from horizons service: $msg", None)

      case HorizonsService2.EphemerisEmpty     =>
        (s"$hid: No response from horizons", None)
    }

    def report(e: ExportError): (String, Option[Throwable]) = e match {
      case OdbError(ex)           =>
        ("Error looking up nonsidereal observations in the database", Some(ex))

      case HorizonsError(hid, h2) =>
        reportH2Error(hid, h2)

      case WriteError(hid, ex)    =>
        (s"$hid: Error writing ephemeris file", Some(ex))
    }

    def logError(e: ExportError, log: Logger, prefix: String): Unit = {
      val (msg, ex) = report(e)
      log.log(Level.WARNING, s"$prefix$msg", ex.orNull)
    }
  }

  type TryExport[A] = EitherT[IO, ExportError, A]

  object TryExport {
    def fromDisjunction[A](a:ExportError \/ A): TryExport[A] =
      EitherT(IO(a))

    def fromTryCatch[A](e: Throwable => ExportError)(a: => A): TryExport[A] =
      fromDisjunction {
        \/.fromTryCatchNonFatal(a).leftMap(e)
      }
  }

  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator.
    */
  def run(ctx: BundleContext)(tmpDir: File, log: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {
    val site = Option(SiteProperty.get(ctx)) | sys.error(s"Property `${SiteProperty.NAME}` not specified.")

    val exportDir = Option(ctx.getProperty(DirectoryProp)).fold(tmpDir) { path =>
      new File(path)
    }

    val odbRef = ctx.getServiceReference(classOf[IDBDatabaseService])
    val odb    = ctx.getService(odbRef)
    val night  = TwilightBoundedNight.forTime(NAUTICAL, Instant.now.toEpochMilli, site)
    val exp    = new TcsEphemerisExport(exportDir, night, site, odb, user)

    log.info(s"Starting ephemeris lookup for $site, writing into $exportDir")
    try {
      exp.export.run.unsafePerformIO() match {
        case -\/(err)     =>
          ExportError.logError(err, log, "Could not refresh ephemeris data: ")

        case \/-(updates) =>
          updates.toList.foreach { case (hid, res) =>
            res match {
              case -\/(err)  => ExportError.logError(err, log, "")
              case \/-(file) => log.log(Level.INFO, s"$hid: updated at ${Instant.ofEpochMilli(file.lastModified())}")
            }
          }
      }
    } finally {
      ctx.ungetService(odbRef)
    }
    log.info("Finish ephemeris lookup.")
  }


  private def formatCoords(coords: Coordinates): String = {
    val ra  = Angle.formatHMS(coords.ra.toAngle, " ", 4)
    val dec = Declination.formatDMS(coords.dec, " ", 3)

    s"$ra $dec"
  }

  /** Writes the given ephemeris map to a String in the format expected by the
    * TCS.
    */
  def formatEphemeris(em: EphemerisMap): String = {
    val dfm = new SimpleDateFormat(DateFormatString, US)
    dfm.setTimeZone(TimeZone.getTimeZone("UTC"))

    em.toList.map { case (time, (coords, raTrack, decTrack)) =>
      val timeS     = dfm.format(new Date(time))
      val jdS       = f"${new JulianDate(time).toDouble}%.9f"
      val coordsS   = formatCoords(coords)
      val raTrackS  = f"$raTrack%9.5f"
      val decTrackS = f"$decTrack%9.5f"
      s" $timeS $jdS     $coordsS $raTrackS $decTrackS"
    }.mkString("$$SOE\n", "\n", "\n$$EOE\n")
  }

  // Need Order to use HorizonsDesignation as a ==>> key
  implicit val OrderHorizonsDesignation: Order[HorizonsDesignation] =
    Order.orderBy(_.toString)
}

class TcsEphemerisExport(dir: File, night: Night, site: Site, odb: IDBDatabaseService, user: java.util.Set[Principal]) {
  import TcsEphemerisExport._

  val start = new Date(night.getStartTime)
  val end   = new Date(night.getEndTime)

  val lookupNonSiderealObservations: TryExport[Set[HorizonsDesignation]] =
    TryExport.fromTryCatch(OdbError) {
      NonSiderealObservationFunctor.query(odb, user).map(_.hid).toSet
    }

  val export: TryExport[HorizonsDesignation ==>> (ExportError \/ File)] =
    lookupNonSiderealObservations >>= exportAll

  private def exportAll(nos: Set[HorizonsDesignation]): TryExport[HorizonsDesignation ==>> (ExportError \/ File)] = {
    // HorizonsDesignation ==>> TryExport[File]
    val exportMap = ==>>.fromList(nos.toList.map(hid => (hid, exportOne(dir, hid))))

    // IO[HorizonsDesignation ==>> (ExportError \/ File)]
    val exportOp  = exportMap.traverse(_.run)

    // TryExport[HorizonsDesignation ==> (ExportError \/ File)]
    EitherT(exportOp.map(_.right[ExportError]))
  }

  def exportOne(dir: File, hid: HorizonsDesignation): TryExport[File] =
    for {
      em <- lookupEphemeris(hid)
      f  <- writeEphemeris(hid, em)
    } yield f

  def lookupEphemeris(hid: HorizonsDesignation): TryExport[EphemerisMap] =
    HorizonsService2.lookupEphemerisE[EphemerisElement](hid, site, start, end, ElementCount) { (ee: EphemerisEntry) =>
      ee.coords.map((_, ee.getRATrack, ee.getDecTrack))
    }.leftMap(e => HorizonsError(hid, e): ExportError)

  def writeEphemeris(hid: HorizonsDesignation, em: EphemerisMap): TryExport[File] = {
    // See TcsConfig.ephemerisFile
    val fileName = URLEncoder.encode(s"${hid.toString}.eph", UTF_8.name)
    val file     = new File(dir, fileName)
    TryExport.fromTryCatch(t => WriteError(hid, t)) {
      Files.write(Paths.get(file.toURI), formatEphemeris(em).getBytes(UTF_8))
      file
    }
  }

}
