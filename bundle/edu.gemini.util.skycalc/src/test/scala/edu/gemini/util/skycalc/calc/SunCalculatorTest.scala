package edu.gemini.util.skycalc.calc

import org.junit.{Ignore, Test}
import org.junit.Assert._
import edu.gemini.spModel.core.Site
import edu.gemini.skycalc.TimeUtils

/**
 * Checks that for local time between 0 and 14hrs we calculate sun set / rise of previous night and
 * for hrs between 14 and 24 hrs sun set/rise of this night (i.e. same night as the given date).
 * Check this for both sites.
 */
class SunCalculatorTest {

  // == GN
  @Test
  @Ignore
  def validateSunCalc00HrsNorth(): Unit = {
    val time     = TimeUtils.time(2014, 11,  1,  0,  0, Site.GN.timezone) // 00:00 (local time)
    val timeSet  = TimeUtils.time(2014, 10, 31, 17, 48, Site.GN.timezone) // expect previous night
    val timeRise = TimeUtils.time(2014, 11,  1,  6, 23, Site.GN.timezone)
    val calc = new SunCalculator(Site.GN, time)
    validate(calc, timeSet, timeRise)
  }

  @Test
  @Ignore
  def validateSunCalc12HrsNorth(): Unit = {
    val time     = TimeUtils.time(2014, 11,  1, 12,  0, Site.GN.timezone) // 12:00
    val timeSet  = TimeUtils.time(2014, 10, 31, 17, 48, Site.GN.timezone) // expect previous night
    val timeRise = TimeUtils.time(2014, 11,  1,  6, 23, Site.GN.timezone)
    val calc = new SunCalculator(Site.GN, time)
    validate(calc, timeSet, timeRise)
  }

  @Test
  @Ignore
  def validateSunCalc14HrsNorth(): Unit = {
    val time     = TimeUtils.time(2014, 11,  1, 14,  0, Site.GN.timezone) // 14:00
    val timeSet  = TimeUtils.time(2014, 11,  1, 17, 47, Site.GN.timezone) // expect same night
    val timeRise = TimeUtils.time(2014, 11,  2,  6, 23, Site.GN.timezone)
    val calc = new SunCalculator(Site.GN, time)
    validate(calc, timeSet, timeRise)
  }

  @Test
  @Ignore
  def validateSunCalc20HrsNorth(): Unit = {
    val time     = TimeUtils.time(2014, 11,  1, 20,  0, Site.GN.timezone) // 20:00
    val timeSet  = TimeUtils.time(2014, 11,  1, 17, 47, Site.GN.timezone) // expect same night
    val timeRise = TimeUtils.time(2014, 11,  2,  6, 23, Site.GN.timezone)
    val calc = new SunCalculator(Site.GN, time)
    validate(calc, timeSet, timeRise)
  }

  // === GS
  @Test
  @Ignore
  def validateSunCalc12HrsSouth(): Unit = {
    val time     = TimeUtils.time(2014, 11,  1, 13, 59, Site.GS.timezone) // 13:59
    val timeSet  = TimeUtils.time(2014, 10, 31, 20, 4, Site.GS.timezone) // expect previous night
    val timeRise = TimeUtils.time(2014, 11,  1,  6, 48, Site.GS.timezone)
    val calc = new SunCalculator(Site.GS, time)
//    println("calc.sunset  = " + CalendarUtil.printTime(calc.set, Site.GS.timezone))
//    println("calc.sunrise = " + CalendarUtil.printTime(calc.rise, Site.GS.timezone))
    validate(calc, timeSet, timeRise)
  }

  @Test
  @Ignore
  def validateSunCalc14HrsSouth(): Unit = {
    val time     = TimeUtils.time(2014, 11,  1, 14,  0, Site.GS.timezone)  // 14:00
    val timeSet  = TimeUtils.time(2014, 11,  1, 20, 5, Site.GS.timezone)  // expect same night
    val timeRise = TimeUtils.time(2014, 11,  2,  6, 47, Site.GS.timezone)
    val calc = new SunCalculator(Site.GS, time)
    validate(calc, timeSet, timeRise)
  }

  private def validate(calc: SunCalculator, expectedSet: Long, expectedRise: Long): Unit = {
    assertTrue(Math.abs(expectedSet  - calc.set)  < 60000) // allow 60 seconds delta
    assertTrue(Math.abs(expectedRise - calc.rise) < 60000)
  }
}
