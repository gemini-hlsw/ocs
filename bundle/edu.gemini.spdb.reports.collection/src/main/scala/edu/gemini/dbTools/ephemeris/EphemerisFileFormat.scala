package edu.gemini.dbTools.ephemeris

import edu.gemini.skycalc.JulianDate
import edu.gemini.spModel.core.{RightAscension, Angle, Coordinates, Declination}

import java.time.Instant
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.time.temporal.{TemporalAccessor, TemporalQuery}
import java.util.Locale.US

import scala.util.parsing.combinator.JavaTokenParsers
import scalaz._, Scalaz._

/** Support for formatting and parsing TCS ephemeris files. */
object EphemerisFileFormat {

  /** Header required by the TCS. */
  val Header =
    """***************************************************************************************
      | Date__(UT)__HR:MN Date_________JDUT     R.A.___(ICRF/J2000.0)___DEC dRA*cosD d(DEC)/dt
      |***************************************************************************************""".stripMargin

  /** Marker for the start of ephemeris elements in the file. */
  val SOE = "$$SOE"

  /** Marker for the end of ephemeris elements in the file. */
  val EOE = "$$EOE"

  object Time {
    // Text date format
    val DateFormatString = "yyyy-MMM-dd HH:mm"

    // Regular expression that extracts the time string in the `DateFormatString`
    // format from a line in an ephemeris file.
    val TimeRegex        = """(\d{4}-[a-zA-Z]{3}-\d{1,2}\s+\d{1,2}:\d{1,2})""".r

    val DateFormat       = DateTimeFormatter.ofPattern(DateFormatString, US).withZone(UTC)

    def format(time: Instant): String =
      DateFormat.format(time)

    def parse(s: String): Throwable \/ Instant =
      \/.fromTryCatchNonFatal {
        DateFormat.parse(s, new TemporalQuery[Instant] () {
          override def queryFrom(temporal: TemporalAccessor): Instant =
            Instant.from(temporal)
         }
        )
      }
  }

  def format(ephemeris: EphemerisMap): String = {
    def formatCoords(coords: Coordinates): String = {
      val ra  = Angle.formatHMS(coords.ra.toAngle, " ", 4)
      val dec = Declination.formatDMS(coords.dec, " ", 3)
      // Add spacing as required for TCS.
      f"$ra%14s $dec%13s"
    }

    val lines = ephemeris.toList.map { case (time, (coords, raTrack, decTrack)) =>
      val timeS     = Time.format(time)
      val jdS       = f"${new JulianDate(time.toEpochMilli).toDouble}%.9f"
      val coordsS   = formatCoords(coords)
      val raTrackS  = f"$raTrack%9.5f"
      val decTrackS = f"$decTrack%9.5f"
      s" $timeS $jdS    $coordsS $raTrackS $decTrackS"
    }

    lines.mkString(s"$Header\n$SOE\n", "\n", (lines.isEmpty ? "" | "\n") + s"$EOE\n")
  }

  object Parser extends JavaTokenParsers {
    override val whiteSpace = """[ \t]+""".r

    private val eol: Parser[Any]     = """(\r?\n)""".r
    private val soeLine: Parser[Any] = SOE~eol
    private val eoeLine: Parser[Any] = EOE
    private val chaff: Parser[Any]   = """.*""".r ~ eol

    def headerLine    = not(SOE | EOE)~>chaff
    def headerSection = rep(headerLine)

    private val utc: Parser[Instant] =
      Time.TimeRegex >> { timeString =>
        new Parser[Instant]() {
          def apply(in: Input): ParseResult[Instant] =
            Time.parse(timeString) match {
              case -\/(ex)   => Failure(s"Could not parse `$timeString` as a time", in)
              case \/-(time) => Success(time, in)
            }
        }
      }

    private val sign: Parser[String] =
      """[-+]?""".r

    private val signedDecimal: Parser[Double] =
      sign~decimalNumber ^^ {
        case "-"~d => -d.toDouble
        case s~d   =>  d.toDouble
      }

    val raTrack: Parser[Double]=
      signedDecimal

    private val decTrack: Parser[Double] =
      signedDecimal

    private def coord[T](f: (String, Int, Int, Double) => Option[T]): Parser[Option[T]] =
      sign~wholeNumber~wholeNumber~decimalNumber ^^ { case sn~h~m~s => f(sn, h.toInt, m.toInt, s.toDouble) }

    private def toRa(sign: String, h: Int, m: Int, s: Double): Option[RightAscension] =
      Angle.parseHMS(s"$sign$h:$m:$s").toOption.map(RightAscension.fromAngle)

    private def toDec(sign: String, d: Int, m: Int, s: Double): Option[Declination] =
      Angle.parseDMS(s"$sign$d:$m:$s").toOption.flatMap(Declination.fromAngle)

    private def ra: Parser[Option[RightAscension]] =
      coord(toRa)

    private def dec: Parser[Option[Declination]] =
      coord(toDec)

    private val coords: Parser[Coordinates] =
      ra~dec ^? {
        case Some(r)~Some(d) => Coordinates(r, d)
      }

    private val ephemerisLine: Parser[(Instant, EphemerisElement)] =
      utc~decimalNumber~coords~raTrack~decTrack<~eol ^^ {
        case u~ignore~c~r~d => (u, (c, r, d))
      }

    private val ephemerisList: Parser[List[(Instant, EphemerisElement)]] =
      rep(ephemerisLine)

    val ephemerisSection: Parser[EphemerisMap] =
      (soeLine~>ephemerisList<~eoeLine).map { lines => ==>>.fromList(lines) }

    val ephemeris: Parser[EphemerisMap] =
      headerSection~>ephemerisSection

    private val timestamp: Parser[Instant] =
      utc<~chaff

    private val timestampList: Parser[ISet[Instant]] =
      rep(timestamp).map(lst => ISet.fromList(lst))

    val timestampsSection: Parser[ISet[Instant]] =
      soeLine~>timestampList<~eoeLine

    val timestamps: Parser[ISet[Instant]] =
      headerSection~>timestampsSection

    def toDisjunction[A](p: Parser[A], input: String): String \/ A =
      parse(p, input) match {
        case Success(a, _)     => a.right
        case NoSuccess(msg, _) => msg.left
      }
  }

  def parse(input: String): String \/ EphemerisMap =
    Parser.toDisjunction(Parser.ephemeris, input)

  def parseTimestamps(input: String): String \/ ISet[Instant] =
    Parser.toDisjunction(Parser.timestamps, input)
}
