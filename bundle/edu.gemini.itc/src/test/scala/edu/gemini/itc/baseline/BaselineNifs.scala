package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.nifs.NifsParameters
import edu.gemini.itc.shared.{IfuRadial, IfuSummed, IfuSingle}
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
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      Wavelength.fromMicrons(2.1),  // central wavelength
      IfuSingle(0),                 // IFU method
      Fixture.AltairNgsFL
    ),
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.K,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      Wavelength.fromMicrons(2.1),  // central wavelength
      IfuSingle(0),                 // IFU method
      Fixture.AltairNgs
    ),
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.K,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      Wavelength.fromMicrons(2.1),  // central wavelength
      IfuSingle(0),                 // IFU method
      Fixture.AltairLgs
    ),


    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.H,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      Wavelength.fromMicrons(2.2),  // central wavelength
      IfuSummed(2, 5, 0, 0),
      Fixture.AltairNgsFL
    ),
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.H,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      Wavelength.fromMicrons(2.2),  // central wavelength
      IfuSummed(2, 5, 0, 0),
      Fixture.AltairNgs
    ),
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.H,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      Wavelength.fromMicrons(2.2),  // central wavelength
      IfuSummed(2, 5, 0, 0),
      Fixture.AltairLgs
    ),


    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.H,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      Wavelength.fromMicrons(2.2),  // central wavelength
      IfuRadial(0, 0),
      Fixture.AltairNgsFL
    ),
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.H,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      Wavelength.fromMicrons(2.2),  // central wavelength
      IfuRadial(0, 0),
      Fixture.AltairNgs
    ),
    new NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.H,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      Wavelength.fromMicrons(2.2),  // central wavelength
      IfuRadial(0, 0),
      Fixture.AltairLgs
    )


  ))

}
