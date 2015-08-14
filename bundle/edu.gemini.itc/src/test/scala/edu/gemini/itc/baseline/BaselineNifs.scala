package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.nifs.NifsParameters
import edu.gemini.spModel.core.Wavelength
import edu.gemini.spModel.gemini.nifs.NIFSParams

/**
 * NIFS baseline test fixtures.
 *
 */
object BaselineNifs {

  lazy val Fixtures = KBandSpectroscopy

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.K,
      NifsParameters.LOW_READ_NOISE,
      Wavelength.fromMicrons(2.1),// central wavelength
      NifsParameters.SINGLE_IFU,// IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "0",                      // num X
      "0",                      // num Y
      "0.0",                    // center x
      "0.0",                    // center y
      Fixture.AltairNgsFL
    ),
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.K,
      NifsParameters.LOW_READ_NOISE,
      Wavelength.fromMicrons(2.1),// central wavelength
      NifsParameters.SINGLE_IFU,// IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "0",                      // num X
      "0",                      // num Y
      "0.0",                    // center x
      "0.0",                    // center y
      Fixture.AltairNgs
    ),
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.K,
      NifsParameters.LOW_READ_NOISE,
      Wavelength.fromMicrons(2.1),// central wavelength
      NifsParameters.SINGLE_IFU,// IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "0",                      // num X
      "0",                      // num Y
      "0.0",                    // center x
      "0.0",                     // center y
      Fixture.AltairLgs
    ),


    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.H,
      NifsParameters.LOW_READ_NOISE,
      Wavelength.fromMicrons(2.2),// central wavelength
      NifsParameters.SUMMED_APERTURE_IFU, // IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "2",                      // num X
      "5",                      // num Y
      "0.0",                    // center x
      "0.0",                    // center y
      Fixture.AltairNgsFL
    ),
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.H,
      NifsParameters.LOW_READ_NOISE,
      Wavelength.fromMicrons(2.2),// central wavelength
      NifsParameters.SUMMED_APERTURE_IFU, // IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "2",                      // num X
      "5",                      // num Y
      "0.0",                    // center x
      "0.0",                    // center y
      Fixture.AltairNgs
    ),
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.H,
      NifsParameters.LOW_READ_NOISE,
      Wavelength.fromMicrons(2.2),// central wavelength
      NifsParameters.SUMMED_APERTURE_IFU, // IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "2",                      // num X
      "5",                      // num Y
      "0.0",                    // center x
      "0.0",                    // center y
      Fixture.AltairLgs
    ),


    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.H,
      NifsParameters.LOW_READ_NOISE,
      Wavelength.fromMicrons(2.2),// central wavelength
      NifsParameters.RADIAL_IFU,// IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "0",                      // num X
      "0",                      // num Y
      "0.0",                    // center x
      "1.0",                    // center y
      Fixture.AltairNgsFL
    ),
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.H,
      NifsParameters.LOW_READ_NOISE,
      Wavelength.fromMicrons(2.2),// central wavelength
      NifsParameters.RADIAL_IFU,// IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "0",                      // num X
      "0",                      // num Y
      "0.0",                    // center x
      "1.0",                    // center y
      Fixture.AltairNgs
    ),
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.H,
      NifsParameters.LOW_READ_NOISE,
      Wavelength.fromMicrons(2.2),// central wavelength
      NifsParameters.RADIAL_IFU,// IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "0",                      // num X
      "0",                      // num Y
      "0.0",                    // center x
      "1.0",                    // center y
      Fixture.AltairLgs
    )


  ))

}
