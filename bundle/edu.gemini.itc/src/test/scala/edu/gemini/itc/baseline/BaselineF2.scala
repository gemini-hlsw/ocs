package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.flamingos2.Flamingos2Parameters

/**
 * F2 baseline test fixtures.
 */
object BaselineF2 {

  lazy val Fixtures = KBandImaging ++ KBandSpectroscopy

  private lazy val KBandImaging = Fixture.kBandImgFixtures(List(
    new Flamingos2Parameters(
      "Open",                             // filter
      Flamingos2Parameters.NOGRISM,       // grism
      "none",                             // FP mask
      "lowNoise"),                        // read noise
    new Flamingos2Parameters(
      "H_G0803",                          // filter
      Flamingos2Parameters.NOGRISM,       // grism
      "none",                             // FP mask
      "medNoise"),                        // read noise
    new Flamingos2Parameters(
      "Jlow_G0801",                       // filter
      Flamingos2Parameters.NOGRISM,       // grism
      "none",                             // FP mask
      "highNoise")                        // read noise
  ))

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new Flamingos2Parameters(
      "Jlow_G0801",
      "JH_G5801",
      "1",
      "lowNoise"),
    new Flamingos2Parameters(
      "H_G0803",
      "HK_G5802",
      "4",
      "medNoise"),
    new Flamingos2Parameters(
      "H_G0803",
      "R3K_G5803",
      "8",
      "highNoise")
  ))

}
