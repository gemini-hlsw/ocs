package edu.gemini.util.skycalc.calc

import edu.gemini.skycalc.SunRiseSet
import edu.gemini.spModel.core.Site

import java.util.{Calendar, GregorianCalendar}

/**
 * Sun related calculations based on SunRiseSet.
 */
case class SunCalculator(site: Site, date: Long) {
  private val sunCalc = sunCalculator

  def set: Long                   = sunCalc.sunset
  def rise: Long                  = sunCalc.sunrise
  def nightTime: Interval         = new Interval(set, rise)
  def middleNightTime: Long       = (set + rise) / 2
  def scienceTime: Interval       = new Interval(sunCalc.nauticalTwilightStart, sunCalc.nauticalTwilightEnd)
  def civilTwilightEnd: Long      = sunCalc.civilTwilightEnd
  def civilTwilightStart: Long    = sunCalc.civilTwilightStart
  def nauticalTwilightEnd: Long   = sunCalc.nauticalTwilightEnd
  def nauticalTwilightStart: Long = sunCalc.nauticalTwilightStart
  def astroTwilightEnd: Long      = sunCalc.astronomicalTwilightEnd
  def astroTwilightStart: Long    = sunCalc.astronomicalTwilightStart

  /**
   * Creates a sun calculator for the given night and site.
   * @return
   */
  private def sunCalculator: SunRiseSet = {
    // IMPORTANT: Subtract 14:00hrs because we count the first 14hrs of each day to the last day/night.
    // Then set to local time noon (12:00) in order to get expected result from SunRiseSet calculator.
    val cal = new GregorianCalendar(site.timezone)
    cal.setTimeInMillis(date)
    cal.add(Calendar.HOUR_OF_DAY, -14)
    cal.set(Calendar.HOUR_OF_DAY, 12)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    new SunRiseSet(cal.getTimeInMillis, site)
  }
}
