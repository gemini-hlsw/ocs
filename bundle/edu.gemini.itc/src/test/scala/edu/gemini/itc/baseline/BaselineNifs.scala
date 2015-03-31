package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.nifs.{NifsParameters, NifsRecipe}

/**
 * NIFS baseline test fixtures.
 */
object BaselineNifs {

  lazy val Fixtures = KBandSpectroscopy

  def executeRecipe(f: Fixture[NifsParameters]): Output =
    cookRecipe(w => new NifsRecipe(f.src, f.odp, f.ocp, f.ins, f.tep, f.alt.get, f.pdp, w))

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new NifsParameters(
      NifsParameters.HK_G0603,
      NifsParameters.K_G5605,
      NifsParameters.LOW_READ_NOISE,
      "2.1",
      NifsParameters.SINGLE_IFU,// IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "0",                      // num X
      "0",                      // num Y
      "0.0",                    // center x
      "0.0"                     // center y
    ),
    new NifsParameters(
      NifsParameters.HK_G0603,
      NifsParameters.H_G5604,
      NifsParameters.LOW_READ_NOISE,
      "2.2",                    // central wavelength
      NifsParameters.SUMMED_APERTURE_IFU, // IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "2",                      // num X
      "5",                      // num Y
      "0.0",                    // center x
      "0.0"                     // center y
    ),
    new NifsParameters(
      NifsParameters.HK_G0603,
      NifsParameters.H_G5604,
      NifsParameters.LOW_READ_NOISE,
      "2.2",                    // central wavelength
      NifsParameters.RADIAL_IFU,// IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "0",                      // num X
      "0",                      // num Y
      "0.0",                    // center x
      "1.0"                     // center y
    )
  ), alt = Fixture.AltairConfigurations)

}
