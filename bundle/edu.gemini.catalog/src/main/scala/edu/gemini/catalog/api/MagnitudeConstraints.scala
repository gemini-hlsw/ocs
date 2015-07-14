package edu.gemini.catalog.api

import edu.gemini.catalog.api.MagnitudeLimits.{SaturationLimit, FaintnessLimit}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core.{MagnitudeBand, Magnitude}
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

  def referenceBand: MagnitudeBand
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
 * Defines a class that can extract a magnitude from a target
 */
sealed trait MagnitudeExtractor {
  def extract(t: SiderealTarget):Option[Magnitude]
  def bandSupported(b: MagnitudeBand):Boolean
}

/**
 * Search for a specific band on the target
 */
case class SingleBandExtractor(band: MagnitudeBand) extends MagnitudeExtractor {
  override def extract(t: SiderealTarget) = t.magnitudeIn(band)
  override def bandSupported(b: MagnitudeBand) = b === band
}

/**
 * Finds the first on a list of bands
 */
case class FirstBandExtractor(bands: NonEmptyList[MagnitudeBand]) extends MagnitudeExtractor {
  override def extract(t: SiderealTarget) = bands.map(t.magnitudeIn).list.find(_.isDefined).flatten
  override def bandSupported(b: MagnitudeBand) = bands.list.contains(b)
}

/**
 * Describes constraints for the magnitude of a target
 */
case class MagnitudeConstraints(referenceBand: MagnitudeBand, extractor: MagnitudeExtractor, faintnessConstraint: FaintnessConstraint, saturationConstraint: Option[SaturationConstraint]) extends MagnitudeFilter {

  override def filter(t: SiderealTarget): Boolean = extractor.extract(t).exists(contains)

  /**
   * Determines whether the magnitude limits include the given magnitude
   * value.
   */
  def contains(m: Magnitude) = extractor.bandSupported(m.band) && faintnessConstraint.contains(m.value) && saturationConstraint.forall(_.contains(m.value))

  /**
   * Returns a combination of two MagnitudeConstraints(this and that) such that
   * the faintness limit is the faintest of the two and the saturation limit
   * is the brightest of the two.  In other words, the widest possible range
   * of magnitude bands.
   */
  def union(that: MagnitudeConstraints): Option[MagnitudeConstraints] =
    (referenceBand === that.referenceBand && extractor == extractor) option {
      val faintness = faintnessConstraint.max(that.faintnessConstraint)

      // Calculate the min saturation limit if both are defined
      val saturation = (saturationConstraint |@| that.saturationConstraint)(_ min _)

      MagnitudeConstraints(referenceBand, extractor, faintness, saturation)
    }

  def adjust(f: Double => Double): MagnitudeConstraints = {
    val fl = f(faintnessConstraint.brightness)
    val sl = saturationConstraint.map(_.brightness).map(f)
    MagnitudeConstraints(referenceBand, extractor, FaintnessConstraint(fl), sl.map(SaturationConstraint.apply))
  }
}

object MagnitudeConstraints {
  val instance = this

  /**
   * Constructor using a direct reference to the magnitude's band
   *
   * @param referenceBand MagnitudeBand to filter on
   * @param faintnessConstraint Limit on how faint a target can be
   * @param saturationConstraint Limit on how bright a target can be
   */
  def apply(referenceBand: MagnitudeBand, faintnessConstraint: FaintnessConstraint, saturationConstraint: Option[SaturationConstraint]):MagnitudeConstraints = MagnitudeConstraints(referenceBand, SingleBandExtractor(referenceBand), faintnessConstraint, saturationConstraint)

  @Deprecated
  def conditionsAdjustmentForJava(limits: MagnitudeLimits, conditions: Conditions): MagnitudeLimits = {
    import edu.gemini.pot.ModelConverters._
    import edu.gemini.shared.util.immutable.ScalaConverters._

    val mc = conditions.adjust(MagnitudeConstraints(limits.getBand.toNewModel, FaintnessConstraint(limits.getFaintnessLimit.getBrightness), limits.getSaturationLimit.asScalaOpt.map(s => SaturationConstraint(s.getBrightness))))
    new MagnitudeLimits(limits.getBand, new FaintnessLimit(mc.faintnessConstraint.brightness), mc.saturationConstraint.map(s => new SaturationLimit(s.brightness)).asGeminiOpt)
  }
}
