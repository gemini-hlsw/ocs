package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.NiriParameters
import edu.gemini.spModel.gemini.niri.Niri._

/**
 * NIRI baseline test fixtures.
 */
object BaselineNiri {

  lazy val Fixtures: List[Fixture[NiriParameters]] = KBandImaging ++ KBandSpectroscopy

  private lazy val KBandImaging = Fixture.kBandImgFixtures(List(
    NiriParameters(
      Filter.BBF_J,
      Disperser.NONE,
      Camera.F14,
      ReadMode.IMAG_SPEC_3TO5,
      WellDepth.DEEP,
      Mask.MASK_IMAGING,
      BuiltinROI.FULL_FRAME,
      Fixture.AltairNgsFL),

    NiriParameters(
      Filter.BBF_J,
      Disperser.NONE,
      Camera.F14,
      ReadMode.IMAG_SPEC_3TO5,
      WellDepth.DEEP,
      Mask.MASK_IMAGING,
      BuiltinROI.FULL_FRAME,
      Fixture.AltairNgs),

    NiriParameters(
      Filter.BBF_J,
      Disperser.NONE,
      Camera.F14,
      ReadMode.IMAG_SPEC_3TO5,
      WellDepth.DEEP,
      Mask.MASK_IMAGING,
      BuiltinROI.CENTRAL_256,
      Fixture.AltairLgs),



    NiriParameters(
      Filter.BBF_K,
      Disperser.NONE,
      Camera.F32,
      ReadMode.IMAG_SPEC_3TO5,
      WellDepth.DEEP,
      Mask.MASK_IMAGING,
      BuiltinROI.CENTRAL_512,
      Fixture.AltairNgsFL),

    NiriParameters(
      Filter.BBF_K,
      Disperser.NONE,
      Camera.F32,
      ReadMode.IMAG_SPEC_3TO5,
      WellDepth.DEEP,
      Mask.MASK_IMAGING,
      BuiltinROI.CENTRAL_768,
      Fixture.AltairNgs),

    NiriParameters(
      Filter.BBF_K,
      Disperser.NONE,
      Camera.F32,
      ReadMode.IMAG_SPEC_3TO5,
      WellDepth.DEEP,
      Mask.MASK_IMAGING,
      BuiltinROI.FULL_FRAME,
      Fixture.AltairLgs)

  ))

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    NiriParameters(
      Filter.BBF_K,
      Disperser.K,
      Camera.F6, // ITC supports only F6 in spectroscopy mode
      ReadMode.IMAG_SPEC_NB,
      WellDepth.SHALLOW,
      Mask.MASK_1,
      BuiltinROI.FULL_FRAME,
      Fixture.NoAltair)
  ))

}
