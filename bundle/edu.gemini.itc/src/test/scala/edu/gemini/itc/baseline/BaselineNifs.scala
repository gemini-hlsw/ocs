package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.{IfuRadial, IfuSingle, IfuSummed, NifsParameters}
import edu.gemini.spModel.gemini.nifs.NIFSParams
import squants.space.LengthConversions._

/**
 * NIFS baseline test fixtures.
 *
 */
object BaselineNifs {

  lazy val Fixtures = KBandSpectroscopy

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.K,
      NIFSParams.ReadMode.FAINT_OBJECT_SPEC,
      2.1.microns,                  // central wavelength
      IfuSingle(0.0),               // IFU method
      Fixture.AltairNgsFL
    ),
    NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.K_SHORT,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      2.1.microns,                  // central wavelength
      IfuSingle(0.5),               // IFU method
      Fixture.AltairNgs
    ),
    NifsParameters(
      NIFSParams.Filter.HK_FILTER,
      NIFSParams.Disperser.K_LONG,
      NIFSParams.ReadMode.BRIGHT_OBJECT_SPEC,
      2.1.microns,                  // central wavelength
      IfuSingle(1.0),               // IFU method
      Fixture.AltairLgs
    ),


    NifsParameters(
      NIFSParams.Filter.JH_FILTER,
      NIFSParams.Disperser.J,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      1.9.microns,                  // central wavelength
      IfuSummed(2, 5, 0.0, 0.0),
      Fixture.AltairNgsFL
    ),
    NifsParameters(
      NIFSParams.Filter.JH_FILTER,
      NIFSParams.Disperser.H,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      1.9.microns,                  // central wavelength
      IfuSummed(2, 5, 0.0, 0.1),
      Fixture.AltairNgs
    ),
    NifsParameters(
      NIFSParams.Filter.JH_FILTER,
      NIFSParams.Disperser.J,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      1.9.microns,                // central wavelength
      IfuSummed(2, 5, 0.1, 0.1),
      Fixture.AltairLgs
    ),


    NifsParameters(
      NIFSParams.Filter.ZJ_FILTER,
      NIFSParams.Disperser.Z,
      NIFSParams.ReadMode.FAINT_OBJECT_SPEC,
      1.2.microns,                // central wavelength
      IfuRadial(0.0, 0.0),
      Fixture.AltairNgsFL
    ),
    NifsParameters(
      NIFSParams.Filter.ZJ_FILTER,
      NIFSParams.Disperser.Z,
      NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC,
      1.2.microns,                // central wavelength
      IfuRadial(0.0, 1.0),
      Fixture.AltairNgs
    ),
    NifsParameters(
      NIFSParams.Filter.ZJ_FILTER,
      NIFSParams.Disperser.Z,
      NIFSParams.ReadMode.BRIGHT_OBJECT_SPEC,
      1.2.microns,                // central wavelength
      IfuRadial(1.0, 1.0),
      Fixture.AltairLgs
    )


  ))

}
