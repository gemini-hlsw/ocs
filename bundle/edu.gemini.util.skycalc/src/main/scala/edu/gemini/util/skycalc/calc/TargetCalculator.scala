package edu.gemini.util.skycalc.calc

import edu.gemini.spModel.core.{Coordinates, Site}
import edu.gemini.skycalc.ImprovedSkyCalc
import java.util.Date

import edu.gemini.util.skycalc.calc.TargetCalculator.Fields

import scala.concurrent.duration._


/**
 * Target calculator that allows to calculate different attributes of a target for a given interval at a given sampling
 * rate. The purpose of this trait is twofold:
 * <ul>
 *   <li>It is a Scala facade to the Java skycalc code in {@see edu.gemini.skycalc.ImprovedSkyCalc}.</li>
 *   <li>It caches the values for a target for a given interval and sampling rate; this is relevant for places
 *       where these values are needed repetitively because the calculation is pretty complex and slow.</li>
 * </ul>
 * If in doubt use {@link isDefinedAt} to make sure that values for a given time are actually calculated before
 * accessing them, otherwise an out of bounds exception will be thrown.
 */
trait TargetCalculator extends Calculator {
  require(site == Site.GN || site == Site.GS)

  val site: Site
  val targetLocation: Long => Coordinates

  val values: Vector[Vector[Double]] = calculate()

  import Fields._

  // ==  Gets the first of all calculated values for a given field, use this if only one value was calculated. ==
  lazy val elevation: Double = valueAt(Elevation.id, start)
  lazy val azimuth: Double = valueAt(Azimuth.id, start)
  lazy val airmass: Double = valueAt(Airmass.id, start)
  lazy val lunarDistance: Double = valueAt(LunarDistance.id, start)
  lazy val parallacticAngle: Double = valueAt(ParallacticAngle.id, start)
  lazy val hourAngle: Double = valueAt(HourAngle.id, start)
  lazy val skyBrightness: Double = valueAt(SkyBrightness.id, start)

  // == Accessor for a value by its field enumerator and a time.
  def valueAt(field: Field, t: Long): Double = valueAt(field.id, t)

  // ==  Accessors for any calculated values for a given field, use this if an interval of values was sampled. ==
  def elevationAt(t: Long): Double = valueAt(Elevation.id, t)
  lazy val minElevation: Double = min(Elevation.id)
  lazy val maxElevation: Double = max(Elevation.id)
  lazy val meanElevation: Double = mean(Elevation.id)

  def azimuthAt(t: Long): Double = valueAt(Azimuth.id, t)
  lazy val minAzimuth: Double = min(Azimuth.id)
  lazy val maxAzimuth: Double = max(Azimuth.id)
  lazy val meanAzimuth: Double = mean(Azimuth.id)

  def airmassAt(t: Long): Double = valueAt(Airmass.id, t)
  lazy val minAirmass: Double = min(Airmass.id)
  lazy val maxAirmass: Double = max(Airmass.id)
  lazy val meanAirmass: Double = mean(Airmass.id)

  def lunarDistanceAt(t: Long): Double = valueAt(LunarDistance.id, t)
  lazy val minLunarDistance: Double = min(LunarDistance.id)
  lazy val maxLunarDistance: Double = max(LunarDistance.id)
  lazy val meanLunarDistance: Double = mean(LunarDistance.id)

  def parallacticAngleAt(t: Long): Double = valueAt(ParallacticAngle.id, t)
  lazy val minParallacticAngle: Double = min(ParallacticAngle.id)
  lazy val maxParallacticAngle: Double = max(ParallacticAngle.id)
  lazy val meanParallacticAngle: Double = mean(ParallacticAngle.id)


  // If the target is visible during the scheduled time, return the weighted mean parallactic angle as Some(angle in degrees).
  // Otherwise, the target is not visible, so return None.
  lazy val weightedMeanParallacticAngle: Option[Double] = {
    val (weightedAngles, weights) = values(ParallacticAngle.id).zip(times).zip(values(Airmass.id)).map {
      case ((angle, t), airmass) =>
        // Wrap negative angles as per Andy's comment in OCSADV-16.
        val normalizedAngle = {
          if (angle < 0) {
            val normalizingFactor = {
              val dec = targetLocation(t).dec.toDegrees
              if (dec - site.latitude < -10) 0
              else if (dec - site.latitude < 10) 180
              else 360
            }
            angle + normalizingFactor
          }
          else angle
        }

        //val weight = if (airmass <= 1.0) 0.0 else 1.6 * math.pow(airmass - 1.0, 0.6)
        val weight = if (airmass <= 1.0) 0.0 else math.pow(airmass - 1.0, 1.3)
        (normalizedAngle * weight, weight)
    }.unzip

    val weightedSum = weights.sum
    if (weightedSum == 0) None
    else Some(weightedAngles.sum / weightedSum)
  }

