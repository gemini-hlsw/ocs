package edu.gemini.horizons.server.backend

import edu.gemini.spModel.core._
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpException
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import edu.gemini.horizons.api.HorizonsException
import java.io.IOException
import CgiHorizonsConstants._
import scalaz._, Scalaz._, scalaz.effect.IO

object HS2 {

  case class InternalError(input: List[String], message: String)

  sealed abstract class Search[A](val queryString: String) extends Product with Serializable {
    def parseResponse(lines: List[String]): InternalError \/ List[HS2.Row[A]]
  }
  object Search {

    final case class Comet(partial: String) extends Search[HorizonsDesignation.Comet](s"COMNAM=$partial*;CAP") {
      def parseResponse(lines: List[String]): InternalError \/ List[HS2.Row[HorizonsDesignation.Comet]] =
        lines match {
          case _ :: header :: tail =>

            // Common case is that we have many results, or none.
            lazy val case0: InternalError \/ List[HS2.Row[HorizonsDesignation.Comet]] =
              ("""  +Small-body Search Results  """.r.findFirstMatchIn(header) \/>
                InternalError(lines, "'Small-body Search Results' header not found")).flatMap { _ =>
                lines.dropWhile(s => !s.trim.startsWith("---")) match {
                  case Nil => Nil.right // no column headers means no results!
                  case colHeaders :: rows => // designation and name at column offsets 2 and 3
                    val offsets = "-+".r.findAllMatchIn(colHeaders).map(m => (m.start, m.end)).toList
                    try {
                      (offsets.lift(2) |@| offsets.lift(3)).tupled.map {
                        case ((ods, ode), (ons, one)) =>
                          rows.takeWhile(_.trim.nonEmpty).map { row =>
                            val desig = row.substring(ods, ode).trim
                            val name  = row.substring(ons, one).trim
                            HS2.Row(HorizonsDesignation.Comet(desig), name)
                          }
                      } \/> InternalError(lines, "Not enough columns.")
                    } catch {
                      case sioobe: StringIndexOutOfBoundsException =>
                        InternalError(lines, "Column value(s) not found.").left
                    }
                }
              }

            // Single result with form: JPL/HORIZONS      Hubble (C/1937 P1)     2015-Dec-31 11:40:21
            lazy val case1 =
              """  +([^(]+)\s+\((.+?)\)  """.r.findFirstMatchIn(header).map { m =>
                List(HS2.Row(HorizonsDesignation.Comet(m.group(2)), m.group(1)))
              } \/> InternalError(lines, "Could not match 'Hubble (C/1937 P1)' header pattern.")

            // Single result with form: JPL/HORIZONS         1P/Halley           2015-Dec-31 11:40:21
            lazy val case2 =
              """  +([^/]+)/(.+?)  """.r.findFirstMatchIn(header).map { m =>
                List(HS2.Row(HorizonsDesignation.Comet(m.group(1)), m.group(2)))
              } \/> InternalError(lines, "Could not match '1P/Halley' header pattern.")

            // First one that works!
            case0 orElse
              case1 orElse
              case2 orElse InternalError(lines, "Could not parse the header line.").left

          case _ => InternalError(lines, "Fewer than 2 lines!").left

        }
    }


