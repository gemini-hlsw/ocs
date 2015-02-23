package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.gems.GemsParameters
import edu.gemini.itc.gsaoi.{GsaoiParameters, GsaoiRecipe}

/**
 * GSAOI baseline test fixtures.
 */
object BaselineGsaoi  {

  lazy val Fixtures = KBandSpectroscopy

  def executeRecipe(f: Fixture[GsaoiParameters]): Output =
    cookRecipe(w => new GsaoiRecipe(f.src, f.odp, f.ocp, f.ins, f.tep, f.gem.get, w))

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new GsaoiParameters(
      "Z_G1101",                                    //String Filter,
      GsaoiParameters.INSTRUMENT_CAMERA,            //String camera,
      GsaoiParameters.BRIGHT_OBJECTS_READ_MODE      //String read mode,
    )
  ), gem = Gems)

  private lazy val Gems = List(
    new GemsParameters(
      0.3,
      "K"
    )
  )

}