  def hourAngleAt(t: Long): Double = valueAt(HourAngle.id, t)
  lazy val minHourAngle: Double = min(HourAngle.id)
  lazy val maxHoursAngle: Double = max(HourAngle.id)
  lazy val meanHoursAngle: Double = mean(HourAngle.id)

  def skyBrightnessAt(t: Long): Double = valueAt(SkyBrightness.id, t)
  lazy val minSkyBrightness: Double = min(SkyBrightness.id)
  lazy val maxSkyBrightness: Double = max(SkyBrightness.id)
  lazy val meanSkyBrightness: Double = mean(SkyBrightness.id)

  /**
   * Calculates all values for the given times.
   * @return
   */
  protected def calculate() = {
    val skycalc = new ImprovedSkyCalc(site)

    // prepare temporary data structure
    val values = Array (
      new Array[Double](samples),
      new Array[Double](samples),
      new Array[Double](samples),
      new Array[Double](samples),
      new Array[Double](samples),
      new Array[Double](samples),
      new Array[Double](samples)
    )
    // fill temporary data structure with calculated values
    for (ix <- 0 to samples-1) {
      val t = times(ix)
      skycalc.calculate(targetLocation(t), new Date(t), true)
      values(Elevation.id)(ix) = skycalc.getAltitude
      values(Azimuth.id)(ix) = skycalc.getAzimuth
      values(Airmass.id)(ix) = skycalc.getAirmass
      values(LunarDistance.id)(ix) = skycalc.getLunarDistance
      values(ParallacticAngle.id)(ix) = skycalc.getParallacticAngle
      values(HourAngle.id)(ix) = skycalc.getHourAngle
      values(SkyBrightness.id)(ix) = skycalc.getTotalSkyBrightness.doubleValue
    }

    // turn into immutable data structure
    Vector(
      // IMPORTANT: Make sure the order reflects the id values of the field enums!
      Vector(values(Elevation.id):_*),
      Vector(values(Azimuth.id):_*),
      Vector(values(Airmass.id):_*),
      Vector(values(LunarDistance.id):_*),
      Vector(values(ParallacticAngle.id):_*),
      Vector(values(HourAngle.id):_*),
      Vector(values(SkyBrightness.id):_*)
    )

  }
}

case class IntervalTargetCalculator(site: Site, targetLocation: Long => Coordinates, defined: Interval, rate: Long) extends FixedRateCalculator with LinearInterpolatingCalculator with TargetCalculator

case class SampleTargetCalculator(site: Site, targetLocation: Long => Coordinates, times: Vector[Long]) extends IrregularIntervalCalculator with LinearInterpolatingCalculator with TargetCalculator

case class SingleValueTargetCalculator(site: Site, targetLocation: Long => Coordinates, time: Long) extends SingleValueCalculator with TargetCalculator

object TargetCalculator {

  /** Enumeration that defines the different fields for this calculator for indexed access in sequence. */
  object Fields extends Enumeration {
    type Field = Value
    val Elevation, Azimuth, Airmass, LunarDistance, ParallacticAngle, HourAngle, SkyBrightness = Value
  }

  def apply(site: Site, targetLocation: Long => Coordinates, defined: Interval, rate: Long = 30.seconds.toMillis) = {
    new IntervalTargetCalculator(site, targetLocation, defined, rate)
  }
  def apply(site: Site, targetLocation: Long => Coordinates, time: Long): TargetCalculator = {
    new SingleValueTargetCalculator(site, targetLocation, time)
  }
  def apply(site: Site, targetLocation: Long => Coordinates, times: Vector[Long]): TargetCalculator = {
    new SampleTargetCalculator(site, targetLocation, times)
  }
}

