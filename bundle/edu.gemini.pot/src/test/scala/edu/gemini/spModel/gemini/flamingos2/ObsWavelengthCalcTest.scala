package edu.gemini.spModel.gemini.flamingos2

import edu.gemini.spModel.gemini.flamingos2.Flamingos2.getObservingWavelength
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.Disperser
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.Filter

import org.junit.Test
import org.junit.Assert._

/**
 * Test cases for the observing wavelength calculation.
 */
class ObsWavelengthCalcTest {
  // Disperser NONE means use the filter wavelength.
  @Test def testDisperserNone() {
    Filter.values.foreach(f =>
      assertEquals(f.getWavelength, getObservingWavelength(Disperser.NONE, f))
    )
  }

  // Disperser not NONE and filter not OPEN means use the filter wavelength.
  @Test def testDisperserAndFilterNotOpen() {
    Filter.values.filter(_ != Filter.OPEN).foreach(f =>
      assertEquals(f.getWavelength, getObservingWavelength(Disperser.R1200HK, f))
    )
  }

  // Disperser not NONE and filter OPEN means use the disperser wavelength.
  @Test def testDisperserAndFilterOpen() {
    Disperser.values.filter(_ != Disperser.NONE).foreach(d =>
      assertEquals(d.getWavelength, getObservingWavelength(d, Filter.OPEN))
    )
  }
}