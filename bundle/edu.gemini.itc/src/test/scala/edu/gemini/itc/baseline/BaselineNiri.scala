package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.NiriParameters
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
      ReadMode.IMAG_SPEC_3TO5,
      WellDepth.DEEP,
      Mask.MASK_IMAGING,
      Fixture.AltairNgsFL),

    new NiriParameters(
      Filter.BBF_J,
      Disperser.NONE,
      Camera.F14,
      ReadMode.IMAG_SPEC_3TO5,
      WellDepth.DEEP,
      Mask.MASK_IMAGING,
      Fixture.AltairNgs),

    new NiriParameters(
      Filter.BBF_J,
      Disperser.NONE,
      Camera.F14,
      ReadMode.IMAG_SPEC_3TO5,
      WellDepth.DEEP,
      Mask.MASK_IMAGING,
      Fixture.AltairLgs),



    new NiriParameters(
      Filter.BBF_K,
      Disperser.NONE,
      Camera.F32,
      ReadMode.IMAG_SPEC_3TO5,
      WellDepth.DEEP,
      Mask.MASK_IMAGING,
      Fixture.AltairNgsFL),

    new NiriParameters(
      Filter.BBF_K,
      Disperser.NONE,
      Camera.F32,
      ReadMode.IMAG_SPEC_3TO5,
      WellDepth.DEEP,
      Mask.MASK_IMAGING,
      Fixture.AltairNgs),

    new NiriParameters(
      Filter.BBF_K,
      Disperser.NONE,
      Camera.F32,
      ReadMode.IMAG_SPEC_3TO5,
      WellDepth.DEEP,
      Mask.MASK_IMAGING,
      Fixture.AltairLgs)

  ))

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new NiriParameters(
      Filter.BBF_K,
      Disperser.K,
      Camera.F6,                        // ITC supports only F6 in spectroscopy mode
      ReadMode.IMAG_SPEC_NB,
      WellDepth.SHALLOW,
      Mask.MASK_1,
      Fixture.NoAltair)
  ))

}
