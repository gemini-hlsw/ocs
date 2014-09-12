package edu.gemini.spModel.gemini.nici

import InstNICI.calcWavelength
import NICIParams.{ImagingMode, Channel1FW, Channel2FW}

import org.junit.Test
import org.junit.Assert._

/**
 * Test cases for the observing wavelength calculation.
 */
class ObsWavelengthCalcTest {

  // Regardless of explicit red or blue channel FW, the imaging mode takes
  // precendence.
  @Test def testImagingMode() {
    val wls = for {
      red  <- Channel1FW.values
      blue <- Channel2FW.values
    } yield calcWavelength(ImagingMode.H1SLA, red, blue)

    val meta = NICIParams.ImagingModeMetaconfig.getMetaConfig(ImagingMode.H1SLA);
    wls.foreach(wl => assertEquals(meta.getChannel1Fw.centralWavelength, wl, 0.000001))
  }

  // Explicit red takes precendece when in manual mode.
  @Test def testRed() {
    val wls = for {
      blue <- Channel2FW.values
    } yield calcWavelength(ImagingMode.MANUAL, Channel1FW.KS, blue)

    wls.foreach(wl => assertEquals(Channel1FW.KS.centralWavelength, wl, 0.000001))
  }

  // Explicit blue is used when all else fails.
  @Test def testBlue() {
    val wl = calcWavelength(ImagingMode.MANUAL, Channel1FW.BLOCK, Channel2FW.J)
    assertEquals(Channel2FW.J.centralWavelength, wl, 0.000001)
  }

  // When there is nothing to go by, use the default.
  @Test def testDefault() {
    val wl = calcWavelength(ImagingMode.MANUAL, Channel1FW.BLOCK, Channel2FW.BLOCK)
    assertEquals(InstNICI.DEF_CENTRAL_WAVELENGTH, wl, 0.000001);
  }
}