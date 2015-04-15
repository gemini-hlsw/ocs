package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.gems.GemsParameters
import edu.gemini.itc.gsaoi.GsaoiParameters
import edu.gemini.spModel.gemini.gsaoi.Gsaoi

/**
 * GSAOI baseline test fixtures.
 */
object BaselineGsaoi  {

  lazy val Fixtures = KBandImaging ++ RBandImaging

  private lazy val KBandImaging = Fixture.kBandImgFixtures(List(
    new GsaoiParameters(
      Gsaoi.Filter.Z,
      Gsaoi.ReadMode.BRIGHT
    )
  ), gem = Gems)

  private lazy val RBandImaging = Fixture.rBandImgFixtures(List(
    new GsaoiParameters(
      Gsaoi.Filter.J,
      Gsaoi.ReadMode.FAINT
    )
  ), gem = Gems)

  private lazy val Gems = List(
    new GemsParameters(
      0.3,
      "K"
    )
  )

}
