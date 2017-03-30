package edu.gemini.horizons.server.backend

import java.io.{IOException, InputStream}
import java.net.{URL, URLConnection, URLEncoder}
import java.nio.charset.Charset
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.util.Locale.US
import java.util.logging.{Level, Logger}
import java.util.{Calendar, Date}
import javax.net.ssl.{HostnameVerifier, HttpsURLConnection, SSLSession}

import edu.gemini.horizons.api._
import edu.gemini.horizons.server.backend.CgiHorizonsConstants._
import edu.gemini.spModel.core.HorizonsDesignation.{AsteroidNewStyle, AsteroidOldStyle, Comet, MajorBody}
import edu.gemini.spModel.core._
import edu.gemini.util.ssl.GemSslSocketFactory

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.matching.Regex
import scala.concurrent.duration._

import scalaz.Scalaz._
import scalaz._
import scalaz.effect.{IO, Resource}

// work-in-progress; this will get a better name and will probably move elsewhere
object HorizonsService2 {
  private val LOG = Logger.getLogger("HorizonsService2")

  // Lets any remote endpoint be connectable
  private val hostnameVerifier: HostnameVerifier = new HostnameVerifier {
    def verify(s: String, sslSession: SSLSession) = true
  }

  private val timeout = 1.minute.toMillis.toInt

  private object ConnectionCharset {
    val default = Charset.forName("UTF-8")

    def set(conn: URLConnection) {
      conn.setRequestProperty("Accept-Charset", default.displayName())
    }

    def get(conn: URLConnection): Charset =
      Option(conn.getContentType).flatMap { charset =>
        val sets = for {
          cs <- charset.replace(" ", "").split(';').toList
          if cs.startsWith("charset=")
        } yield Charset.forName(cs.substring("charset=".length))
        sets.headOption
      }.getOrElse(default)
  }

  case class ResponseStream(in: InputStream, charset: Charset)

  /** The type of programs that talk to HORIZONS */
  type HS2[A] = EitherT[IO, HS2Error, A]
  object HS2 {
    def fromDisjunction[A](a: HS2Error \/ A): HS2[A] = EitherT(IO(a))
    def delay[A](a: => A): HS2[A] = EitherT(IO(a.right))
    val unit: HS2[Unit] = ().point[HS2]
  }

  implicit class HS2Ops[A](hs2: HS2[A]) {

    /** Return an equivalent action that logs failures. */
    def withResultLogging(log: Logger): HS2[A] =
      EitherT(hs2.run >>! {
        case -\/(HorizonsError(e))    => IO(log.log(Level.SEVERE, e.getMessage, e))
        case -\/(ParseError(in, msg)) => IO(log.log(Level.SEVERE, msg + "; input follows:" + in.mkString("\n")))
        case -\/(EphemerisEmpty)      => IO(log.warning("Ephemeris was empty."))
        case \/-(_)                   => IO.ioUnit
      })

    /** Return an equivalent action that ensures the provided `coda` is run in all cases. */
    def ensuring(coda: HS2[Unit]): HS2[A] =
      EitherT(hs2.run ensuring coda.run)

  }

  /** The type of failures that might arise when talking to HORIZONS. */
  sealed trait HS2Error
  case class  HorizonsError(e: HorizonsException) extends HS2Error
  case class  ParseError(input: List[String], message: String) extends HS2Error
  case object EphemerisEmpty extends HS2Error

  /** A value annotated with a name. Used when multiple results are returned. */
  case class Row[A](a: A, name: String)

  /** A partial-name search specification for HORIZONS. */
  sealed abstract class Search[A](val queryString: String) extends Product with Serializable
  object Search {
    final case class Comet(partial: String)     extends Search[HorizonsDesignation.Comet](s"NAME=$partial*;CAP")
    final case class Asteroid(partial: String)  extends Search[HorizonsDesignation.Asteroid](s"ASTNAM=$partial*")
    final case class MajorBody(partial: String) extends Search[HorizonsDesignation.MajorBody](s"$partial")
  }

  /** Construct a program that performs a partial-name search. */
  def search[A](search: Search[A]): HS2[List[Row[A]]] = {

    val queryParams: Map[String, String] =
      Map(
        BATCH      -> "1",
        TABLE_TYPE -> OBSERVER_TABLE,
        CSV_FORMAT -> NO,
        EPHEMERIS  -> NO,
        COMMAND    -> s"'${search.queryString}'"
      )

    // Lift our pure String \/ List[Row[A]] into HS2
    def parseLines(lines: List[String]): HS2[List[Row[A]]] =
      HS2.fromDisjunction(parseResponse(search, lines).leftMap(ParseError(lines, _)))

    // And finally
    horizonsRequestLines(queryParams) >>= parseLines
  }

