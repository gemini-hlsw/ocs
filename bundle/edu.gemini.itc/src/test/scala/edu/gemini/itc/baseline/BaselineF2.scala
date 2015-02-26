package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.flamingos2.{Flamingos2Parameters, Flamingos2Recipe}

/**
 * F2 baseline test fixtures.
 */
object BaselineF2 {

  lazy val Fixtures = KBandImaging ++ KBandSpectroscopy

  def executeRecipe(f: Fixture[Flamingos2Parameters]): Output =
    cookRecipe(w => new Flamingos2Recipe(f.src, f.odp, f.ocp, f.ins, f.tep, f.pdp, w))

  private lazy val KBandImaging = Fixture.kBandImgFixtures(List(
    new Flamingos2Parameters(
      "H_G0803",                          // filter
      Flamingos2Parameters.NOGRISM,       // grism
      "none",                             // FP mask
      "lowNoise")                         // read noise
  ))

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new Flamingos2Parameters(
      "H_G0803",
      "R3K_G5803",
      "1",
      "medNoise")
  ))

}
