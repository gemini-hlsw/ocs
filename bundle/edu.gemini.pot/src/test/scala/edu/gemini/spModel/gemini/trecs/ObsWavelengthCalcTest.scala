package edu.gemini.spModel.gemini.trecs

import InstTReCS.calcWavelength
import TReCSParams.Disperser
import TReCSParams.Filter

import org.junit.Test
import org.junit.Assert._


/**
 * Test cases for the observing wavelength calculation.
 */
class ObsWavelengthCalcTest {

  // If the disperser is mirror, then the wavelength associated with the filter
  // should be used.
  @Test def testDisperserMirror() {
    Filter.values.foreach(f =>
      assertEquals(f.getWavelength, calcWavelength(Disperser.MIRROR, f, 42.0))
    )
  }

  // If there is a disperser, then we use whatever value is present for the
  // disperser lambda.
  @Test def testDisperserSome() {
    Disperser.values.filter(Disperser.MIRROR.!=).foreach(d =>
      assertEquals("42.0", calcWavelength(d, Filter.K, 42.0))
    )
  }

  // Reject disperser lambda of 0
  @Test def testDisperserLambda0() {
    assertNull(calcWavelength(Disperser.LOW_RES_10, Filter.K, 0.0))
  }
}