package edu.gemini.spModel.gemini.nifs

import InstNIFS.calcWavelength
import NIFSParams.Disperser
import NIFSParams.Filter

import org.junit.Test
import org.junit.Assert._

/**
 * Test cases for the observing wavelength calculation.
 */
class ObsWavelengthCalcTest {
  @Test def testDisperserNone() {
    Filter.values.foreach(f =>
      assertEquals(f.getWavelength, calcWavelength(Disperser.MIRROR, f, 42.0))
    )
  }

  @Test def testDisperserSome() {
    Disperser.values.filter(Disperser.MIRROR.!=).foreach(d =>
      assertEquals("42.0", calcWavelength(d, Filter.JH_FILTER, 42.0))
    )
  }

  // This is an odd case that needs to be defined
  @Test def testSameAsMirror() {
    val same = Filter.SAME_AS_DISPERSER
    assertNull(calcWavelength(Disperser.MIRROR, same, 42.0))
  }

  // Reject disperser lambda of 0
  @Test def testDisperserLambda0() {
    assertNull(calcWavelength(Disperser.K, Filter.JH_FILTER, 0.0))
  }
}