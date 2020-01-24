package edu.gemini.model.p1.targetio.table

import edu.gemini.spModel.core._
import scalaz._
import Scalaz._

object Readers {
  def readNone[T]: PartialFunction[Any, Option[T]] = {
    case null     => None
    case "INDEF"  => None
    case ""       => None
  }

  val readOptionalString: PartialFunction[Any, Option[String]] =
    readNone orElse { case s: String => Some(s) }

  val readString: PartialFunction[Any, String] = {
    case s: String => s.toString
  }

  val readNan: PartialFunction[Any, Option[Double]] = {
    case d: Double if d.isNaN => None
    case f: Float  if f.isNaN => None
  }

  val WholeNumber = """([-+]?\d+)""".r
  val readInt: PartialFunction[Any, Int] = {
    case s: String if WholeNumber.findFirstIn(s).isDefined  =>  s.parseInt.getOrElse(sys.error(s"Not an int $s"))
    case b: Byte   => b.toInt
    case s: Short  => s.toInt
    case i: Int    => i
  }

  val DecimalNumber = """([-+]?\d+(?:\.\d*)?)""".r
  val readFloating: PartialFunction[Any, Double] = {
    case s: String if DecimalNumber.findFirstIn(s).isDefined  =>  s.parseDouble.getOrElse(sys.error(s"Not a double $s"))
    case d: Double        => d
    case f: Float         => f.toDouble
  }

  val readDouble = readFloating orElse (readInt andThen { _.toDouble })

  val readOptionalDouble: PartialFunction[Any, Option[Double]] =
    readNone orElse readNan orElse (readDouble andThen { d => Some(d) })

  def readSexigesimal[T](f: String => T): PartialFunction[Any, T] = {
    case s: String if s.contains(":") => f(s)
  }

  def readDegrees[T](f: Double => T): PartialFunction[Any, T] = readDouble andThen f
  def parseDegrees(d: Double): Option[Angle] = Some(Angle.fromDegrees(d))

  private def parseDMS(s: String): Option[Angle] = Angle.parseDMS(s).toOption
  private def parseHMS(s: String): Option[Angle] = Angle.parseHMS(s).toOption

  val readRa: PartialFunction[Any, Option[RightAscension]] = (readSexigesimal(parseHMS) orElse readDegrees(parseDegrees)) andThen (a => a.map(RightAscension.fromAngle))
  val readDec: PartialFunction[Any, Option[Declination]] = (readSexigesimal(parseDMS) orElse readDegrees(parseDegrees)) andThen (a => a.flatMap(Declination.fromAngle))

  def readOptionalMagnitude(band: MagnitudeBand): PartialFunction[Any, Option[Magnitude]] =
    readOptionalDouble andThen { _.map(new Magnitude(_, band)) }

  private def matches(name: String, sys: MagnitudeSystem) = sys.name.equalsIgnoreCase(name)
  val readSystem: PartialFunction[Any, MagnitudeSystem] = {
     case s: String if MagnitudeSystem.allForOT.exists(matches(s, _)) =>
       MagnitudeSystem.allForOT.find(matches(s, _)).get
  }

  def readOptionalSystem(band: MagnitudeBand): PartialFunction[Any, Option[MagnitudeSystem]] =
    readNone orElse (readSystem andThen { s => Some(s) })
}
