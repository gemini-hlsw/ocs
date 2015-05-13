package edu.gemini.spModel.io.impl.migration.to2015B

import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.shared.skyobject.Magnitude.System

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
    """\s*(nucleus at|at|@|/|:)?\s*""".r ^^^ { () }

  private val num: Parser[BigDecimal] =
    """-?\d+(\.\d*)?""".r <~ """\s*(mag)?""".r ^^ (s => BigDecimal(new java.math.BigDecimal(s))) // BigDecimal.exact(s)

  private val band: Parser[Band] = {
    def bandParser(b: Band): Parser[Band] = {
      val p = b.toString.r <~ """\s*(_mag|mag|-band|band)?""".r ^^^ b
      p | paren(p) | paren(p, '<', '>')
    }
    Band.values.map(bandParser).foldRight[Parser[Band]](failure("expected band"))(_ | _)
  }

  private case class Sys(s: System, scale: BigDecimal = BigDecimal(1, 0)) {
    def toMag(b: Band, m: BigDecimal): Magnitude = new Magnitude(b, (m * scale).doubleValue(), s)
  }

  private val Jy   = Sys(System.Jy)
  private val mJy  = Sys(System.Jy, BigDecimal(1, 3))  // 1 mJy = 0.001 Jy
  private val AB   = Sys(System.AB)
  private val Vega = Sys(System.Vega)

  private val sys:  Parser[Sys] =
    "Jy"  ^^^ Jy  |
    "mJy" ^^^ mJy |
    "AB"  ^^^ AB  |
    success(Vega)

  private def paren[A](p: Parser[A], bra: Char = '(', ket: Char = ')'): Parser[A] =
    (bra ~> p) <~ ket

  private val mag: Parser[Magnitude] =
    (band <~ equ) ~ num ~ (ows ~> sys)               ^^ { case b ~ m ~ s   => s.toMag(b, m) } |  // Band = Num Sys
    (band <~ paren(num) <~ equ) ~ num ~ (ows ~> sys) ^^ { case b ~ m ~ s   => s.toMag(b, m) } |  // Band(nnn) = Num Sys
     band ~ paren(sys) ~ (equ ~> num)                ^^ { case b ~ s ~ m   => s.toMag(b, m) } |  // Band(Sys)=Num
     band ~ ('_' ~> sys) ~ (equ ~> num)              ^^ { case b ~ s ~ m   => s.toMag(b, m) } |  // Band_Sys=Num
    (num ~ sys <~ at) ~ band                         ^^ { case m ~ s ~ b   => s.toMag(b, m) } |  // Num Sys @ Band
     num ~ paren(band ~ sys)                         ^^ { case m ~ (b ~ s) => s.toMag(b, m) } |  // Num(Band Sys)
     num ~ band                                      ^^ { case m ~ b       => Vega.toMag(b, m) } // Num Band

  private val mags: Parser[List[Magnitude]] =
    rep1sep(mag, """\s*,\s*""".r)

  /** Parse a string into a non-empty list of Magnitude, if possible. */
  def parseBrightness(s: String): Option[NonEmptyList[Magnitude]] =
    parseAll(mags, s.trim).map(_.toNel).getOrElse(None)

}
