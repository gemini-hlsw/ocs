package edu.gemini.util.skycalc.calc

import edu.gemini.shared.util.DateTimeUtils
import edu.gemini.spModel.core.{Coordinates, Site}
import org.junit.{Ignore, Test}
import org.junit.Assert._

import scala.concurrent.duration._

/**
 * Compare some random values with results from http://catserver.ing.iac.es/staralt/index.php
 * This is not meant to test the underlying SkyCalc implementations, we assume that this is all working,
 * this only tests the general mechanics of the TargetCalculator class.
 */
class TargetCalculatorTest {

  @Test def calculatesTarget(): Unit = {
    val t = DateTimeUtils.timeInMs(2014, 3, 1, 20, 0, 0, Site.GN.timezone.toZoneId)
    val c = (_: Long) => Coordinates.fromDegrees(150, 20).get
    val target = TargetCalculator(Site.GN, c, t)

    // check definition interval
    assertFalse(target.isDefinedAt(t - 1))
    assertTrue(target.isDefinedAt(t))
    assertFalse(target.isDefinedAt(t + 1))

    // check some values
    assertEquals(37, target.elevation, 1)
    assertEquals(1.6, target.airmass, 0.1)
  }

  @Test def calculatesTargetInterval(): Unit = {
    val t = DateTimeUtils.timeInMs(2014, 3, 1, 20, 0, 0, Site.GN.timezone.toZoneId)
    val c = (_: Long) => Coordinates.fromDegrees(150, 20).get
    val interval = Interval(t, t + 4.hours.toMillis)
    val target: TargetCalculator = TargetCalculator(Site.GN, c, interval)

    // check definition interval
    assertFalse(target.isDefinedAt(interval.start - 1))
    assertTrue(target.isDefinedAt(interval.start))
    assertTrue(target.isDefinedAt(interval.end))
//    assertFalse(target.isDefinedAt(interval.end + 1))

    // check some values
    assertEquals(37, target.elevationAt(t), 1)
    assertEquals(1.6, target.airmassAt(t), 0.1)
    assertEquals(89, target.maxElevation, 1)
    assertEquals(37, target.minElevation, 1)
  }

  // === this is for performance trimming purposes, not an actual test case

  @Ignore
  @Test def timingTest(): Unit = {

    val pos = (_: Long) => Coordinates.fromDegrees(150, 20).get
    val t0 = DateTimeUtils.timeInMs(2014, 3, 1, 14, 0, 0, Site.GN.timezone.toZoneId)
    val t1 = DateTimeUtils.timeInMs(2014, 3, 2, 14, 0, 0, Site.GN.timezone.toZoneId)
    val t = System.currentTimeMillis()

    val tc = TargetCalculator(Site.GN, pos, Interval(t0, t1), 5.minutes.toMillis)  // 288 samples

    println(s"time: ${System.currentTimeMillis() - t}ms" )
  }
}