  /** Convenience method; looks up ephemeris for the current semester. */
  def lookupEphemeris(target: HorizonsDesignation, site: Site, elems: Int): HS2[Ephemeris] =
    HS2.delay(new Semester(site)).flatMap(lookupEphemeris(target, site, elems, _))

  /** Convenience method; looks up ephemeris for the given semester. */
  def lookupEphemeris(target: HorizonsDesignation, site: Site, elems: Int, sem: Semester): HS2[Ephemeris] =
    lookupEphemeris(target, site, sem.getStartDate(site), sem.getEndDate(site), elems)

  /** Convenience method; looks up ephemeris for the given semester, with a month of padding on either side. */
  def lookupEphemerisWithPadding(target: HorizonsDesignation, site: Site, elems: Int, sem: Semester): HS2[Ephemeris] = {
    val cal   = Calendar.getInstance(site.timezone)
    val start = { cal.setTime(sem.getStartDate(site)); cal.add(Calendar.MONTH, -1); cal.getTime }
    val end   = { cal.setTime(sem.getEndDate(site));   cal.add(Calendar.MONTH,  1); cal.getTime }
    lookupEphemeris(target, site, start, end, elems)
  }

  /**
    * Look up the ephemeris for the given target when viewed from the given site, requesting `elems`
    * total elements spread uniformly over the over the given time period. The computed ephemeris may
    * contain slightly more or fewer entries than requested due to rounding, or many fewer for very
    * short timespans (no more than one entry will be returned per minute).
    */
  def lookupEphemeris(target: HorizonsDesignation, site: Site, start: Date, stop: Date, elems: Int): HS2[Ephemeris] =
    lookupEphemerisE(target, site, start, stop, elems)(_.coords).map(Ephemeris(site, _))

  /** Date formatter used for formatting the Horizons start/stop time parameters. */
  private val DateFormat = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss", US).withZone(UTC)

  /**
    * Look up the ephemeris for the given target when viewed from the given site, requesting `elems`
    * total elements spread uniformly over the over the given time period. The computed ephemeris may
    * contain slightly more or fewer entries than requested due to rounding, or many fewer for very
    * short timespans (no more than one entry will be returned per minute).
    */
  def lookupEphemerisE[E](target: HorizonsDesignation, site: Site, start: Date, stop: Date, elems: Int)(ef: EphemerisEntry => Option[E]): HS2[Long ==>> E] = {

    def formatDate(date: Date): String =
      s"'${DateFormat.format(date.toInstant)}'"

    def siteCoord(site: Site): String =
      site match {
        case Site.GN => SITE_COORD_GN
        case Site.GS => SITE_COORD_GS
      }

    def toEphemeris(r: HorizonsReply): Long ==>> E =
      ==>>.fromList {
        r.getEphemeris.asScala.toList.map { e =>
          ef(e).strengthL(e.timestamp)
        }.collect { case Some(p) => p }
      }

    // The step size in minutes, such that the total number of elements will be roughly what was
    // requested, or fewer for very short timespans (no more than 1/min)
    val stepSize = 1L max (stop.getTime - start.getTime) / (1000L * 60 * elems)

    val queryParams: Map[String, String] =
      Map(
        BATCH            -> "1",
        TABLE_TYPE       -> OBSERVER_TABLE,
        CSV_FORMAT       -> NO,
        EPHEMERIS        -> YES,
        TABLE_FIELDS_ARG -> TABLE_FIELDS,
        CENTER           -> CENTER_COORD,
        COORD_TYPE       -> COORD_TYPE_GEO,
        COMMAND          -> s"'${target.queryString}'",
        SITE_COORD       -> siteCoord(site),
        START_TIME       -> formatDate(start),
        STOP_TIME        -> formatDate(stop),
        STEP_SIZE        -> s"${stepSize}m",
        EXTRA_PRECISION  -> YES,
        TIME_DIGITS      -> FRACTIONAL_SEC
      )

    val replyType = target match {
      case Comet(_)            => HorizonsReply.ReplyType.COMET
      case AsteroidNewStyle(_) => HorizonsReply.ReplyType.MINOR_OBJECT
      case AsteroidOldStyle(_) => HorizonsReply.ReplyType.MINOR_OBJECT
      case MajorBody(_)        => HorizonsReply.ReplyType.MAJOR_PLANET
    }

    def buildEphemeris(m: ResponseStream): IO[Long ==>> E]  =
      IO(CgiReplyBuilder.readEphemeris(m.in, replyType, m.charset.displayName())).map(toEphemeris)

    // And finally
    horizonsRequest(queryParams)(buildEphemeris).ensure(EphemerisEmpty)(_.size > 0)
  }

  ///
  /// INTERNALS
  ///

  // Representation of column offsets as (start, end) pairs, as deduced from --- -------- --- -----
  private type Offsets = List[(Int, Int)]

