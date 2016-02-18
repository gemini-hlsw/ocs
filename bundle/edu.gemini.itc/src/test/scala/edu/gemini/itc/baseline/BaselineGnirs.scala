package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.GnirsParameters
import edu.gemini.spModel.gemini.gnirs.GNIRSParams._
import edu.gemini.spModel.core.WavelengthConversions._

/**
 * GNIRS baseline test fixtures.
 */
object BaselineGnirs {

  lazy val Fixtures = KBandSpectroscopy ++ KBandImaging

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new GnirsParameters(
      PixelScale.PS_005,
      None,
      Some(Disperser.D_10),
      ReadMode.FAINT,
      CrossDispersed.LXD,
      2.4.microns,
      SlitWidth.SW_1,
      Fixture.NoAltair),

    new GnirsParameters(
      PixelScale.PS_015,
      None,
      Some(Disperser.D_111),
      ReadMode.VERY_BRIGHT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.SW_3,
      Fixture.NoAltair),

    new GnirsParameters(
      PixelScale.PS_005,
      None,
      Some(Disperser.D_32),
      ReadMode.FAINT,
      CrossDispersed.LXD,
      2.6.microns,
      SlitWidth.SW_6,
      Fixture.NoAltair),

    new GnirsParameters(
      PixelScale.PS_015,
      None,
      Some(Disperser.D_32),
      ReadMode.VERY_BRIGHT,
      CrossDispersed.NO,
      2.6.microns,
      SlitWidth.SW_8,
      Fixture.NoAltair)

  ))

  private lazy val KBandImaging = Fixture.kBandImgFixtures(List(
    new GnirsParameters(
      PixelScale.PS_005,
      Some(Filter.K),
      None,
      ReadMode.FAINT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.ACQUISITION,
      Fixture.NoAltair),

    new GnirsParameters(
      PixelScale.PS_005,
      Some(Filter.J),
      None,
      ReadMode.BRIGHT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.ACQUISITION,
      Fixture.AltairNgs),

    new GnirsParameters(
      PixelScale.PS_005,
      Some(Filter.Y),
      None,
      ReadMode.VERY_FAINT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.ACQUISITION,
      Fixture.AltairLgs),



    new GnirsParameters(
      PixelScale.PS_015,
      Some(Filter.ORDER_4),
      None,
      ReadMode.FAINT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.ACQUISITION,
      Fixture.AltairNgsFL),

    new GnirsParameters(
      PixelScale.PS_015,
      Some(Filter.K),
      None,
      ReadMode.VERY_BRIGHT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.ACQUISITION,
      Fixture.AltairNgs),

    new GnirsParameters(
      PixelScale.PS_015,
      Some(Filter.K),
      None,
      ReadMode.FAINT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.ACQUISITION,
      Fixture.AltairLgs)
  ))


}
