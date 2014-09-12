package edu.gemini.spModel.gemini.niri

import edu.gemini.spModel.gemini.niri.Niri.Filter
import edu.gemini.spModel.gemini.niri.Niri.Disperser
import edu.gemini.spModel.gemini.niri.InstNIRI.calcWavelength

import org.junit.Test
import org.junit.Assert._

/**
 * Test cases for the observing wavelength calculation.
 */
class ObsWavelengthCalcTest {

  @Test def testDisperserNone() {
    assertEquals(Filter.BBF_H.getWavelengthAsString, calcWavelength(Disperser.NONE, Filter.BBF_H))
  }

  @Test def testDisperserSome() {
    assertFalse(Disperser.J.getCentralWavelengthAsString == Filter.BBF_H.getWavelengthAsString)
    assertEquals(Disperser.J.getCentralWavelengthAsString, calcWavelength(Disperser.J, Filter.BBF_H))
  }
}