  // Parse many results based on an expected header pattern. This will read through the tail until
  // headers are found, then parse the remaining lines (until a blank line is encountered) using
  // the function constructed by `f`, if any (otherwise it is assumed that there were not enough
  // columns found). Returns a list of values or an error message.
  private def parseMany[A](header: String, tail: List[String], headerPattern: Regex)(f: Offsets => Option[String => A] ): String \/ List[A] =
  (headerPattern.findFirstMatchIn(header) \/> ("Header pattern not found: " + headerPattern)) *>
    (tail.dropWhile(s => !s.trim.startsWith("---")) match {
      case Nil                => Nil.right // no column headers means no results!
      case colHeaders :: rows =>           // we have a header row with data rows following
        val offsets = "-+".r.findAllMatchIn(colHeaders).map(m => (m.start, m.end)).toList
        try {
          f(offsets).map(g => rows.takeWhile(_.trim.nonEmpty).map(g)) \/> "Not enough columns."
        } catch {
          case    nfe: NumberFormatException           =>  ("Number format exception: " + nfe.getMessage).left
          case sioobe: StringIndexOutOfBoundsException =>   "Column value(s) not found.".left
        }
    })

  // Split into header/tail, parse
  private def parseHeader[A](lines: List[String])(f: (String, List[String]) => String \/ List[A]): String \/ List[A] =
    lines match {
      case _ :: h :: t => f(h, t)
      case _           => "Fewer than 2 lines!".left
    }

  // Parse the result of the given search
  private def parseResponse[A](s: Search[A], lines: List[String]): \/[String, List[Row[A]]] =
    parseHeader[Row[A]](lines) { case (header, tail) =>
      s match {

        case Search.Comet(_) =>

          // Common case is that we have many results, or none.
          lazy val case0 =
            parseMany[Row[HorizonsDesignation.Comet]](header, tail, """  +Small-body Index Search Results  """.r) { os =>
              (os.lift(2) |@| os.lift(3)).tupled.map {
                case ((ods, ode), (ons, one)) => { row =>
                  val desig = row.substring(ods, ode).trim
                  val name  = row.substring(ons     ).trim // last column, so no end index because rows are ragged
                  Row(HorizonsDesignation.Comet(desig), name)
                }
              }
            }

          // Single result with form: JPL/HORIZONS      Hubble (C/1937 P1)     2015-Dec-31 11:40:21
          lazy val case1 =
            """  +([^(]+)\s+\((.+?)\)  """.r.findFirstMatchIn(header).map { m =>
              List(Row(HorizonsDesignation.Comet(m.group(2)), m.group(1)))
            } \/> "Could not match 'Hubble (C/1937 P1)' header pattern."

          // Single result with form: JPL/HORIZONS         1P/Halley           2015-Dec-31 11:40:21
          lazy val case2 =
            """  +([^/]+)/(.+?)  """.r.findFirstMatchIn(header).map { m =>
              List(Row(HorizonsDesignation.Comet(m.group(1)), m.group(2)))
            } \/> "Could not match '1P/Halley' header pattern."

          // First one that works!
          case0 orElse
            case1 orElse
            case2 orElse "Could not parse the header line as a comet".left

        case Search.Asteroid(_) =>

          // Common case is that we have many results, or none.
          lazy val case0 =
            parseMany[Row[HorizonsDesignation.Asteroid]](header, tail, """  +Small-body Index Search Results  """.r) { os =>
              (os.lift(0) |@| os.lift(1) |@| os.lift(2)).tupled.map {
                case ((ors, ore), (ods, ode), (ons, one)) => { row =>
                  val rec   = row.substring(ors, ore).trim.toInt
                  val desig = row.substring(ods, ode).trim
                  val name  = row.substring(ons     ).trim // last column, so no end index because rows are ragged
                  desig match {
                    case "(undefined)" => Row(HorizonsDesignation.AsteroidOldStyle(rec): HorizonsDesignation.Asteroid, name)
                    case des           => Row(HorizonsDesignation.AsteroidNewStyle(des): HorizonsDesignation.Asteroid, name)
                  }
                }
              }
            }

          // Single result with form: JPL/HORIZONS      90377 Sedna (2003 VB12)     2015-Dec-31 11:40:21
          lazy val case1 =
            """  +\d+ ([^(]+)\s+\((.+?)\)  """.r.findFirstMatchIn(header).map { m =>
              List(Row(HorizonsDesignation.AsteroidNewStyle(m.group(2)) : HorizonsDesignation.Asteroid, m.group(1)))
            } \/> "Could not match '90377 Sedna (2003 VB12)' header pattern."

          // Single result with form: JPL/HORIZONS      4 Vesta     2015-Dec-31 11:40:21
          lazy val case2 =
            """  +(\d+) ([^(]+?)  """.r.findFirstMatchIn(header).map { m =>
              List(Row(HorizonsDesignation.AsteroidOldStyle(m.group(1).toInt) : HorizonsDesignation.Asteroid, m.group(2)))
            } \/> "Could not match '4 Vesta' header pattern."

          // Single result with form: JPL/HORIZONS    (2016 GB222)    2016-Apr-20 15:22:36
          lazy val case3 =
            """  +\((.+?)\)  """.r.findFirstMatchIn(header).map { m =>
              List(Row(HorizonsDesignation.AsteroidNewStyle(m.group(1)) : HorizonsDesignation.Asteroid, m.group(1)))
            } \/> "Could not match '(2016 GB222)' header pattern."

          // Single result with form: JPL/HORIZONS        418993 (2009 MS9)            2016-Sep-07 18:23:54
          lazy val case4 =
            """  +\d+\s+\((.+?)\)  """.r.findFirstMatchIn(header).map { m =>
              List(Row(HorizonsDesignation.AsteroidNewStyle(m.group(1)) : HorizonsDesignation.Asteroid, m.group(1)))
            } \/> "Could not match '418993 (2009 MS9)' header pattern."

          // First one that works!
          case0 orElse
            case1 orElse
            case2 orElse
            case3 orElse
            case4 orElse "Could not parse the header line as an asteroid".left

        case Search.MajorBody(_) =>

          // Common case is that we have many results, or none.
          lazy val case0 =
            parseMany[Row[HorizonsDesignation.MajorBody]](header, tail, """Multiple major-bodies match string""".r) { os =>
              (os.lift(0) |@| os.lift(1)).tupled.map {
                case ((ors, ore), (ons, one)) => { row =>
                  val rec  = row.substring(ors, ore).trim.toInt
                  val name = row.substring(ons, one).trim
                  Row(HorizonsDesignation.MajorBody(rec.toInt), name)
                }
              }
            } .map(_.filterNot(_.a.num < 0)) // filter out spacecraft

          // Single result with form:  Revised: Aug 11, 2015       Charon / (Pluto)     901
          lazy val case1 =
            """  +(.*?) / \((.+?)\)  +(\d+) *$""".r.findFirstMatchIn(header).map { m =>
              List(Row(HorizonsDesignation.MajorBody(m.group(3).toInt), m.group(1)))
            } \/> "Could not match 'Charon / (Pluto)     901' header pattern."

          // First one that works, otherwise Nil because it falls through to small-body search
          case0 orElse
            case1 orElse Nil.right

      }
    }

