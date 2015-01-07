package edu.gemini.catalog.api

import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core.{MagnitudeBand, Magnitude}

import scalaz._
import Scalaz._

/**
 * Interface for a constraint to be applied to a certain magnitude
 */
sealed trait MagConstraint {
  val brightness: Double
  def contains(m: Magnitude): Boolean

  /**
   * Constructs a magnitude out of the band parameter
   */
  def toMagnitude(band: MagnitudeBand): Magnitude = new Magnitude(brightness, band)
}

/**
 * Constrain a target's if a magnitude is fainter than a threshold
 */
case class FaintnessConstraint(brightness: Double) extends MagConstraint {
  override def contains(m: Magnitude) = m.value <= brightness
}

object FaintnessConstraint {
  /** @group Typeclass Instances */
  implicit val order: Order[FaintnessConstraint] =
    Order.orderBy(_.brightness)
}

/**
 * Constrain a target's if a magnitude is brighter than a threshold
 */
case class SaturationConstraint(brightness: Double) extends MagConstraint {
  override def contains(m: Magnitude) = m.value >= brightness
}

object SaturationConstraint {
  /** @group Typeclass Instances */
  implicit val order: Order[SaturationConstraint] =
    Order.orderBy(_.brightness)
}

/**
 * Describes constraints for the magnitude of a target
 * See OT-19.
 */
case class MagnitudeConstraints(band: MagnitudeBand, faintnessConstraint: FaintnessConstraint, saturationConstraint: Option[SaturationConstraint]) {

  /**
   * Maps a transformation into a new MagnitudeConstraints
   */
  def map(f: Magnitude => Magnitude): MagnitudeConstraints = {
    val mappedFaintness = f(faintnessConstraint.toMagnitude(band)) 
    val mappedSaturation = saturationConstraint.map(_.toMagnitude(band)).map(f)

    val fl = FaintnessConstraint(mappedFaintness.value)
    val sl = mappedSaturation.map(s => SaturationConstraint(s.value))

    MagnitudeConstraints(mappedFaintness.band, fl, sl)
  }

  def filter: SiderealTarget => Boolean = t => t.magnitudeIn(band).exists(contains)

  /**
   * Determines whether the magnitude limits include the given magnitude
   * value.
   */
  def contains(m: Magnitude) = m.band === band && faintnessConstraint.contains(m) && saturationConstraint.forall(_.contains(m))

  /**
   * Returns a combination of two MagnitudeLimits (this and that) such that
   * the faintness limit is the faintest of the two and the saturation limit
   * is the brightest of the two.  In other words, the widest possible range
   * of magnitude bands.
   */
  def union(that: MagnitudeConstraints): Option[MagnitudeConstraints] =
    (band == that.band) option {
      val faintness = faintnessConstraint.max(that.faintnessConstraint)

      // Calculate the max out of the saturation limit if both are defined
      val saturation = (saturationConstraint |@| that.saturationConstraint)(_ max _)

      MagnitudeConstraints(band, faintness, saturation)
    }
}

object MagnitudeConstraints {
  def empty(band: MagnitudeBand): MagnitudeConstraints =
    MagnitudeConstraints(band, FaintnessConstraint(0.0), SaturationConstraint(0.0).some)
}
