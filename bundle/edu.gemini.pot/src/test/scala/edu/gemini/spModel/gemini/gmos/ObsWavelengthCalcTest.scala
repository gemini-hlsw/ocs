package edu.gemini.spModel.gemini.gmos

import GmosNorthType.{DisperserNorth, FilterNorth}
import GmosSouthType.{DisperserSouth, FilterSouth}

import org.junit.Test
import org.junit.Assert._

/**
 * Test cases for the observing wavelength calculation.
 */
class ObsWavelengthCalcTest {
  private val mirrorNorth = DisperserNorth.MIRROR
  private val mirrorSouth = DisperserSouth.MIRROR

  // When not using a disperser, the wavelength is determined by the filter.
  @Test def testMirror() {
    FilterNorth.values.foreach {
      f => assertEquals(f.getWavelength, InstGmosCommon.calcWavelength(mirrorNorth, f, 42.0))
    }

    FilterSouth.values.foreach {
      f => assertEquals(f.getWavelength, InstGmosCommon.calcWavelength(mirrorSouth, f, 42.0))
    }
  }

  // When using a disperser, the wavelength is the explicitly passed value
  // divided by 1000
  @Test def testNotMirror() {
    val fn = FilterNorth.g_G0301
    DisperserNorth.values.filter(mirrorNorth.!=).foreach {
      d => assertEquals("0.042", InstGmosCommon.calcWavelength(d, fn, 42.0))
    }
    val fs = FilterSouth.CaT_G0333
    DisperserSouth.values.filter(mirrorSouth.!=).foreach {
      d => assertEquals("0.042", InstGmosCommon.calcWavelength(d, fs, 42.0))
    }
  }

  // When using a disperser and the central wavelength is 0, we don't have
  // an observing wavelength.
  @Test def testCentralWavelength0() {
    assertNull(InstGmosCommon.calcWavelength(DisperserNorth.B1200_G5301, FilterNorth.CaT_G0309, 0.0))
    assertNull(InstGmosCommon.calcWavelength(DisperserSouth.B600_G5323,  FilterSouth.CaT_G0333, 0.0))
  }
}