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

  private lazy val KBandSpectroscopy = Fixture.kBandIfuFixtures(List(
    NifsParameters(
      Filter.HK_FILTER,
      Disperser.K,
      ReadMode.FAINT_OBJECT_SPEC,
      2.1.microns,                  // central wavelength
      Fixture.AltairNgsFL
    ),
    NifsParameters(
      Filter.HK_FILTER,
      Disperser.K_SHORT,
      ReadMode.MEDIUM_OBJECT_SPEC,
      2.1.microns,                  // central wavelength
      Fixture.AltairNgs
    ),
    NifsParameters(
      Filter.JH_FILTER,
      Disperser.H,
      ReadMode.MEDIUM_OBJECT_SPEC,
      1.9.microns,                  // central wavelength
      Fixture.AltairNgs
    ),
    NifsParameters(
      Filter.JH_FILTER,
      Disperser.J,
      ReadMode.MEDIUM_OBJECT_SPEC,
      1.9.microns,                // central wavelength
      Fixture.AltairLgs
    ),
    NifsParameters(
      Filter.ZJ_FILTER,
      Disperser.Z,
      ReadMode.MEDIUM_OBJECT_SPEC,
      1.2.microns,                // central wavelength
      Fixture.AltairNgs
    ),
    NifsParameters(
      Filter.ZJ_FILTER,
      Disperser.Z,
      ReadMode.BRIGHT_OBJECT_SPEC,
      1.2.microns,                // central wavelength
      Fixture.AltairLgs
    )

  ))

}
