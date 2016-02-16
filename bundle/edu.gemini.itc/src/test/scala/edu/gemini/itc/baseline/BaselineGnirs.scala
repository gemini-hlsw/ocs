package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.GnirsParameters
import edu.gemini.spModel.gemini.gnirs.GNIRSParams._
import edu.gemini.spModel.core.WavelengthConversions._

/**
 * GNIRS baseline test fixtures.
 */
object BaselineGnirs {

  lazy val Fixtures = KBandSpectroscopy

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new GnirsParameters(
      PixelScale.PS_005,
      null,
      Disperser.D_10,
      ReadMode.FAINT,
      CrossDispersed.LXD,
      2.4.microns,
      SlitWidth.SW_1,
      Fixture.NoAltair),

    new GnirsParameters(
      PixelScale.PS_015,
      null,
      Disperser.D_111,
      ReadMode.VERY_BRIGHT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.SW_3,
      Fixture.NoAltair),

    new GnirsParameters(
      PixelScale.PS_005,
      null,
      Disperser.D_32,
      ReadMode.FAINT,
      CrossDispersed.LXD,
      2.6.microns,
      SlitWidth.SW_6,
      Fixture.NoAltair),

    new GnirsParameters(
      PixelScale.PS_015,
      null,
      Disperser.D_32,
      ReadMode.VERY_BRIGHT,
      CrossDispersed.NO,
      2.6.microns,
      SlitWidth.SW_8,
      Fixture.NoAltair)

  ))


}
