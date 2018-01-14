package edu.gemini.util.skycalc.calc

import edu.gemini.shared.util.DateTimeUtils
import org.junit.{Ignore, Test}
import org.junit.Assert._
import edu.gemini.spModel.core.Site

import scala.concurrent.duration._

/**
 * Checks that for local time between 0 and 14hrs we calculate sun set / rise of previous night and
 * for hrs between 14 and 24 hrs sun set/rise of this night (i.e. same night as the given date).
 * Check this for both sites.
 */
class SunCalculatorTest {
  private val zoneGN = Site.GN.timezone.toZoneId
  private val zoneGS = Site.GS.timezone.toZoneId
  
  // == GN
  @Test
  @Ignore
  def validateSunCalc00HrsNorth(): Unit = {
    val time     = DateTimeUtils.timeInMs(2014, 11,  1,  0,  0, 0, zoneGN) // 00:00 (local time)
    val timeSet  = DateTimeUtils.timeInMs(2014, 10, 31, 17, 48, 0, zoneGN) // expect previous night
    val timeRise = DateTimeUtils.timeInMs(2014, 11,  1,  6, 23, 0, zoneGN)
    val calc     = SunCalculator(Site.GN, time)
    validate(calc, timeSet, timeRise)
  }

  @Test
  @Ignore
  def validateSunCalc12HrsNorth(): Unit = {
    val time     = DateTimeUtils.timeInMs(2014, 11,  1, 12,  0, 0, zoneGN) // 12:00
    val timeSet  = DateTimeUtils.timeInMs(2014, 10, 31, 17, 48, 0, zoneGN) // expect previous night
    val timeRise = DateTimeUtils.timeInMs(2014, 11,  1,  6, 23, 0, zoneGN)
    val calc     = SunCalculator(Site.GN, time)
    validate(calc, timeSet, timeRise)
  }

  @Test
  @Ignore
  def validateSunCalc14HrsNorth(): Unit = {
    val time     = DateTimeUtils.timeInMs(2014, 11,  1, 14,  0, 0, zoneGN) // 14:00
    val timeSet  = DateTimeUtils.timeInMs(2014, 11,  1, 17, 47, 0, zoneGN) // expect same night
    val timeRise = DateTimeUtils.timeInMs(2014, 11,  2,  6, 23, 0, zoneGN)
    val calc     = SunCalculator(Site.GN, time)
    validate(calc, timeSet, timeRise)
  }

  @Test
  @Ignore
  def validateSunCalc20HrsNorth(): Unit = {
    val time     = DateTimeUtils.timeInMs(2014, 11,  1, 20,  0, 0, zoneGN) // 20:00
    val timeSet  = DateTimeUtils.timeInMs(2014, 11,  1, 17, 47, 0, zoneGN) // expect same night
    val timeRise = DateTimeUtils.timeInMs(2014, 11,  2,  6, 23, 0, zoneGN)
    val calc     = SunCalculator(Site.GN, time)
    validate(calc, timeSet, timeRise)
  }

  // === GS
  @Test
  @Ignore
  def validateSunCalc12HrsSouth(): Unit = {
    val time     = DateTimeUtils.timeInMs(2014, 11,  1, 13, 59, 0, zoneGS) // 13:59
    val timeSet  = DateTimeUtils.timeInMs(2014, 10, 31, 20,  4, 0, zoneGS) // expect previous night
    val timeRise = DateTimeUtils.timeInMs(2014, 11,  1,  6, 48, 0, zoneGS)
    val calc     = SunCalculator(Site.GS, time)
    validate(calc, timeSet, timeRise)
  }

  @Test
  @Ignore
  def validateSunCalc14HrsSouth(): Unit = {
    val time     = DateTimeUtils.timeInMs(2014, 11,  1, 14,  0, 0, zoneGS)  // 14:00
    val timeSet  = DateTimeUtils.timeInMs(2014, 11,  1, 20,  5, 0, zoneGS)  // expect same night
    val timeRise = DateTimeUtils.timeInMs(2014, 11,  2,  6, 47, 0, zoneGS)
    val calc     = SunCalculator(Site.GS, time)
    validate(calc, timeSet, timeRise)
  }

  private def validate(calc: SunCalculator, expectedSet: Long, expectedRise: Long): Unit = {
    assertTrue(Math.abs(expectedSet  - calc.set)  < 60.seconds.toMillis) // allow 60 seconds delta
    assertTrue(Math.abs(expectedRise - calc.rise) < 60.seconds.toMillis)
  }
}
