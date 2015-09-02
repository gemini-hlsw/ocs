package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.{IfuRadial, IfuSingle, IfuSummed, NifsParameters}
import edu.gemini.spModel.gemini.nifs.NIFSParams._
import edu.gemini.spModel.core.WavelengthConversions._

/**
 * NIFS baseline test fixtures.
 *
 */
object BaselineNifs {

  lazy val Fixtures = KBandSpectroscopy

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    NifsParameters(
      Filter.HK_FILTER,
      Disperser.K,
      ReadMode.FAINT_OBJECT_SPEC,
      2.1.microns,                  // central wavelength
      IfuSingle(0.0),               // IFU method
      Fixture.AltairNgsFL
    ),
    NifsParameters(
      Filter.HK_FILTER,
      Disperser.K_SHORT,
      ReadMode.MEDIUM_OBJECT_SPEC,
      2.1.microns,                  // central wavelength
      IfuSingle(0.5),               // IFU method
      Fixture.AltairNgs
    ),
    NifsParameters(
      Filter.HK_FILTER,
      Disperser.K_LONG,
      ReadMode.BRIGHT_OBJECT_SPEC,
      2.1.microns,                  // central wavelength
      IfuSingle(1.0),               // IFU method
      Fixture.AltairLgs
    ),


    NifsParameters(
      Filter.JH_FILTER,
      Disperser.J,
      ReadMode.MEDIUM_OBJECT_SPEC,
      1.9.microns,                  // central wavelength
      IfuSummed(2, 5, 0.0, 0.0),
      Fixture.AltairNgsFL
    ),
    NifsParameters(
      Filter.JH_FILTER,
      Disperser.H,
      ReadMode.MEDIUM_OBJECT_SPEC,
      1.9.microns,                  // central wavelength
      IfuSummed(2, 5, 0.0, 0.1),
      Fixture.AltairNgs
    ),
    NifsParameters(
      Filter.JH_FILTER,
      Disperser.J,
      ReadMode.MEDIUM_OBJECT_SPEC,
      1.9.microns,                // central wavelength
      IfuSummed(2, 5, 0.1, 0.1),
      Fixture.AltairLgs
    ),


    NifsParameters(
      Filter.ZJ_FILTER,
      Disperser.Z,
      ReadMode.FAINT_OBJECT_SPEC,
      1.2.microns,                // central wavelength
      IfuRadial(0.0, 0.0),
      Fixture.AltairNgsFL
    ),
    NifsParameters(
      Filter.ZJ_FILTER,
      Disperser.Z,
      ReadMode.MEDIUM_OBJECT_SPEC,
      1.2.microns,                // central wavelength
      IfuRadial(0.0, 1.0),
      Fixture.AltairNgs
    ),
    NifsParameters(
      Filter.ZJ_FILTER,
      Disperser.Z,
      ReadMode.BRIGHT_OBJECT_SPEC,
      1.2.microns,                // central wavelength
      IfuRadial(1.0, 1.0),
      Fixture.AltairLgs
    )

  ))

}
