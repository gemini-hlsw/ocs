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

  private val num: Parser[Double] =
    """-?\d+(\.\d*)?""".r <~ """\s*(mag)?""".r ^^ (_.toDouble)

  private val band: Parser[Band] = {
    def bandParser(b: Band): Parser[Band] = {
      val p = ("(?i)" + b.toString).r <~ """\s*(_mag|mag|-band|band)?""".r ^^^ b
      p | paren(p) | paren(p, '<', '>')
    }
    Band.values.map(bandParser).foldRight[Parser[Band]](failure("expected band"))(_ | _)
  }

  private val sys:  Parser[System] =
    "(mJy|Jy)".r ^^^ System.Jy |
    "AB"         ^^^ System.AB |
    success(System.Vega)

  private def paren[A](p: Parser[A], bra: Char = '(', ket: Char = ')'): Parser[A] =
    (bra ~> p) <~ ket

  private val mag: Parser[Magnitude] =
    (band <~ equ) ~ num ~ (ows ~> sys)               ^^ { case b ~ m ~s    => new Magnitude(b, m, s) } | // Band = Num Sys
    (band <~ paren(num) <~ equ) ~ num ~ (ows ~> sys) ^^ { case b ~ m ~s    => new Magnitude(b, m, s) } | // Band(nnn) = Num Sys
     band ~ paren(sys) ~ (equ ~> num)                ^^ { case b ~ s ~ m   => new Magnitude(b, m, s) } | // Band(Sys)=Num
     band ~ ('_' ~> sys) ~ (equ ~> num)              ^^ { case b ~ s ~ m   => new Magnitude(b, m, s) } | // Band_Sys=Num
    (num ~ sys <~ at) ~ band                         ^^ { case m ~ s ~ b   => new Magnitude(b, m, s) } | // Num Sys @ Band
     num ~ paren(band ~ sys)                         ^^ { case m ~ (b ~ s) => new Magnitude(b, m, s) } | // Num(Band Sys)
     num ~ band                                      ^^ { case m ~ b       => new Magnitude(b, m) }      // Num Band

  private val mags: Parser[List[Magnitude]] =
    rep1sep(mag, """\s*,\s*""".r)

  /** Parse a string into a non-empty list of Magnitude, if possible. */
  def parseBrightness(s: String): Option[NonEmptyList[Magnitude]] =
    parseAll(mags, s.trim).map(_.toNel).getOrElse(None)

}
