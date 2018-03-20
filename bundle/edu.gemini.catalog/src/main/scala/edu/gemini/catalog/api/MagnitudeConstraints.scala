package edu.gemini.catalog.api

import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.core.{BandsList, Magnitude, MagnitudeBand}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.MagnitudeAdjuster

import scalaz._
import Scalaz._

/**
 * Interface for a constraint to be applied to a certain magnitude
 */
sealed trait MagConstraint {
  val brightness: Double
  def contains(v: Double): Boolean

  /**
   * Constructs a magnitude out of the band parameter
   */
  def toMagnitude(band: MagnitudeBand): Magnitude = new Magnitude(brightness, band)
}

/**
 * Constrain a target's if a magnitude is fainter than a threshold
 */
case class FaintnessConstraint(brightness: Double) extends MagConstraint {
  override def contains(v: Double) = v <= brightness
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
  override def contains(v: Double) = v >= brightness
}

object SaturationConstraint {
  /** @group Typeclass Instances */
  implicit val order: Order[SaturationConstraint] =
    Order.orderBy(_.brightness)
}

sealed trait MagnitudeFilter {
  def filter(t: SiderealTarget): Boolean

  def searchBands: BandsList
  def faintnessConstraint: FaintnessConstraint
  def saturationConstraint: Option[SaturationConstraint]
}

/**
 * Defines a typeclass of classes that can adjust MagnitudeConstraints, e.g. given certain conditions
 */
trait ConstraintsAdjuster[T] {
  def adjust(t: T, mc: MagnitudeConstraints): MagnitudeConstraints
}

object ConstraintsAdjuster {
  def fromMagnitudeAdjuster[M <: MagnitudeAdjuster]: ConstraintsAdjuster[M] = new ConstraintsAdjuster[M] {
    override def adjust(t: M, mc: MagnitudeConstraints): MagnitudeConstraints =
      mc.adjust(_ + t.getAdjustment(mc.searchBands))
  }
}

/**
 * Describes constraints for the magnitude of a target
 */
case class MagnitudeConstraints(searchBands: BandsList, faintnessConstraint: FaintnessConstraint, saturationConstraint: Option[SaturationConstraint]) extends MagnitudeFilter {

  override def filter(t: SiderealTarget): Boolean = searchBands.extract(t).exists(contains)

  /**
   * Determines whether the magnitude limits include the given magnitude
   * value.
   */
  def contains(m: Magnitude) = searchBands.bandSupported(m.band) && faintnessConstraint.contains(m.value) && saturationConstraint.forall(_.contains(m.value))

  /**
   * Returns a combination of two MagnitudeConstraints(this and that) such that
   * the faintness limit is the faintest of the two and the saturation limit
   * is the brightest of the two.  In other words, the widest possible range
   * of magnitude bands.
   */
  def union(that: MagnitudeConstraints): Option[MagnitudeConstraints] =
    (searchBands === that.searchBands) option {
      val faintness = faintnessConstraint.max(that.faintnessConstraint)

      // Calculate the min saturation limit if both are defined
      val saturation = (saturationConstraint |@| that.saturationConstraint)(_ min _)

      MagnitudeConstraints(searchBands, faintness, saturation)
    }

  def adjust(f: Double => Double): MagnitudeConstraints = {
    val fl = f(faintnessConstraint.brightness)
    val sl = saturationConstraint.map(_.brightness).map(f)
    MagnitudeConstraints(searchBands, FaintnessConstraint(fl), sl.map(SaturationConstraint.apply))
  }
}
