package edu.gemini.spModel.io.impl.migration.to2015B

import edu.gemini.spModel.core.{Magnitude, MagnitudeBand, MagnitudeSystem}

import scala.util.parsing.combinator.RegexParsers
import scalaz._, Scalaz._

object BrightnessParser extends RegexParsers {

  // N.B. this is not an elegant parser; it is hacked to parse the 50 most common pre-2010B
  // "brightness" values found in the database. Examples can be found in the testcase.

  private val ows: Parser[Unit] =
    """\s*""".r ^^^ { () }

  private val equ: Parser[Unit] =
    """\s*[~=,]?\s*""".r ^^^ { () }

  private val at: Parser[Unit] =
    """\s*(nucleus at|at|in|@|/|:)?\s*""".r ^^^ { () }

  private val num: Parser[BigDecimal] =
    """-?\d+(\.\d*)?""".r <~ """\s*(mag)?""".r ^^ (s => BigDecimal(new java.math.BigDecimal(s))) // BigDecimal.exact(s)

  private val band: Parser[MagnitudeBand] = {
    def bandParser(b: MagnitudeBand): Parser[MagnitudeBand] = {
      val p = (b.name + """'?\s*(_mag|mag|-band|band)?""").r ^^^ b
      p | paren(p) | paren(p, '<', '>')
    }

    MagnitudeBand.all.map(bandParser).foldRight[Parser[MagnitudeBand]](failure("expected band"))(_ ||| _)
  }

  private case class Sys(s: MagnitudeSystem, scale: BigDecimal = BigDecimal(1, 0)) {
    def toMag(b: MagnitudeBand, m: BigDecimal): Magnitude = new Magnitude((m * scale).doubleValue, b, s)
  }

  private val Jy     = Sys(MagnitudeSystem.Jy)
  private val mJy    = Sys(MagnitudeSystem.Jy, BigDecimal(1, 3))  // 1 mJy = 0.001 Jy
  private val AB     = Sys(MagnitudeSystem.AB)
  private val Vega   = Sys(MagnitudeSystem.Vega)

  private val AllSys = List(Jy, mJy, AB, Vega)

  private val sys: Parser[Option[Sys]] =
    "Jy"   ^^^ Some(Jy)   |
    "mJy"  ^^^ Some(mJy)  |
    "AB"   ^^^ Some(AB)   |
    "Vega" ^^^ Some(Vega) |
    success(None)

  private def paren[A](p: Parser[A], bra: Char = '(', ket: Char = ')'): Parser[A] =
    (bra ~> p) <~ ket

  private def mkMag(s: Option[Sys], b: MagnitudeBand, m: BigDecimal): Magnitude = {
    def defaultSys(b: MagnitudeBand): Sys = AllSys.find(_.s == b.defaultSystem) | Vega

    (s | defaultSys(b)).toMag(b, m)
  }

  private val mag: Parser[Magnitude] =
    (band <~ equ) ~ num ~ (ows ~> sys)               ^^ { case b ~ m ~ s   => mkMag(s, b, m) } |||  // Band = Num Sys
    (band <~ paren(num) <~ equ) ~ num ~ (ows ~> sys) ^^ { case b ~ m ~ s   => mkMag(s, b, m) } |||  // Band(nnn) = Num Sys
     band ~ paren(sys) ~ (equ ~> num)                ^^ { case b ~ s ~ m   => mkMag(s, b, m) } |||  // Band(Sys)=Num
     band ~ ('_' ~> sys) ~ (equ ~> num)              ^^ { case b ~ s ~ m   => mkMag(s, b, m) } |||  // Band_Sys=Num
    (num ~ sys <~ at) ~ band                         ^^ { case m ~ s ~ b   => mkMag(s, b, m) } |||  // Num Sys @ Band
     num ~ band ~ sys                                ^^ { case m ~ b ~ s   => mkMag(s, b, m) } |||  // Num Band Sys
     num ~ band ~ paren(sys)                         ^^ { case m ~ b ~ s   => mkMag(s, b, m) } |||  // Num Band (Sys)
     num ~ paren(band ~ sys)                         ^^ { case m ~ (b ~ s) => mkMag(s, b, m) } |||  // Num(Band Sys)
     num ~ band                                      ^^ { case m ~ b       => mkMag(None, b, m) }   // Num Band

  private val mags: Parser[List[Magnitude]] =
    rep1sep(mag, """\s*,\s*""".r)

  /** Parse a string into a non-empty list of Magnitude, if possible. */
  def parseBrightness(s: String): Option[NonEmptyList[Magnitude]] =
    parseAll(mags, s.trim).map(_.toNel).getOrElse(None)

}
