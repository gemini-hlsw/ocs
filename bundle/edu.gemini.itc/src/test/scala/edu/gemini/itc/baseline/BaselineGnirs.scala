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
      None,
      WellDepth.SHALLOW,
      Fixture.NoAltair),

    new GnirsParameters(
      PixelScale.PS_015,
      None,
      Some(Disperser.D_111),
      ReadMode.VERY_BRIGHT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.SW_3,
      None,
      WellDepth.SHALLOW,
      Fixture.NoAltair),

    new GnirsParameters(
      PixelScale.PS_005,
      None,
      Some(Disperser.D_32),
      ReadMode.FAINT,
      CrossDispersed.LXD,
      2.6.microns,
      SlitWidth.SW_6,
      None,
      WellDepth.SHALLOW,
      Fixture.NoAltair),

    new GnirsParameters(
      PixelScale.PS_015,
      None,
      Some(Disperser.D_32),
      ReadMode.VERY_BRIGHT,
      CrossDispersed.NO,
      2.6.microns,
      SlitWidth.SW_8,
      None,
      WellDepth.SHALLOW,
      Fixture.NoAltair),

  new GnirsParameters(
    PixelScale.PS_005,
    None,
    Some(Disperser.D_32),
    ReadMode.BRIGHT,
    CrossDispersed.NO,
    2.4.microns,
    SlitWidth.SW_7,
    None,
    WellDepth.DEEP,
    Fixture.AltairNgs),

  new GnirsParameters(
    PixelScale.PS_005,
    None,
    Some(Disperser.D_111),
    ReadMode.VERY_FAINT,
    CrossDispersed.LXD,
    1.4.microns,
    SlitWidth.SW_1,
    None,
    WellDepth.SHALLOW,
    Fixture.AltairLgs),


  new GnirsParameters(
    PixelScale.PS_015,
    None,
    Some(Disperser.D_111),
    ReadMode.FAINT,
    CrossDispersed.NO,
    1.8.microns,
    SlitWidth.SW_3,
    None,
    WellDepth.SHALLOW,
    Fixture.AltairNgsFL),

  new GnirsParameters(
    PixelScale.PS_015,
    None,
    Some(Disperser.D_32),
    ReadMode.VERY_BRIGHT,
    CrossDispersed.NO,
    2.9.microns,
    SlitWidth.SW_5,
    None,
    WellDepth.SHALLOW,
    Fixture.AltairNgs),

  new GnirsParameters(
    PixelScale.PS_015,
    None,
    Some(Disperser.D_111),
    ReadMode.FAINT,
    CrossDispersed.SXD,
    2.0.microns,
    SlitWidth.SW_5,
    None,
    WellDepth.DEEP,
    Fixture.AltairLgs)

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
      None,
      WellDepth.SHALLOW,
      Fixture.NoAltair),

    new GnirsParameters(
      PixelScale.PS_005,
      Some(Filter.J),
      None,
      ReadMode.BRIGHT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.ACQUISITION,
      None,
      WellDepth.SHALLOW,
      Fixture.AltairNgs),

    new GnirsParameters(
      PixelScale.PS_005,
      Some(Filter.Y),
      None,
      ReadMode.VERY_FAINT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.ACQUISITION,
      None,
      WellDepth.SHALLOW,
      Fixture.AltairLgs),



    new GnirsParameters(
      PixelScale.PS_015,
      Some(Filter.ORDER_4),
      None,
      ReadMode.FAINT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.ACQUISITION,
      None,
      WellDepth.SHALLOW,
      Fixture.AltairNgsFL),

    new GnirsParameters(
      PixelScale.PS_015,
      Some(Filter.K),
      None,
      ReadMode.VERY_BRIGHT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.ACQUISITION,
      None,
      WellDepth.SHALLOW,
      Fixture.AltairNgs),

    new GnirsParameters(
      PixelScale.PS_015,
      Some(Filter.K),
      None,
      ReadMode.FAINT,
      CrossDispersed.NO,
      2.4.microns,
      SlitWidth.ACQUISITION,
      None,
      WellDepth.SHALLOW,
      Fixture.AltairLgs)
  ))


}
