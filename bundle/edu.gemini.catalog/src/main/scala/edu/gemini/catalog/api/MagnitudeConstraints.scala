package edu.gemini.catalog.api

import edu.gemini.catalog.api.MagnitudeLimits.{SaturationLimit, FaintnessLimit}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core.{SingleBand, BandsList, MagnitudeBand, Magnitude}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions

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

object MagnitudeConstraints {

  // Only used when constructing magnitude limits on the TPE. Should not be needed when using the new catalog navigator
  @Deprecated
  def conditionsAdjustmentForJava(limits: MagnitudeLimits, conditions: Conditions): MagnitudeLimits = {
    import edu.gemini.pot.ModelConverters._
    import edu.gemini.shared.util.immutable.ScalaConverters._

    val mc = conditions.adjust(MagnitudeConstraints(SingleBand(limits.getBand.toNewModel), FaintnessConstraint(limits.getFaintnessLimit.getBrightness), limits.getSaturationLimit.asScalaOpt.map(s => SaturationConstraint(s.getBrightness))))
    new MagnitudeLimits(limits.getBand, new FaintnessLimit(mc.faintnessConstraint.brightness), mc.saturationConstraint.map(s => new SaturationLimit(s.brightness)).asGeminiOpt)
  }
}
