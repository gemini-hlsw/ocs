package edu.gemini.util.skycalc

import java.time.Instant
import java.util.Date

import edu.gemini.skycalc.ImprovedSkyCalc
import edu.gemini.spModel.core.Site
import edu.gemini.util.skycalc.Night._
import edu.gemini.util.skycalc.calc.{Interval, MoonCalculator, Solution, SunCalculator}
import edu.gemini.util.skycalc.constraint.MoonElevationConstraint
import edu.gemini.shared.util.DateTimeUtils

import scala.concurrent.duration._

//
// SW: I would like to refactor this class but it is used heavily in QV, which
// is under development in another source tree and will be merged back in later.
// The edu.gemini.skycalc.Night interface and the TwilightBoundedNight seems
// like a better concept since it is about a single night interval.  This
// class mixes a single night interval with all the different bounds types.
// If we need easy access to all of them, we should rename this as Nights
// (plural) and hold references to instances with all the TwilightBoundTypes.
//
// At any rate the fact that is one is called "Night" keeps me from using this
// package to house the stuff in edu.gemini.skycalc.
//

object Night {
  trait Bound
  object Day extends Bound
  object SunsetSunrise extends Bound
  object CivilTwilight extends Bound
  object NauticalTwilight extends Bound
  object AstronomicalTwilight extends Bound
}

/**
 * Calculate and store general information about a night.
 * All information in this object is only depending on site and date.
 */
case class Night(site: Site, date: Long, bound: Bound = SunsetSunrise) {
  // don't allow Site.BOTH or Site.NONE
  require(site == Site.GN || site == Site.GS, "specific site needed, this works only for either GN or GS")

  /** These two objects calculate and store/cache all the relevant values. */
  private val sunCalc = SunCalculator(site, date)

  /** Gets the interval for this night that is bounded by the given type of bound. */
  def bounds(bound: Bound): Interval = bound match {
    case Day => Interval(dayStart, dayEnd)
    case SunsetSunrise => Interval(sunset, sunrise)
    case CivilTwilight => Interval(civilTwilightStart, civilTwilightEnd)
    case NauticalTwilight => Interval(nauticalTwilightStart, nauticalTwilightEnd)
    case AstronomicalTwilight => Interval(astroTwilightStart, astroTwilightEnd)
  }

  val dayStart: Long = DateTimeUtils.startOfDayInMs(date, site.timezone.toZoneId)
  val dayEnd: Long   = DateTimeUtils.endOfDayInMs(date, site.timezone.toZoneId)

  /** Sunset, start of the night. */
  val sunset: Long = sunCalc.set

  /** Sunrise, end of the night. */
  val sunrise: Long = sunCalc.rise

  /** Civil twilight end. */
  val civilTwilightEnd: Long = sunCalc.civilTwilightEnd

  /** Civil twilight start. */
  val civilTwilightStart: Long = sunCalc.civilTwilightStart

  /** Nautical twilight end. */
  val nauticalTwilightEnd: Long = sunCalc.nauticalTwilightEnd

  /** Nautical twilight start. */
  val nauticalTwilightStart: Long = sunCalc.nauticalTwilightStart

  /** Astronomical twilight end. */
  val astroTwilightEnd: Long = sunCalc.astroTwilightEnd

  /** Astronomical twilight start. */
  val astroTwilightStart: Long = sunCalc.astroTwilightStart

  /** Interval between sunset and sunrise. */
  val nightTime: Interval = sunCalc.nightTime

  /** Interval between the 12 degree or nautical twilight. */
  val scienceTime: Interval = sunCalc.scienceTime

  /** Middle of the night, defined as the mid point between sunset and sunrise. */
  val middleNightTime: Long = sunCalc.middleNightTime

  /** Interval between defines start and end time. */
  val start: Long = bounds(bound).start
  val end: Long = bounds(bound).end
  val interval: Interval = Interval(start, end)
  val duration: Long = end - start

  // =====
  // Some additional values which are often interesting to know but are calculated only lazily.
  // =====

  /**
   * The part of the science time for this night which is considered "dark" in milliseconds.
   * The rules are: A night is bright entirely if the moon is illuminated more than 96% (full moon) and dark
   * entirely if the moon is illuminated less than 15% (new moon). For all other nights we consider that part
   * of the night as dark during which the moon is below the horizon.
   */
  lazy val darkScienceTime: Long =
    if (moonCalculator.maxIlluminatedFraction >= 0.96) 0
    else if (moonCalculator.maxIlluminatedFraction <= 0.15) scienceTime.duration
    else moonBelowHorizon.restrictTo(scienceTime).duration

  /** The part of the science time for this night which is considered "bright" in milliseconds. */
  lazy val brightScienceTime: Long = scienceTime.duration - darkScienceTime

  /**
   * The rise time of the moon (if any) between start and end of night.
   * Only interested in value if moon rises after night starts (otherwise it was already up at the start of the night).
   */
  lazy val moonRise: Option[Long] = moonAboveHorizon.intervals.find(_.start != interval.start).map(_.start)

  /**
   * The set time of the moon (if any) between start and end of night.
   * Only interested in value if moon sets before night ends.
   */
  lazy val moonSet: Option[Long] = moonAboveHorizon.intervals.find(_.end != interval.end).map(_.end)

  /** Solution with interval moon is below horizon for this night. */
  lazy val moonBelowHorizon: Solution =
    Solution(interval).reduce(moonAboveHorizon)

  /** Solution with interval moon is above horizon for this night. */
  lazy val moonAboveHorizon: Solution =
    MoonElevationConstraint(0, Double.MaxValue, 3.minutes.toMillis).solve(interval, moonCalculator)

  /** Local sidereal time at the middle of the night (ie. halfway between sunset and sunrise). */
  lazy val lstMiddleNightTime: Long = {
    val skyCalc = new ImprovedSkyCalc(site)
    skyCalc.getLst(new Date(middleNightTime - site.timezone().getRawOffset)).getTime
  }

  /** Local sidereal time as a fraction of 24hrs at the middle of the night (ie. halfway between sunset and sunrise). */
  lazy val lst: Double = {
    val zdt = Instant.ofEpochMilli(lstMiddleNightTime).atZone(site.timezone.toZoneId)
    val hrs = zdt.getHour
    val min = zdt.getMinute
    val sec = zdt.getSecond
    hrs.toDouble + min.toDouble / DateTimeUtils.MinutesPerHour + sec.toDouble / DateTimeUtils.SecondsPerHour
  }

  /** A moon calculator for this night. See {@link MoonCalculator} for details. */
  lazy val moonCalculator = MoonCalculator(site, interval)

}
