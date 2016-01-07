package edu.gemini.horizons.server.backend

import edu.gemini.spModel.core._
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpException
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import edu.gemini.horizons.api.HorizonsException
import java.io.IOException
import CgiHorizonsConstants._
import edu.gemini.horizons.api.HorizonsReply
import java.util.Date
import java.text.SimpleDateFormat

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import scala.io.Source
import scalaz._, Scalaz._, scalaz.effect.IO

object HS2 {

  case class InternalError(input: List[String], message: String)
  case class Row[A](a: A, name: String)

  sealed abstract class Search[A](val queryString: String) extends Product with Serializable {
    def parseResponse(lines: List[String]): String \/ List[HS2.Row[A]]
  }
  object Search {

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

    // Comet
    final case class Comet(partial: String) extends Search[HorizonsDesignation.Comet](s"COMNAM=$partial*;CAP") {
      def parseResponse(lines: List[String]): String \/ List[HS2.Row[HorizonsDesignation.Comet]] =
        parseHeader[HS2.Row[HorizonsDesignation.Comet]](lines) { case (header, tail) => 

          // Common case is that we have many results, or none.
          lazy val case0 =
            parseMany[HS2.Row[HorizonsDesignation.Comet]](header, tail, """  +Small-body Search Results  """.r) { os =>
              (os.lift(2) |@| os.lift(3)).tupled.map {
                case ((ods, ode), (ons, one)) => { row =>
                  val desig = row.substring(ods, ode).trim
                  val name  = row.substring(ons, one).trim
                  HS2.Row(HorizonsDesignation.Comet(desig), name)
                }
              }
            }

          // Single result with form: JPL/HORIZONS      Hubble (C/1937 P1)     2015-Dec-31 11:40:21
          lazy val case1 =
            """  +([^(]+)\s+\((.+?)\)  """.r.findFirstMatchIn(header).map { m =>
              List(HS2.Row(HorizonsDesignation.Comet(m.group(2)), m.group(1)))
            } \/> "Could not match 'Hubble (C/1937 P1)' header pattern."

          // Single result with form: JPL/HORIZONS         1P/Halley           2015-Dec-31 11:40:21
          lazy val case2 =
            """  +([^/]+)/(.+?)  """.r.findFirstMatchIn(header).map { m =>
              List(HS2.Row(HorizonsDesignation.Comet(m.group(1)), m.group(2)))
            } \/> "Could not match '1P/Halley' header pattern."

          // First one that works!
          case0 orElse
          case1 orElse
          case2 orElse "Could not parse the header line.".left
        }
    }

    // Asteroid
    final case class Asteroid(partial: String) extends Search[HorizonsDesignation.Asteroid](s"ASTNAM=$partial*") {
      def parseResponse(lines: List[String]): \/[String, List[Row[HorizonsDesignation.Asteroid]]] =
        parseHeader[Row[HorizonsDesignation.Asteroid]](lines) { case (header, tail) =>

          // Common case is that we have many results, or none.
          lazy val case0 =
            parseMany[HS2.Row[HorizonsDesignation.Asteroid]](header, tail, """  +Small-body Index Search Results  """.r) { os =>
              (os.lift(0) |@| os.lift(1) |@| os.lift(2)).tupled.map {
                case ((ors, ore), (ods, ode), (ons, one)) => { row =>
                  val rec   = row.substring(ors, ore).trim.toInt
                  val desig = row.substring(ods, ode).trim
                  val name  = row.substring(ons     ).trim // last column
                  desig match {
                    case "(undefined)" => HS2.Row(HorizonsDesignation.AsteroidOldStyle(rec): HorizonsDesignation.Asteroid, name)
                    case des           => HS2.Row(HorizonsDesignation.AsteroidNewStyle(des): HorizonsDesignation.Asteroid, name)
                  }
                }
              }
            }

          // Single result with form: JPL/HORIZONS      90377 Sedna (2003 VB12)     2015-Dec-31 11:40:21
          lazy val case1 =
            """  +\d+ ([^(]+)\s+\((.+?)\)  """.r.findFirstMatchIn(header).map { m =>
              List(HS2.Row(HorizonsDesignation.AsteroidNewStyle(m.group(2)) : HorizonsDesignation.Asteroid, m.group(1)))
            } \/> "Could not match '90377 Sedna (2003 VB12)' header pattern."

          // Single result with form: JPL/HORIZONS      4 Vesta     2015-Dec-31 11:40:21
          lazy val case2 =
            """  +(\d+) ([^(]+?)  """.r.findFirstMatchIn(header).map { m =>
              List(HS2.Row(HorizonsDesignation.AsteroidOldStyle(m.group(1).toInt) : HorizonsDesignation.Asteroid, m.group(2)))
            } \/> "Could not match '4 Vesta' header pattern."

          // First one that works!
          case0 orElse
          case1 orElse
          case2 orElse "Could not parse the header line.".left

        }
    }


