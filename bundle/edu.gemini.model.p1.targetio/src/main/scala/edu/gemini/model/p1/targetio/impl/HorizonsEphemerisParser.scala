package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.targetio.api._
import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.immutable.CoordinatesEpoch.J_2000

import java.text.SimpleDateFormat

import edu.gemini.spModel.core.{Angle, Coordinates, Declination, RightAscension}

import util.parsing.combinator.JavaTokenParsers
import io.Source
import java.io.{InputStream, File}
import java.util.{UUID, TimeZone}

object HorizonsEphemerisParser extends JavaTokenParsers {
  override val whiteSpace = """[ \t]+""".r

  val NAME_TAG           = "Target body name: "
  val START_OF_EPHEMERIS = "$$SOE"
  val END_OF_EPHEMERIS   = "$$EOE"

  object Date {
    private val ERA_DATE_FORMAT = new SimpleDateFormat("G yyyy-MMM-dd h:mm") {
      setTimeZone(TimeZone.getTimeZone("UTC"))
    }
    private val DATE_FORMAT = new SimpleDateFormat("yyyy-MMM-dd h:mm") {
      setTimeZone(TimeZone.getTimeZone("UTC"))
    }

    def parse(utc: String): Option[Long] = synchronized {
      try {
        Some((if (utc.startsWith("b"))
          ERA_DATE_FORMAT.parse("BC " + utc.substring(1))
        else
          DATE_FORMAT.parse(utc)).getTime)
      } catch {
        case ex: java.text.ParseException => None
      }
    }

    def format(timestamp: Long): String = synchronized {
      ERA_DATE_FORMAT.format(new java.util.Date(timestamp))
    }
  }

  def eol      = """\r?\n""".r
  def any      = """[^\r\n]*""".r
  def chaff    = any~eol

  def nameText = """[^{]+""".r ^^ { _.trim }
  def endName  = "{" ~ chaff
  def name     = NAME_TAG~>nameText<~endName

  def headerLine    = not(NAME_TAG | START_OF_EPHEMERIS | END_OF_EPHEMERIS)~>chaff
  def headerSection = rep(headerLine)

  val UTC_EX = """b?\d+-[a-zA-Z]{3}-\d{1,2}\s+\d{1,2}:\d{1,2}""".r
  val parseTimeString: PartialFunction[String,Long] = {
    case s if Date.parse(s).isDefined => Date.parse(s).get
  }

  def wholeNumberPlus: Parser[String] =
    """[-+]?\d+""".r ^^ { s => if (s.startsWith("+")) s.substring(1) else s }

  def utc: Parser[Long] = UTC_EX.^?(parseTimeString, s => "Couldn't parse UTC time value '%s'".format(s))

  def sign: Parser[String] = """[-+]?""".r

  def toRa(sign: String, h: Int, m: Int, s: Double):Option[RightAscension] = {
    val sig = sign match {
      case "-" => -1
      case _   => 1
    }
    Angle.fromHMS(sig * h, m, s).map(RightAscension.fromAngle)
  }
  def toDec(sign: String, d: Int, m: Int, s: Double):Option[Declination] = {
    sign match {
      case "-" => Angle.fromDMS(d, m, s).flatMap(x => Declination.fromAngle(Angle.zero - x))
      case _   => Angle.fromDMS(d, m, s).flatMap(Declination.fromAngle)
    }
  }

  def ra: Parser[Option[RightAscension]] = coord(toRa)
  def dec: Parser[Option[Declination]]   = coord(toDec)

  def coords: Parser[Coordinates] = ra~dec ^? {
    case Some(r)~Some(d) => Coordinates(r, d)
  }

  def mag: Parser[Double]    = decimalNumber ^^ { _.toDouble }

  private def coord[T](f: (String, Int, Int, Double) => Option[T]): Parser[Option[T]] =
    sign~wholeNumber~wholeNumber~decimalNumber ^^ { case sn~h~m~s => f(sn, h.toInt, m.toInt, s.toDouble) }

  def element: Parser[EphemerisElement] = utc~coords~mag ^^ {
    case d~c~m => EphemerisElement(c, Some(m), d)
  }

  def elementLine: Parser[EphemerisElement] = element<~chaff
  def ephemeris: Parser[List[EphemerisElement]] = rep(elementLine)

  def soeLine = START_OF_EPHEMERIS~eol
  def eoeLine = END_OF_EPHEMERIS~eol

  def ephemerisSection: Parser[List[EphemerisElement]] = soeLine~>ephemeris<~eoeLine

  def header: Parser[String] = headerSection~>name<~headerSection

  def target: Parser[NonSiderealTarget] = header~ephemerisSection ^^ {
    case n~e => NonSiderealTarget(UUID.randomUUID(), n, e, J_2000, None, None)
  }

  def read(file: File): Either[TargetIoError, NonSiderealTarget] =
    read(Source.fromFile(file))

  def read(data: String): Either[TargetIoError, NonSiderealTarget] =
    read(Source.fromString(data))

  def read(is: InputStream): Either[TargetIoError, NonSiderealTarget] =
    read(Source.fromInputStream(is))

  private def read(src: Source): Either[TargetIoError, NonSiderealTarget] =
    try {
      val s = src.getLines().mkString("\n")
      parse(target, s) match {
        case Success(t, _)        => Right(t)
        case NoSuccess(msg, next) => Left(DataSourceError("Problem parsing ephemeris file on line %d: %s".format(next.pos.line, msg)))
      }
    } catch {
      case ex: Exception => Left(DataSourceError("Problem reading input: " + ex.nonNullMessage))
    } finally {
      src.close()
    }
}