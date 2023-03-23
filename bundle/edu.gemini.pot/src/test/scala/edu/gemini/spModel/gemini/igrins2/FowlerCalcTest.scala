package edu.gemini.spModel.gemini.igrins2

import org.junit.Assert._
import org.junit.Test
import squants.time.TimeConversions.TimeConversions

/**
 * Test cases for the observing wavelength calculation.
 */
class FowlerCalcTest {

  @Test def testCalculation(): Unit = {
    assertEquals(1, Igrins2.fowlerSamples(0.seconds))
    assertEquals(1, Igrins2.fowlerSamples(1.63.seconds))
    assertEquals(2, Igrins2.fowlerSamples(5.seconds))
    assertEquals(4, Igrins2.fowlerSamples(10.seconds))
    assertEquals(8, Igrins2.fowlerSamples(15.seconds))
    assertEquals(16, Igrins2.fowlerSamples(25.seconds))
    assertEquals(16, Igrins2.fowlerSamples(1299.seconds))
  }
}