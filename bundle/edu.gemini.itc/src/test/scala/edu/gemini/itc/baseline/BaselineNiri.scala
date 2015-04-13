package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.niri.NiriParameters
import edu.gemini.spModel.gemini.niri.Niri._

/**
 * NIRI baseline test fixtures.
 */
object BaselineNiri {

  lazy val Fixtures = KBandImaging ++ KBandSpectroscopy

  private lazy val KBandImaging = Fixture.kBandImgFixtures(List(
    new NiriParameters(
      Filter.BBF_J,
      Disperser.NONE,
      Camera.F14,
      NiriParameters.HIGH_READ_NOISE,
      NiriParameters.HIGH_WELL_DEPTH,
      Mask.MASK_IMAGING),
    new NiriParameters(
      Filter.BBF_K,
      Disperser.NONE,
      Camera.F32,
      NiriParameters.HIGH_READ_NOISE,
      NiriParameters.HIGH_WELL_DEPTH,
      Mask.MASK_IMAGING)
  ), alt = Fixture.AltairConfigurations)

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new NiriParameters(
      Filter.BBF_K,
      Disperser.K,
      Camera.F6,                        // ITC supports only F6 in spectroscopy mode
      NiriParameters.LOW_READ_NOISE,
      NiriParameters.LOW_WELL_DEPTH,
      Mask.MASK_1)
  ), alt = Fixture.NoAltair)

}