    final case class Asteroid(partial: String) extends Search[HorizonsDesignation.Asteroid](s"ASTNAM=$partial*") {
      def parseResponse(lines: List[String]): \/[InternalError, List[Row[HorizonsDesignation.Asteroid]]] =
        lines match {
          case _ :: header :: tail =>

            // Common case is that we have many results, or none.
            lazy val case0: InternalError \/ List[HS2.Row[HorizonsDesignation.Asteroid]] =
              ("""  +Small-body Index Search Results  """.r.findFirstMatchIn(header) \/>
                InternalError(lines, "'Small-body Index Search Results' header not found")).flatMap { _ =>
                lines.dropWhile(s => !s.trim.startsWith("---")) match {
                  case Nil => Nil.right // no column headers means no results!
                  case colHeaders :: rows => // designation and name at column offsets 2 and 3
                    val offsets = "-+".r.findAllMatchIn(colHeaders).map(m => (m.start, m.end)).toList
                    try {
                      (offsets.lift(0) |@| offsets.lift(1) |@| offsets.lift(2)).tupled.map {
                        case ((ors, ore), (ods, ode), (ons, one)) =>
                          rows.takeWhile(_.trim.nonEmpty).map { row =>
                            val rec = row.substring(ors, ore).trim.toInt
                            val desig = row.substring(ods, ode).trim
                            val name = row.substring(ons).trim // last column
                            if (desig == "(undefined)") {
                              HS2.Row(HorizonsDesignation.AsteroidOldStyle(rec): HorizonsDesignation.Asteroid, name)
                            } else {
                              HS2.Row(HorizonsDesignation.AsteroidNewStyle(desig): HorizonsDesignation.Asteroid, name)
                            }
                          }
                      } \/> InternalError(lines, "Not enough columns.")
                    } catch {
                      case nfe: NumberFormatException =>
                        InternalError(lines, "Old-style identifier not an integer.").left
                      case sioobe: StringIndexOutOfBoundsException =>
                        InternalError(lines, "Column value(s) not found.").left
                    }
                }
              }

            // Single result with form: JPL/HORIZONS      90377 Sedna (2003 VB12)     2015-Dec-31 11:40:21
            lazy val case1 =
              """  +\d+ ([^(]+)\s+\((.+?)\)  """.r.findFirstMatchIn(header).map { m =>
                List(HS2.Row(HorizonsDesignation.AsteroidNewStyle(m.group(2)) : HorizonsDesignation.Asteroid, m.group(1)))
              } \/> InternalError(lines, "Could not match '90377 Sedna (2003 VB12)' header pattern.")

            // Single result with form: JPL/HORIZONS      4 Vesta     2015-Dec-31 11:40:21
            lazy val case2 =
              """  +(\d+) ([^(]+?)  """.r.findFirstMatchIn(header).map { m =>
                List(HS2.Row(HorizonsDesignation.AsteroidOldStyle(m.group(1).toInt) : HorizonsDesignation.Asteroid, m.group(2)))
              } \/> InternalError(lines, "Could not match '4 Vesta' header pattern.")

            // First one that works!
            case0 orElse
            case1 orElse
            case2 orElse InternalError(lines, "Could not parse the header line.").left

          case _ => InternalError(lines, "Fewer than 2 lines!").left

        }

    }


    final case class MajorBody(partial: String) extends Search[HorizonsDesignation.MajorBody](s"$partial") {
      def parseResponse(lines: List[String]): \/[InternalError, List[Row[HorizonsDesignation.MajorBody]]] =
        lines match {
          case _ :: header :: tail =>

            // Common case is that we have many results, or none.
            lazy val case0: InternalError \/ List[HS2.Row[HorizonsDesignation.MajorBody]] =
              ("""Multiple major-bodies match string""".r.findFirstMatchIn(header) \/>
                InternalError(lines, "'Multiple major-bodies match string' header not found")).flatMap { _ =>
                lines.dropWhile(s => !s.trim.startsWith("---")) match {
                  case Nil => Nil.right // no column headers means no results!
                  case colHeaders :: rows => // designation and name at column offsets 2 and 3
                    val offsets = "-+".r.findAllMatchIn(colHeaders).map(m => (m.start, m.end)).toList
                    try {
                      (offsets.lift(0) |@| offsets.lift(1)).tupled.map {
                        case ((ors, ore), (ons, one)) =>
                          rows.takeWhile(_.trim.nonEmpty).map { row =>
                            val rec = row.substring(ors, ore).trim.toInt
                            val name = row.substring(ons, one).trim
                            HS2.Row(HorizonsDesignation.MajorBody(rec.toInt), name)
                          }.filterNot(_.a.num < 0) // filter out spacecraft
                      } \/> InternalError(lines, "Not enough columns.")
                    } catch {
                      case nfe: NumberFormatException =>
                        InternalError(lines, "Old-style identifier not an integer.").left
                      case sioobe: StringIndexOutOfBoundsException =>
                        InternalError(lines, "Column value(s) not found.").left
                    }
                }
              }

            // Single result with form:  Revised: Aug 11, 2015       Charon / (Pluto)     901
            lazy val case1 =
              """  +(.*?)  +(\d+) $""".r.findFirstMatchIn(header).map { m =>
                List(HS2.Row(HorizonsDesignation.MajorBody(m.group(2).toInt), m.group(1)))
              } \/> InternalError(lines, "Could not match 'Charon / (Pluto)     901' header pattern.")

            // First one that works, otherwise Nil because it falls through to small-body search
            case0 orElse case1 orElse Nil.right

          case _ => InternalError(lines, "Fewer than 2 lines!").left

        }
    }

  }

  case class Row[A](a: A, name: String)

