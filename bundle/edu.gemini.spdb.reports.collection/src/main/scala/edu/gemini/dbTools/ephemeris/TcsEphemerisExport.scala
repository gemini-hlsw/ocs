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
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security.Principal
import java.text.{DecimalFormat, SimpleDateFormat}
import java.time.Instant
import java.util.logging.{Level, Logger}
import java.util.{TimeZone, Date}

import scalaz._
import Scalaz._
import scalaz.effect._

/** TCS ephemeris export cron job.
  */
object TcsEphemerisExport {

  // If specified, ephemeris files will be written here.
  val DirectoryProp = "edu.gemini.dbTools.tcs.ephemeris.directory"

  // We will request this many elements from horizons, though the actual number
  // provided may differ.
  val ElementCount  = 1440

  type EphemerisElement = (Coordinates, Double, Double)
  type EphemerisMap     = Long ==>> EphemerisElement

  sealed trait EEError
  case class OdbError(ex: Throwable) extends EEError
  case class HorizonsError(hid: HorizonsDesignation, e: HorizonsService2.HS2Error) extends EEError
  case class WriteError(hid: HorizonsDesignation, ex: Throwable) extends EEError

  object EEError {
    private def reportH2Error(hid: HorizonsDesignation, h2: HS2Error): (String, Option[Throwable]) = h2 match {
      case HorizonsService2.HorizonsError(e)   =>
        (s"$hid: Error communicating with horizons service", Some(e))

      case HorizonsService2.ParseError(_, msg) =>
        (s"$hid: Could not parse response from horizons service: $msg", None)

      case HorizonsService2.EphemerisEmpty     =>
        (s"$hid: No response from horizons", None)
    }

    def report(e: EEError): (String, Option[Throwable]) = e match {
      case OdbError(ex)           =>
        ("Error looking up nonsidereal observations in the database", Some(ex))

      case HorizonsError(hid, h2) =>
        reportH2Error(hid, h2)

      case WriteError(hid, ex)    =>
        (s"$hid: Error writing ephemeris file", Some(ex))
    }

    def logError(e: EEError, log: Logger, prefix: String): Unit = {
      val (msg, ex) = report(e)
      log.log(Level.WARNING, s"$prefix$msg", ex.orNull)
    }
  }

  type EE[A] = EitherT[IO, EEError, A]

  object EE {
    def fromDisjunction[A](a:EEError \/ A): EE[A] =
      EitherT(IO(a))

    def fromTryCatch[A](e: Throwable => EEError)(a: => A): EE[A] =
      fromDisjunction {
        \/.fromTryCatchNonFatal(a).leftMap(e)
      }
  }

  /** Cron job entry point.  See edu.gemini.spdb.cron.osgi.Activator.
    */
  def run(ctx: BundleContext)(tmpDir: File, log: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {
    val site = SiteProperty.get(ctx)

    val exportDir = Option(ctx.getProperty(DirectoryProp)).fold(tmpDir) { path =>
      new File(path)
    }

    val odbRef = ctx.getServiceReference(classOf[IDBDatabaseService])
    val odb    = ctx.getService(odbRef)
    val night  = TwilightBoundedNight.forTime(NAUTICAL, Instant.now.toEpochMilli, site)
    val tee    = new TcsEphemerisExport(exportDir, night, site, odb, user)

    log.info(s"Starting ephemeris lookup for $site, writing into $exportDir")
    try {
      tee.export.run.unsafePerformIO() match {
        case -\/(err)     =>
          EEError.logError(err, log, "Could not refresh emphemeris data: ")

        case \/-(updates) =>
          updates.toList.foreach { case (hid, res) =>
            res match {
              case -\/(err)  => EEError.logError(err, log, "")
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
    // TODO: fix coordinate formating in core.spModel and make it flexible
    // TODO: enough to specify precision, then use that instead of doing it here
    def normalize(a: Int, b: Int, c: Double, fractionalDigits: Int): String = {
      val df = new DecimalFormat("00." + ("0" * fractionalDigits))

      val s0 = df.format(c)
      val (s, carryC) = s0.startsWith("60.") ? (("00", 1)) | ((s0, 0))

      val m0 = b + carryC
      val (m, carryB) = (m0 == 60) ? (("00", 1)) | ((f"$m0%02d", 0))

      val x0 = a + carryB
      val x  = f"$x0%02d"

      s"$x $m $s"
    }

    val ra0  = coords.ra.toAngle.toHourAngle
    val ra1  = normalize(ra0.hours, ra0.minutes, ra0.seconds, 4)
    val raS  = ra1.startsWith("24") ? "00 00 00.0000" | ra1

    val dec0 = coords.dec.toDegrees
    val sgn  = (dec0 < 0) ? "-" | ""
    val dec1 = Angle.fromDegrees(dec0.abs).toSexigesimal
    val decS = sgn + normalize(dec1.degrees, dec1.minutes, dec1.seconds, 3)

    s"$raS $decS"
  }

  /** Writes the given ephemeris map to a String in the format expected by the
    * TCS.
    */
  def formatEphemeris(em: EphemerisMap): String = {
    val dfm = new SimpleDateFormat("yyyy-MMM-dd HH:mm")
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

  val lookupNonSiderealObservations: EE[HorizonsDesignation ==>> Set[String]] =
    EE.fromTryCatch(OdbError) {
      val ns = NonSiderealObservationFunctor.query(odb, user)
      (==>>.empty[HorizonsDesignation, Set[String]]/:ns) { (m, n) =>
        m.updateAppend(n.hid, Set(n.targetName))
      }
    }

  val export: EE[HorizonsDesignation ==>> (EEError \/ File)] =
    lookupNonSiderealObservations >>= exportAll

  private def exportAll(nos: HorizonsDesignation ==>> Set[String]): EE[HorizonsDesignation ==>> (EEError \/ File)] =
    EitherT(nos.mapOptionWithKey { (hid, names) =>
      // There can be multiple names for the same horizons id.  We'll just pick
      // one at random for now ...
      names.headOption.map { exportOne(dir, hid, _) }
    }.traverse(_.run).map(_.right[EEError]))

  def exportOne(dir: File, hid: HorizonsDesignation, name: String): EE[File] =
    for {
      em <- lookupEphemeris(hid)
      f  <- writeEphemeris(hid, name, em)
    } yield f

  def lookupEphemeris(hid: HorizonsDesignation): EE[EphemerisMap] = {
    val start = new Date(night.getStartTime)
    val end   = new Date(night.getEndTime)
    val map   = HorizonsService2.lookupEphemerisE[EphemerisElement](hid, site, start, end, ElementCount) { (ee: EphemerisEntry) =>
      ee.coords.map((_, ee.getRATrack, ee.getDecTrack))
    }

    map.leftMap(e => HorizonsError(hid, e): EEError)
  }

  def writeEphemeris(hid: HorizonsDesignation, name: String, em: EphemerisMap): EE[File] = {
    val fileName = s"${name}_${hid.toString}.eph".replaceAll("/", "-")
    val file     = new File(dir, fileName)
    EE.fromTryCatch(t => WriteError(hid, t)) {
      Files.write(Paths.get(file.toURI), formatEphemeris(em).getBytes(StandardCharsets.UTF_8))
      file
    }
  }

}
