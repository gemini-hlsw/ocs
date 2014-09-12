package edu.gemini.gsa.client.impl

import edu.gemini.gsa.client.api.{GsaNonSiderealParams, GsaSiderealParams, GsaParams}
import edu.gemini.model.p1.immutable.Coordinates
import java.net.{URL, URLEncoder}

object GsaUrl {
  val ROOT = "http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/cadcbin/gsa/wdbi.cgi/gsa/gsa_science/query"

  /**
   * Converts the GsaParams into a URL that can be used to query the GSA.
   */
  def apply(params: GsaParams): URL = {
    val args = formatParams(params)  map {
      case (name, value) => "%s=%s".format(name, URLEncoder.encode(value, "UTF-8"))
    }
    new URL("%s?%s".format(ROOT, args.mkString("&")))
  }

  sealed trait Param {
    def name: String
    def format(p: GsaParams): Option[String]
  }

  case class FixedParam(name: String, value: String) extends Param {
    def format(p: GsaParams) = Some(value)
  }

  case class SiderealFixedParam(name: String, value: String) extends Param {
    def format(p: GsaParams) = p match {
      case _: GsaSiderealParams => Some(value)
      case _ => None
    }
  }

  case class NonSiderealFixedParam(name: String, value: String) extends Param {
    def format(p: GsaParams) = p match {
      case _: GsaNonSiderealParams => Some(value)
      case _ => None
    }
  }

  case object INST_PARAM extends Param {
    def name = "instrument"
    def format(p: GsaParams) = Some(p.instrument.gsa)
  }

  case object LIMIT_PARAM extends Param {
    def name = "max_rows_returned"
    def format(p: GsaParams) = Some(p.limit.toString)
  }

  abstract class CoordParam(f: Coordinates => String) extends Param {
    def format(p: GsaParams): Option[String] =
      p match {
        case s: GsaSiderealParams => Some(f(s.coords).replace(':', ' '))
        case _ => None
      }
  }
  case object RA_PARAM extends CoordParam(_.toHmsDms.ra.toString) {
    def name = "ra2000"
  }
  case object DEC_PARAM extends CoordParam(_.toHmsDms.dec.toString) {
    def name = "dec2000"
  }

  case object TARGET_NAME_PARAM extends Param {
    def name="tobject"
    private def allcase(s: String): List[String] = {
      val res = List(s.toUpperCase, s.toLowerCase)
      if (res.contains(s)) res else s :: res
    }
    def format(p: GsaParams): Option[String] =
      p match {
        case n: GsaNonSiderealParams => Some(allcase(n.targetName).mkString("==", " | ==", ""))
        case _ => None
      }
  }

  val PARAMS = List(
    INST_PARAM,
    LIMIT_PARAM,
    RA_PARAM,
    DEC_PARAM,
    TARGET_NAME_PARAM,
    FixedParam("wdbi_order", "ut_date_time desc"),
    FixedParam("tab_mode", "yes"),
    SiderealFixedParam("box", "00 01 00"),
    NonSiderealFixedParam("simbad", "none")
  )

  private def formatParams(params: GsaParams): List[(String, String)] =
    for {
      (name, Some(value)) <- PARAMS.map(p => p.name -> p.format(params))
    } yield name -> value

}
