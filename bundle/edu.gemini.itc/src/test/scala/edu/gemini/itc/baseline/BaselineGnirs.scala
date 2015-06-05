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
      GnirsParameters.LOW_READ_NOISE,
      GnirsParameters.X_DISP_ON,
      Wavelength.fromMicrons(2.4),
      GnirsParameters.SLIT0_1),

    new GnirsParameters(
      PixelScale.PS_015,
      Disperser.D_111,
      GnirsParameters.HIGH_READ_NOISE,
      GnirsParameters.X_DISP_OFF,
      Wavelength.fromMicrons(2.4),
      GnirsParameters.SLIT0_2),

    new GnirsParameters(
      PixelScale.PS_005,
      Disperser.D_32,
      GnirsParameters.LOW_READ_NOISE,
      GnirsParameters.X_DISP_ON,
      Wavelength.fromMicrons(2.6),
      GnirsParameters.SLIT0_675),

    new GnirsParameters(
      PixelScale.PS_015,
      Disperser.D_32,
      GnirsParameters.HIGH_READ_NOISE,
      GnirsParameters.X_DISP_OFF,
      Wavelength.fromMicrons(2.6),
      GnirsParameters.SLIT3_0)

  ))


}
