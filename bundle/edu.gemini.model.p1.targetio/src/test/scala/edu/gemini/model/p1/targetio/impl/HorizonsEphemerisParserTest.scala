package edu.gemini.model.p1.targetio.impl

import HorizonsEphemerisParser._
import TargetUtil._

import edu.gemini.model.p1.immutable.EphemerisElement

import org.junit.Test
import org.junit.Assert._

class HorizonsEphemerisParserTest {

  @Test def testMissingName() {
    val file = """
line
line
$$SOE
 2011-Mec-29 00:00     23 51 58.27 -11 19 56.6   9.06   6.98 2.98469607736702  22.6989658  77.0713 /T  19.1192
$$EOE
"""
    parse(target, file) match {
      case Success(t, _)        => fail("should be missing name")
      case NoSuccess(msg, next) => assertTrue(msg.contains("Target body name"))
    }
  }

  private def badEphemeris(file: String) {
    parse(target, file) match {
      case Success(t, _)        => fail()
      case NoSuccess(msg, next) => // ok
    }
  }

  @Test def testMissingEphemeris() {
    badEphemeris("""
Target body name: Ceres (1)    {xxx}
line
line
""")
  }

  @Test def testBadDate() {
    badEphemeris("""
Target body name: Ceres (1)    {xxx}
line
$$SOE
 2011-Dec-29 00:00     23 51 58.27 -11 19 56.6   9.06   6.98 2.98469607736702  22.6989658  77.0713 /T  19.1192
 2011-Mec-29 00:00     23 51 58.27 -11 19 56.6   9.06   6.98 2.98469607736702  22.6989658  77.0713 /T  19.1192
 2011-Dec-29 00:00     23 51 58.27 -11 19 56.6   9.06   6.98 2.98469607736702  22.6989658  77.0713 /T  19.1192
$$EOE
line
""")
  }

  @Test def testMissingMag() {
    badEphemeris("""
Target body name: Ceres (1)    {xxx}
line
$$SOE
 2011-Dec-29 00:00     23 51 58.27 -11 19 56.6   9.06   6.98 2.98469607736702  22.6989658  77.0713 /T  19.1192
 2011-Mec-29 00:00     23 51 58.27 -11 19 56.6
 2011-Dec-29 00:00     23 51 58.27 -11 19 56.6   9.06   6.98 2.98469607736702  22.6989658  77.0713 /T  19.1192
$$EOE
line
""")
  }

  private def goodEphemeris(expected: List[EphemerisElement], file: String) {
    parse(target, file) match {
      case Success(t, _)        =>
        assertEquals("Ceres (1)", t.name)
        validateElements(expected, t.ephemeris)
      case NoSuccess(msg, next) => fail(msg)
    }
  }

  @Test def testEmpty() {
    goodEphemeris(Nil, """
Target body name: Ceres (1)    {xxx}
$$SOE
$$EOE
""")
  }

  @Test def testOne() {
    goodEphemeris(List(dec30), """
Target body name: Ceres (1)    {xxx}
$$SOE
 2011-Dec-30 00:00     23 52 00.00 -11 20 00.0   9.00   6.98 2.98469607736702  22.6989658  77.0713 /T  19.1192
$$EOE
""")
  }

  @Test def testBadCoordinates() {
    badEphemeris("""
Target body name: Ceres (1)    {xxx}
$$SOE
 2011-Dec-30 00:00     -2 -52 00.00 21 20 00.0   9.00   6.98 2.98469607736702  22.6989658  77.0713 /T  19.1192
$$EOE
""")
  }

  @Test def testMultiple() {
    goodEphemeris(List(dec30, dec31, jan01), """
Target body name: Ceres (1)    {xxx}
$$SOE
 2011-Dec-30 00:00     23 52 00.00 -11 20 00.0   9.00   6.98 2.98469607736702  22.6989658  77.0713 /T  19.1192
 2011-Dec-31 00:00     23 52 01.00 -11 20 01.0   9.10   6.98 2.98469607736702  22.6989658  77.0713 /T  19.1192
 2012-Jan-01 00:00     23 52 02.00 -11 20 02.0   9.20   6.98 2.98469607736702  22.6989658  77.0713 /T  19.1192
$$EOE
""")
  }
}