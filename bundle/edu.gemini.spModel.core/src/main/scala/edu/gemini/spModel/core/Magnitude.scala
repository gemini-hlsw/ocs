package edu.gemini.spModel.core


case class Magnitude(value: Double, band: MagnitudeBand, error: Option[Double], system: MagnitudeSystem) {

  /** Secondary constructor defaulting to `MagnitudeSystem.VEGA`. */
  def this(value: Double, band: MagnitudeBand, error: Double) =
    this(value, band, Some(error), MagnitudeSystem.VEGA)

  def this(value: Double, band: MagnitudeBand, error: Double, system: MagnitudeSystem) =
    this(value, band, Some(error), MagnitudeSystem.VEGA)

  /** Secondary constructor defaulting to no error. */
  def this(value: Double, band: MagnitudeBand, system: MagnitudeSystem) =
    this(value, band, None, system)

  /** Secondary constructor defaulting to no given error and `MagnitudeSystem.VEGA`. */
  def this(value: Double, band: MagnitudeBand) =
    this(value, band, None, MagnitudeSystem.VEGA)

  def add(v: Double): Magnitude =
    copy(value = value + v)

}

object Magnitude {

  // by system name, band name, value and error (in that order)
  implicit val MagnitudeOrdering: scala.math.Ordering[Magnitude] =
    scala.math.Ordering.by(m => (m.system.name, m.band.name, m.value, m.error))

  /** @group Typeclass Instances */
  implicit val equals = scalaz.Equal.equalA[Magnitude]

}