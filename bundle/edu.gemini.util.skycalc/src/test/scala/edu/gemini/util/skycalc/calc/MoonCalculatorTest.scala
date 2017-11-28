package edu.gemini.util.skycalc.calc

import edu.gemini.shared.util.DateTimeUtils
import edu.gemini.spModel.core.Site
import org.junit.Test
import org.junit.Assert._

import scala.concurrent.duration._

/**
 * Compare some random values with results from http://catserver.ing.iac.es/staralt/index.php
 * This is not meant to test the underlying SkyCalc implementations, we assume that this is all working,
 * this only tests the general mechanics of the TargetCalculator class.
 */
class MoonCalculatorTest {

  @Test
  def calculatesMoon(): Unit = {
    val t = DateTimeUtils.timeInMs(2014, 3, 14, 20, 0, 0, Site.GN.timezone.toZoneId)
    val moon = MoonCalculator(Site.GN, t)

    // check definition interval
    assertFalse(moon.isDefinedAt(t - 1))
    assertTrue(moon.isDefinedAt(t))
    assertFalse(moon.isDefinedAt(t + 1))

    // check some values
    assertEquals(37, moon.elevation, 1)
  }

  @Test
  def calculatesMoonInterval(): Unit = {
    val t = DateTimeUtils.timeInMs(2014, 3, 14, 20, 0, 0, Site.GN.timezone.toZoneId)
    val interval = Interval(t, t + 4.hours.toMillis)
    val moon = MoonCalculator(Site.GN, interval)

    // check definition interval
    assertFalse(moon.isDefinedAt(interval.start - 1))
    assertTrue(moon.isDefinedAt(interval.start))
    assertTrue(moon.isDefinedAt(interval.end))
//    assertFalse(moon.isDefinedAt(interval.end + 1))

    // check some values
    assertEquals(37, moon.elevationAt(t), 1)
    assertEquals(74, moon.maxElevation, 1)
    assertEquals(37, moon.minElevation, 1)
  }

  @Test
  def calculatesMoonPhases(): Unit = {
    val t = DateTimeUtils.timeInMs(2014, 2, 1, 14, 0, 0, Site.GN.timezone.toZoneId)
    val interval = Interval(t, t + 63.days.toMillis)
    val moon = MoonCalculator(Site.GN, interval, 1.day.toMillis)

    // quantitative test: there should be two occurrences of each phase in the given 9 weeks period
    assertEquals(2, moon.newMoons.size)
    assertEquals(2, moon.firstQuarterMoons.size)
    assertEquals(2, moon.fullMoons.size)
    assertEquals(2, moon.lastQuarterMoons.size)

    // check date of full moons
    val full1 = DateTimeUtils.timeInMs(2014, 2, 14, 6, 26, 0, Site.GN.timezone.toZoneId)
    val full2 = DateTimeUtils.timeInMs(2014, 3, 15, 19, 11, 0, Site.GN.timezone.toZoneId)
    assertEquals(full1, moon.fullMoons(0), DateTimeUtils.MillisecondsPerHour)
    assertEquals(full2, moon.fullMoons(1), DateTimeUtils.MillisecondsPerHour)

    // check that illumination corresponds to full moon phase
    assertEquals(1.0, moon.illuminatedFractionAt(full1), 0.1)  // expect high illumination
    assertEquals(1.0, moon.illuminatedFractionAt(full2), 0.1)
  }
}