    final case class MajorBody(partial: String) extends Search[HorizonsDesignation.MajorBody](s"$partial") {
      def parseResponse(lines: List[String]): \/[String, List[Row[HorizonsDesignation.MajorBody]]] =
        parseHeader[Row[HorizonsDesignation.MajorBody]](lines) { case (header, tail) =>

          // Common case is that we have many results, or none.
          lazy val case0 = 
            parseMany[HS2.Row[HorizonsDesignation.MajorBody]](header, tail, """Multiple major-bodies match string""".r) { os =>
              (os.lift(0) |@| os.lift(1)).tupled.map {
                case ((ors, ore), (ons, one)) => { row =>
                  val rec  = row.substring(ors, ore).trim.toInt
                  val name = row.substring(ons, one).trim
                  HS2.Row(HorizonsDesignation.MajorBody(rec.toInt), name)
                }
              }
            } .map(_.filterNot(_.a.num < 0)) // filter out spacecraft

          // Single result with form:  Revised: Aug 11, 2015       Charon / (Pluto)     901
          lazy val case1 =
            """  +(.*?)  +(\d+) $""".r.findFirstMatchIn(header).map { m =>
              List(HS2.Row(HorizonsDesignation.MajorBody(m.group(2).toInt), m.group(1)))
            } \/> "Could not match 'Charon / (Pluto)     901' header pattern."

          // First one that works, otherwise Nil because it falls through to small-body search
          case0 orElse 
          case1 orElse Nil.right
        }
    }

  }

  def search[A](search: Search[A]): EitherT[IO, InternalError, List[Row[A]]] = {
    val queryParams: Map[String, String] =
      Map(
        BATCH      -> "1",
        TABLE_TYPE -> OBSERVER_TABLE,
        CSV_FORMAT -> NO,
        EPHEMERIS  -> NO,
        COMMAND    -> s"'${search.queryString}'"
      )
    EitherT(fetch(queryParams).map(ss => search.parseResponse(ss).leftMap(HS2.InternalError(ss, _))))
  }

  def lookup(d: HorizonsDesignation, site: Site, start: Long, duration: Long, steps: Int): Throwable \/ Ephemeris =
    ???


  def fetch(params: Map[String, String]): IO[List[String]] =
    genFetch(params) { method =>
      val s = Source.fromInputStream(method.getResponseBodyAsStream, method.getRequestCharSet)
      try     s.getLines.toList
      finally s.close()
    }

  def genFetch[A](params: Map[String, String])(f: GetMethod => A): IO[A] =
    IO {
      // HORIZONS allows only one request at a time for a given host :-/
      HS2.synchronized {
        val method = new GetMethod(CgiHorizonsConstants.HORIZONS_URL)
        try {
          method.setQueryString(params.map { case (k, v) => new NameValuePair(k, v) } .toArray)
          (new HttpClient).executeMethod(method)
          f(method)
        } catch {
          case ex: HorizonsException => throw ex
          case ex: HttpException     => throw HorizonsException.create(ex)
          case ex: IOException       => throw HorizonsException.create(ex)
        } finally {
          method.releaseConnection
        }
      }
    }

}



object Executor {

  def execute(query: HorizonsDesignation, site: Site, elements: Int): IO[Ephemeris] =
    execute(query, site, elements, new Semester(site))

  def execute(query: HorizonsDesignation, site: Site, elements: Int, sem: Semester): IO[Ephemeris] =
    execute(query, site, sem.getStartDate(site), sem.getEndDate(site), elements)

  def execute(query: HorizonsDesignation, site: Site, start: Date, stop: Date, elements: Int): IO[Ephemeris] = {

    def formatDate(date: Date): String = {
      val df = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss")
      s"'${df.format(date)}'"
    }

    def siteCoord(site: Site): String =
      site match {
        case Site.GN => SITE_COORD_GN
        case Site.GS => SITE_COORD_GS
      }

    def toEphemeris(r: HorizonsReply): Ephemeris = 
      ==>>.fromList {
        r.getEphemeris.asScala.toList.map { e =>
          val cs = e.getCoordinates
          Coordinates.fromDegrees(cs.getRaDeg, cs.getDecDeg).strengthL(e.getDate.getTime)
        } .collect { case Some(p) => p }
      }

    // The step size in minutes, such that the total number of elements will be roughly what was
    // requested, or fewer for very short timespans (no more than 1/min)
    val stepSize = 1L max (stop.getTime - start.getTime) / (1000L * 60 * elements)

    val queryParams: Map[String, String] =
      Map(
        BATCH            -> "1",
        TABLE_TYPE       -> OBSERVER_TABLE,
        CSV_FORMAT       -> NO,
        EPHEMERIS        -> YES,
        TABLE_FIELDS_ARG -> TABLE_FIELDS,
        CENTER           -> CENTER_COORD,
        COORD_TYPE       -> COORD_TYPE_GEO,
        COMMAND          -> s"'${query.queryString}'",
        SITE_COORD       -> siteCoord(site),
        START_TIME       -> formatDate(start),
        STOP_TIME        -> formatDate(stop),
        STEP_SIZE        -> s"${stepSize}m"
      )

    HS2.genFetch(queryParams)(m => CgiReplyBuilder.buildResponse(m.getResponseBodyAsStream, m.getRequestCharSet)).map(toEphemeris)

  }

}


