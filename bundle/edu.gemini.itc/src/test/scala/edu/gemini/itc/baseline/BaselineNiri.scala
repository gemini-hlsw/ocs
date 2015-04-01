package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.niri.NiriParameters

/**
 * NIRI baseline test fixtures.
 */
object BaselineNiri {

  lazy val Fixtures = KBandImaging ++ KBandSpectroscopy

  private lazy val KBandImaging = Fixture.kBandImgFixtures(List(
    new NiriParameters(
      "J",
      "none",
      NiriParameters.F14,
      NiriParameters.HIGH_READ_NOISE,
      NiriParameters.HIGH_WELL_DEPTH,
      NiriParameters.NO_SLIT),
    new NiriParameters(
      "K",
      "none",
      NiriParameters.F32,
      NiriParameters.HIGH_READ_NOISE,
      NiriParameters.HIGH_WELL_DEPTH,
      NiriParameters.NO_SLIT)
  ), alt = Fixture.AltairConfigurations)

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new NiriParameters(
      "K",
      "K-grism",
      NiriParameters.F6,                      // only F6 is supported in spectroscopy mode
      NiriParameters.LOW_READ_NOISE,
      NiriParameters.LOW_WELL_DEPTH,
      NiriParameters.SLIT_2_PIX_CENTER)
  ), alt = Fixture.NoAltair)

}
