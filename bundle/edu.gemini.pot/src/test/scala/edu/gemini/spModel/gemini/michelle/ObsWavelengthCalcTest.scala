package edu.gemini.spModel.gemini.michelle

import InstMichelle.calcWavelength
import MichelleParams.Disperser
import MichelleParams.Filter

import org.junit.Test
import org.junit.Assert._

/**
 * Test cases for the observing wavelength calculation.
 */
class ObsWavelengthCalcTest {
  // Should be same as the filter if Disperser is none
  @Test def testDisperserNone() {
    Filter.values.foreach(f =>
      assertEquals(f.getWavelength, calcWavelength(Disperser.MIRROR, f, 42.0))
    )
  }

  // Should be the central wavelength (i.e. "disperser lambda") if using a
  // disperser.
  @Test def testDisperserSome() {
    Disperser.values.filter(Disperser.MIRROR.!=).foreach(d =>
      assertEquals("42.0", calcWavelength(d, Filter.N_PRIME, 42.0))
    )
  }

  // A disperser lambda of 0.0 should be rejected.
  @Test def testDisperserLambda0() {
    assertNull(calcWavelength(Disperser.LOW_RES_10, Filter.N10, 0.0))
  }
}