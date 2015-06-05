package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.gnirs.GnirsParameters
import edu.gemini.spModel.core.Wavelength
import edu.gemini.spModel.gemini.gnirs.GNIRSParams._

/**
 * GNIRS baseline test fixtures.
 */
object BaselineGnirs {

  lazy val Fixtures = KBandSpectroscopy

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new GnirsParameters(
      PixelScale.PS_005,
      Disperser.D_10,
      ReadMode.FAINT,
      GnirsParameters.X_DISP_ON,
      Wavelength.fromMicrons(2.4),
      SlitWidth.SW_1),

    new GnirsParameters(
      PixelScale.PS_015,
      Disperser.D_111,
      ReadMode.VERY_BRIGHT,
      GnirsParameters.X_DISP_OFF,
      Wavelength.fromMicrons(2.4),
      SlitWidth.SW_3),

    new GnirsParameters(
      PixelScale.PS_005,
      Disperser.D_32,
      ReadMode.FAINT,
      GnirsParameters.X_DISP_ON,
      Wavelength.fromMicrons(2.6),
      SlitWidth.SW_6),

    new GnirsParameters(
      PixelScale.PS_015,
      Disperser.D_32,
      ReadMode.VERY_BRIGHT,
      GnirsParameters.X_DISP_OFF,
      Wavelength.fromMicrons(2.6),
      SlitWidth.SW_8)

  ))


}
