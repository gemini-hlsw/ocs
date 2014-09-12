package edu.gemini.util.skycalc.calc

import edu.gemini.spModel.core.Site
import org.junit.Test
import org.junit.Assert._
import edu.gemini.skycalc.TimeUtils

/**
 * Compare some random values with results from http://catserver.ing.iac.es/staralt/index.php
 * This is not meant to test the underlying SkyCalc implementations, we assume that this is all working,
 * this only tests the general mechanics of the TargetCalculator class.
 */
class MoonCalculatorTest {

  @Test
  def calculatesMoon(): Unit = {
    val t = TimeUtils.time(2014, 3, 14, 20, 0, Site.GN.timezone)
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
    val t = TimeUtils.time(2014, 3, 14, 20, 0, Site.GN.timezone)
    val interval = Interval(t, t + TimeUtils.hours(4))
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
    val t = TimeUtils.time(2014, 2, 1, 14, 0, Site.GN.timezone)
    val interval = Interval(t, t + TimeUtils.weeks(9))
    val moon = MoonCalculator(Site.GN, interval, TimeUtils.days(1))

    // quantitative test: there should be two occurrences of each phase in the given 9 weeks period
    assertEquals(2, moon.newMoons.size)
    assertEquals(2, moon.firstQuarterMoons.size)
    assertEquals(2, moon.fullMoons.size)
    assertEquals(2, moon.lastQuarterMoons.size)

    // check date of full moons
    val full1 = TimeUtils.time(2014, 2, 14, 6, 26, Site.GN.timezone)
    val full2 = TimeUtils.time(2014, 3, 15, 19, 11, Site.GN.timezone)
    assertEquals(full1, moon.fullMoons(0), TimeUtils.hours(1))
    assertEquals(full2, moon.fullMoons(1), TimeUtils.hours(1))

    // check that illumination corresponds to full moon phase
    assertEquals(1.0, moon.illuminatedFractionAt(full1), 0.1)  // expect high illumination
    assertEquals(1.0, moon.illuminatedFractionAt(full2), 0.1)
  }
}
