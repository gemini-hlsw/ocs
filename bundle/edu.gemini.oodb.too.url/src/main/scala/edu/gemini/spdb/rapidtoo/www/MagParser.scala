package edu.gemini.spdb.rapidtoo.www

import edu.gemini.shared.util.immutable.ImList
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.{Magnitude, MagnitudeBand, MagnitudeSystem}

import scala.util.parsing.combinator.RegexParsers

/** Magnitude parser; instantiate for use from Java. */
class MagParser extends RegexParsers {

  val num: Parser[Double] =
    """-?\d+(\.\d*)?""".r ^^ (_.toDouble)

  val band: Parser[MagnitudeBand] =
    MagnitudeBand.all.map(b => b.toString ^^^ b).reverse.reduceLeft(_ | _) // reverse to put UC ahead of U

  val sys:  Parser[MagnitudeSystem] =
    MagnitudeSystem.allForOT.map(s => s.toString ^^^ s).reduceLeft(_ | _)

  val mag: Parser[Magnitude] =
    (num <~ '/') ~ (band <~ '/') ~ sys ^^ { case n ~ b ~ s => new Magnitude(n, b, s) }

  val mags: Parser[ImList[Magnitude]] =
    repsep(mag, ',').map(_.asImList)

  def unsafeParse(s: String): ImList[Magnitude] =
    parseAll(mags, s).getOrElse(throw new BadRequestException("invalid magnitude list: " + s))

}
