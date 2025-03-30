package edu.gemini.spModel.gemini.igrins2

import org.junit.Assert._
import org.junit.Test
import squants.time.TimeConversions.TimeConversions

/**
 * Test cases for the Fowler samples calculation.
 */
class FowlerCalcTest {

  @Test def testCalculation(): Unit = {
    assertEquals(2, Igrins2.fowlerSamples(3.08.seconds))
    assertEquals(2, Igrins2.fowlerSamples(4.53236.seconds))
    assertEquals(4, Igrins2.fowlerSamples(4.53237.seconds))
    assertEquals(4, Igrins2.fowlerSamples(10.35152.seconds))
    assertEquals(8, Igrins2.fowlerSamples(10.35153.seconds))
    assertEquals(8,  Igrins2.fowlerSamples(21.98984.seconds))
    assertEquals(16, Igrins2.fowlerSamples(21.98985.seconds))
    assertEquals(16, Igrins2.fowlerSamples(1299.seconds))
  }
}
