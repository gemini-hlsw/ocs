package edu.gemini.model.p1.targetio.table

import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.{mutable => M}
import edu.gemini.spModel.core.{MagnitudeSystem, MagnitudeBand, Magnitude}

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
    case WholeNumber(s)  => s.toInt
    case b: Byte   => b.toInt
    case s: Short  => s.toInt
    case i: Int    => i
  }

  val DecimalNumber = """([-+]?\d+(?:\.\d*)?)""".r
  val readFloating: PartialFunction[Any, Double] = {
    case DecimalNumber(s) => s.toDouble
    case d: Double   => d
    case f: Float    => f.toDouble
  }

  val readDouble = readFloating orElse (readInt andThen { _.toDouble })

  val readOptionalDouble: PartialFunction[Any, Option[Double]] =
    readNone orElse readNan orElse (readDouble andThen { d => Some(d) })

  def readSexigesimal[T](f: String => T): PartialFunction[Any, T] = {
    case s: String if s.contains(":") => f(s)
  }

  def readDegrees[T](f: Double => T): PartialFunction[Any, T] = readDouble andThen { f(_) }

  val readRa: PartialFunction[Any, HMS]  = readSexigesimal(HMS(_)) orElse readDegrees(HMS(_))
  val readDec: PartialFunction[Any, DMS] = readSexigesimal(DMS(_)) orElse readDegrees(DMS(_))


  def readOptionalMagnitude(band: MagnitudeBand): PartialFunction[Any, Option[Magnitude]] =
    readOptionalDouble andThen { _.map(new Magnitude(_, band)) }

  private def matches(name: String, sys: MagnitudeSystem) = sys.name.equalsIgnoreCase(name)
  val readSystem: PartialFunction[Any, MagnitudeSystem] = {
     case s: String if MagnitudeSystem.all.exists(matches(s, _)) =>
       MagnitudeSystem.all.find(matches(s, _)).get
  }

  def readOptionalSystem(band: MagnitudeBand): PartialFunction[Any, Option[MagnitudeSystem]] =
    readNone orElse (readSystem andThen { s => Some(s) })
}




