package edu.gemini.dbTools.ephemeris

import edu.gemini.skycalc.{JulianDate, TwilightBoundedNight}
import edu.gemini.skycalc.TwilightBoundType.NAUTICAL
import edu.gemini.spModel.core.HorizonsDesignation.MajorBody
import edu.gemini.spModel.core.{Angle, RightAscension, Declination, Coordinates, Site}

import java.nio.file.{Path, Files}
import java.security.Principal
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.{Locale, TimeZone}
import java.util.function.Consumer
import java.util.logging.Logger

import scala.collection.JavaConverters._
import scala.util.Random
import scala.util.parsing.combinator._
import scalaz._, Scalaz._

object TcsEphemerisExportTest extends TestSupport {
  val log   = Logger.getLogger(TcsEphemerisExport.getClass.getName)
  val site  = Site.GS
  val night = TwilightBoundedNight.forTime(NAUTICAL, Instant.now.toEpochMilli, site)

  def withTempDir(f: Path => Boolean): Boolean = {
    val dir = Files.createTempDirectory(s"tcsEphemerisExportTest-${Random.nextLong()}")
    try {
      f(dir)
    } finally {
      Files.list(dir).forEach(new Consumer[Path] {
        override def accept(t: Path): Unit = Files.delete(t)
      })
      Files.delete(dir)
    }
  }

  case class EpElem(time: Long, julian: JulianDate, coords: Coordinates, raTrack: Double, decTrack: Double)

  object Parser extends JavaTokenParsers {
    override val whiteSpace = """[ \t]+""".r

    val SOE: Parser[String] = "$$SOE"
    val EOE: Parser[String] = "$$EOE"

    object Date {
      val Format = new SimpleDateFormat(TcsEphemerisExport.DateFormatString, Locale.US) {
        setTimeZone(TimeZone.getTimeZone("UTC"))
      }
      def parse(utc: String): Long = synchronized {
        Format.parse(utc).getTime
      }
    }

    val eol: Parser[Any]     = """(\r?\n)""".r
    val soeLine: Parser[Any] = SOE~eol
    val eoeLine: Parser[Any] = EOE

    val UtcEx = """\d{4}-[a-zA-Z]{3}-\d{1,2}\s+\d{1,2}:\d{1,2}""".r
    val utc: Parser[Long]             = UtcEx ^^ { x => Date.parse(x) }
    val julian: Parser[JulianDate]    = decimalNumber ^^ { dn => new JulianDate(dn.toDouble) }

    val sign: Parser[String]          = """[-+]?""".r
    val signedDecimal: Parser[Double] = sign~decimalNumber ^^ {
      case "-"~d => -d.toDouble
      case s~d   => d.toDouble
    }

    val raTrack: Parser[Double]     = signedDecimal
    val decTrack: Parser[Double]    = signedDecimal

    def coord[T](f: (String, Int, Int, Double) => Option[T]): Parser[Option[T]] =
      sign~wholeNumber~wholeNumber~decimalNumber ^^ { case sn~h~m~s => f(sn, h.toInt, m.toInt, s.toDouble) }

    def toRa(sign: String, h: Int, m: Int, s: Double): Option[RightAscension] =
      Angle.parseHMS(s"$sign$h:$m:$s").toOption.map(RightAscension.fromAngle)

    def toDec(sign: String, d: Int, m: Int, s: Double): Option[Declination] =
      Angle.parseDMS(s"$sign$d:$m:$s").toOption.flatMap(Declination.fromAngle)

    def ra: Parser[Option[RightAscension]] = coord(toRa)
    def dec: Parser[Option[Declination]]   = coord(toDec)

    val coords: Parser[Coordinates] = ra~dec ^? {
      case Some(r)~Some(d) => Coordinates(r, d)
    }

    val ephemerisLine: Parser[EpElem]       = utc~julian~coords~raTrack~decTrack<~eol ^^ {
      case u~j~c~r~d => EpElem(u, j, c, r, d)
    }
    val ephemerisList: Parser[List[EpElem]] = rep1(ephemerisLine)

    val ephemeris: Parser[List[EpElem]] = soeLine~>ephemerisList<~eoeLine

    private def dump(input: String): Unit =
      println(input.split("\n").zipWithIndex.map(_.swap).map{
        case (num, s) => f"${num+1}%3d $s"
      }.mkString("\n"))

    def read(file: Path): String \/ List[EpElem] = {
      val lines = Files.readAllLines(file)
      val input = lines.asScala.mkString("\n")

      parse(ephemeris, input) match {
        case Success(l, _)     =>
          val expected = lines.size - 2  // removing 2 for SOE and EOE
          if (l.size == expected) l.right
          else s"Expected $expected results, but found ${l.size}".left
        case NoSuccess(msg, n) =>
          s"$msg: line=${n.pos.line}, col=${n.pos.column}".left
      }
    }
  }

  "TcsEphemerisExport" should {
    "export parseable ephemeris files" in {
      withTempDir { dir =>
        val exp = new TcsEphemerisExport(dir.toFile, night, site, null, java.util.Collections.emptySet[Principal])
        exp.exportOne(dir.toFile, MajorBody(606), "Titan").run.unsafePerformIO() match {
          case -\/(err)  =>
            TcsEphemerisExport.ExportError.logError(err, log, "")
            false
          case \/-(file) =>
            Parser.read(file.toPath) match {
              case -\/(msg) =>
                sys.error(msg)
                false
              case \/-(_)   =>
                true
            }
        }
      }
    }
  }

}
