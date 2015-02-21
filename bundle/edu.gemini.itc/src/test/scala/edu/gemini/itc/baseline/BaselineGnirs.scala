package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.gnirs.{GnirsParameters, GnirsRecipe}

/**
 * GNIRS baseline test fixtures.
 */
object BaselineGnirs {

  lazy val Fixtures = KBandSpectroscopy

  def executeRecipe(f: Fixture[GnirsParameters]): Output =
    cookRecipe(w => new GnirsRecipe(f.src, f.odp, f.ocp, f.ins, f.tep, f.pdp, w))

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new GnirsParameters(
      GnirsParameters.LONG_CAMERA,
      GnirsParameters.G10,
      GnirsParameters.LOW_READ_NOISE,
      GnirsParameters.X_DISP_ON,
      "4.7",
      "2.4",
      GnirsParameters.SLIT0_1),

    new GnirsParameters(
      GnirsParameters.SHORT_CAMERA,
      GnirsParameters.G110,
      GnirsParameters.HIGH_READ_NOISE,
      GnirsParameters.X_DISP_OFF,
      "4.7",
      "2.4",
      GnirsParameters.SLIT0_2),

    new GnirsParameters(
      GnirsParameters.LONG_CAMERA,
      GnirsParameters.G32,
      GnirsParameters.LOW_READ_NOISE,
      GnirsParameters.X_DISP_ON,
      "4.7",
      "2.6",
      GnirsParameters.SLIT0_675),

    new GnirsParameters(
      GnirsParameters.SHORT_CAMERA,
      GnirsParameters.G32,
      GnirsParameters.HIGH_READ_NOISE,
      GnirsParameters.X_DISP_OFF,
      "4.7",
      "2.6",
      GnirsParameters.SLIT3_0)

  ))


}