  def search[A](s: Search[A]): Throwable \/ List[Row[A]] =
    ???

  def lookup(d: HorizonsDesignation, site: Site, start: Long, duration: Long, steps: Int): Throwable \/ Ephemeris =
    ???

}


object Searcher {

  def search[A](search: HS2.Search[A]): HS2.InternalError \/ List[HS2.Row[A]] = {

    val queryParams: Map[String, String] =
      Map(
        BATCH      -> "1",
        TABLE_TYPE -> OBSERVER_TABLE,
        CSV_FORMAT -> NO,
        EPHEMERIS  -> NO,
        COMMAND    -> s"'${search.queryString}'"
      )

    val lines = fetch(queryParams).unsafePerformIO()
    // lines.foreach(println)
    search.parseResponse(lines)
  }


  def fetch(params: Map[String, String]): IO[List[String]] =
    IO {
      // HORIZONS allows only one request at a time for a given host
      Searcher.synchronized {
        val method = new GetMethod(CgiHorizonsConstants.HORIZONS_URL)
        try {
          method.setQueryString(params.map { case (k, v) => new NameValuePair(k, v) } .toArray)
          (new HttpClient).executeMethod(method)
          val s = scala.io.Source.fromInputStream(method.getResponseBodyAsStream, method.getRequestCharSet)
          try {
            s.getLines.toList
          } finally s.close()
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

//
//
//object Executor {
//
//  val initialParams: Map[String, String] =
//    Map(
//      BATCH            -> "1",
//      TABLE_TYPE       -> OBSERVER_TABLE,
//      CSV_FORMAT       -> NO,
//      EPHEMERIS        -> YES,
//      TABLE_FIELDS_ARG -> TABLE_FIELDS,
//      CENTER           -> CENTER_COORD,
//      COORD_TYPE       -> COORD_TYPE_GEO
//    )
//
//  def siteParams(site: Site): Map[String, String] =
//    site match {
//      case Site.GN => Map(SITE_COORD -> SITE_COORD_GN)
//      case Site.GS => Map(SITE_COORD -> SITE_COORD_GS)
//    }
//
//  def dateParams(startDate: Date, endDate: Date): Map[String, String] = {
//    def formatDate(date: Date): String = {
//      val formatter: DateFormat = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss")
//      val sb: StringBuilder = new StringBuilder("'")
//      sb.append(formatter.format(date))
//      sb.append("'")
//      return sb.toString
//    }
//    Map(
//      START_TIME -> formatDate(startDate),
//      STOP_TIME  -> formatDate(endDate)
//    )
//  }
//
//  def commandParam(id: String): Map[String, String] =
//    Map(COMMAND -> id)
//
//  def stepParams(stepSize: Int, stepUnits: StepUnits): Map[String, String] =
//    if (stepSize > 0) Map(STEP_SIZE -> (stepSize + stepUnits.suffix))
//    else Map.empty
//
//  def execute(query: HorizonsQuery): HorizonsReply = {
//
//    var objectId: String = query.getObjectId.trim
//    if (query.getObjectType eq HorizonsQuery.ObjectType.MINOR_BODY) {
//      objectId += ";"
//    }
//    if (objectId.contains(" ")) {
//      val sb: StringBuffer = new StringBuffer
//      sb.append("'")
//      sb.append(objectId)
//      sb.append("'")
//      objectId = sb.toString
//    }
//
//    val queryParams: Map[String, String] =
//      initialParams ++
//      siteParams(query.getSite) ++
//      dateParams(query.getStartDate, query.getEndDate) ++
//      commandParam(objectId) ++
//      stepParams(query.getStepSize, query.getStepUnits)
//
//    val method: GetMethod = new GetMethod(CgiHorizonsConstants.HORIZONS_URL)
//    method.setQueryString(queryParams.map { case (k, v) => new NameValuePair(k, v) } .toArray)
//    var reply: HorizonsReply = null
//    val client: HttpClient = new HttpClient
//    try {
//      client.executeMethod(method)
//      reply = CgiReplyBuilder.buildResponse(method.getResponseBodyAsStream, method.getRequestCharSet)
//      method.releaseConnection
//    } catch {
//      case ex: HorizonsException => {
//        throw ex
//      }
//      case ex: HttpException => {
//        throw HorizonsException.create(ex)
//      }
//      case ex: IOException => {
//        throw HorizonsException.create(ex)
//      }
//    }
//    return reply
//  }
//
//}


