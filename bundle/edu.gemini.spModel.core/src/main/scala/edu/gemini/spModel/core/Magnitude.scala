package edu.gemini.spModel.core

import scalaz._
import Scalaz._

case class Magnitude(value: Double, band: MagnitudeBand, error: Option[Double], system: MagnitudeSystem) {

  /** Secondary constructor. */
  def this(value: Double, band: MagnitudeBand, error: Double) =
    this(value, band, Some(error), band.defaultSystem)

  def this(value: Double, band: MagnitudeBand, error: Double, system: MagnitudeSystem) =
    this(value, band, Some(error), system)

  /** Secondary constructor defaulting to no error. */
  def this(value: Double, band: MagnitudeBand, system: MagnitudeSystem) =
    this(value, band, None, system)

  /** Secondary constructor defaulting to no given error. */
  def this(value: Double, band: MagnitudeBand) =
    this(value, band, None, band.defaultSystem)

  def add(v: Double): Magnitude =
    copy(value = value + v)

}

object Magnitude {

  // Lenses
  val value: Magnitude  @> Double          = Lens.lensu((a, b) => a.copy(value = b), _.value)
  val band:  Magnitude  @> MagnitudeBand   = Lens.lensu((a, b) => a.copy(band = b), _.band)
  val error: Magnitude  @> Option[Double]  = Lens.lensu((a, b) => a.copy(error = b), _.error)
  val system: Magnitude @> MagnitudeSystem = Lens.lensu((a, b) => a.copy(system = b), _.system)

  // by system name, band name, value and error (in that order)
  implicit val MagnitudeOrdering: scala.math.Ordering[Magnitude] =
    scala.math.Ordering.by(m => (m.system.name, m.band.name, m.value, m.error))

  private def compareOptionWith(
    x: Option[Magnitude],
    y: Option[Magnitude],
    c: scala.math.Ordering[Magnitude]
  ): Int =
    (x,y) match {
      case (Some(m1), Some(m2)) => c.compare(m1, m2)
      case (None,     None)     =>  0
      case (_,        None)     => -1
      case (None,     _)        =>  1
    }

  // comparison on Option[Magnitude] that reverses the way that None is treated, i.e. None is always > Some(Magnitude).
  implicit val MagnitudeOptionOrdering: scala.math.Ordering[Option[Magnitude]] =
    new scala.math.Ordering[Option[Magnitude]] {
      override def compare(x: Option[Magnitude], y: Option[Magnitude]): Int =
        compareOptionWith(x, y, MagnitudeOrdering)
    }


  // Sort by value only. not implicit.  In general different bands are not
  // comparable, but all "R-like" bands can be compared.
  val MagnitudeValueOrdering: scala.math.Ordering[Magnitude] =
    scala.math.Ordering.by(_.value)

  val MagnitudeOptionValueOrdering: scala.math.Ordering[Option[Magnitude]] =
    new scala.math.Ordering[Option[Magnitude]] {
      override def compare(x: Option[Magnitude], y: Option[Magnitude]): Int =
        compareOptionWith(x, y, MagnitudeValueOrdering)
    }

  /** @group Typeclass Instances */
  implicit val equals = Equal.equalA[Magnitude]

  /** group Typeclass Instances */
  implicit val MagnitudeShow: Show[Magnitude] =
    scalaz.Show.shows { mag =>
      val errStr = mag.error.map { e => f" e$e%.2f " } | " "
      f"${mag.band.name}${mag.value}%.2f$errStr(${mag.system.name})"
    }
}