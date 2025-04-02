package edu.gemini.spModel.gemini.igrins2

import org.junit.Assert._
import org.junit.Test
import squants.time.TimeConversions.TimeConversions

/**
 * Test cases for the Fowler samples calculation.
 */
class FowlerCalcTest {

  @Test def testCalculation(): Unit = {
    assertEquals(1, Igrins2.fowlerSamples(3.08.seconds))
    assertEquals(1, Igrins2.fowlerSamples(4.53236.seconds))
    assertEquals(2, Igrins2.fowlerSamples(4.53237.seconds))
    assertEquals(2, Igrins2.fowlerSamples(7.44194.seconds))
    assertEquals(4, Igrins2.fowlerSamples(7.44195.seconds))
    assertEquals(4, Igrins2.fowlerSamples(13.26110.seconds))
    assertEquals(8, Igrins2.fowlerSamples(13.26111.seconds))
    assertEquals(8,  Igrins2.fowlerSamples(24.89942.seconds))
    assertEquals(16, Igrins2.fowlerSamples(24.89943.seconds))
    assertEquals(16, Igrins2.fowlerSamples(1299.seconds))
  }
}