  // Construct a program thet performs a HORIZONS request and yields the response as lines of text.
  private def horizonsRequestLines(params: Map[String, String]): HS2[List[String]] =
    horizonsRequest(params) { method =>
      IO {
        val s = Source.fromInputStream(method.in, method.charset.displayName())
        try     s.getLines.toList
        finally s.close()
      }
    }

  implicit def ResponseStreamResource: Resource[ResponseStream] =
    new Resource[ResponseStream] {
      def close(m: ResponseStream): IO[Unit] =
        IO(m.in.close)
    }

  private def unsafeReadRemote(baseURL: String, params: Map[String, String]): ResponseStream = {
    // This must be globally synchronized because HORIZONS only allows one request at a time
    // from a given IP address (or so it seems). An open question is whether or not it's a
    // problem that the lifetime of the GetMethod extends out of this block; it is possible
    // that `f` will be streaming data back after the monitor has been released. So far this
    // is not a problem in testing but it's worth keeping an eye on. Failure will be obvious.
    HorizonsService2.synchronized {
      val queryParams = params.map { case (k, v) => s"$k=${URLEncoder.encode(v, ConnectionCharset.default.displayName())}" }.mkString("&")
      val url: URL = new URL(s"$baseURL?$queryParams")
      LOG.info(s"Horizons request $url")
      val conn = url.openConnection().asInstanceOf[HttpsURLConnection]
      conn.setHostnameVerifier(hostnameVerifier)
      conn.setSSLSocketFactory(GemSslSocketFactory.get)
      conn.setReadTimeout(timeout)
      ConnectionCharset.set(conn)
      ResponseStream(conn.getInputStream, ConnectionCharset.get(conn))
    }
  }

  // Construct a program that performs a HORIZONS request and transforms the responses with the
  // provided function.
  private def horizonsRequest[A](params: Map[String, String])(f: ResponseStream => IO[A]): HS2[A] =
  EitherT {
    IO(unsafeReadRemote(CgiHorizonsConstants.HORIZONS_URL, params)).flatMap(f).catchSomeLeft {
      case ex: HorizonsException => HorizonsError(ex).some
      case ex: IOException       => HorizonsError(HorizonsException.create(ex)).some
    }
  }

}