package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.gmos.{GmosParameters, GmosRecipe}
import edu.gemini.spModel.gemini.gmos.GmosNorthType.{DisperserNorth, FilterNorth}
import edu.gemini.spModel.gemini.gmos.GmosSouthType.{DisperserSouth, FilterSouth}

/**
 * GMOS baseline test fixtures.
 */
object BaselineGmos {


  lazy val Fixtures = RBandImaging ++ KBandSpectroscopy

  def executeRecipe(f: Fixture[GmosParameters]): Output =
    cookRecipe(w => new GmosRecipe(f.src, f.odp, f.ocp, f.ins, f.tep, f.pdp, w))

  private lazy val RBandImaging = Fixture.rBandImgFixtures(List(
    new GmosParameters(
      FilterNorth.i_G0302,
      DisperserNorth.MIRROR,
      500.0,                      // wavelength
      GmosParameters.NO_SLIT,
      1,
      1,
      "",                         // IFU method
      0.0,                        // IFU offset
      0.0,
      0.3,
      "2",                        // HAMAMATSU CCD
      GmosParameters.GMOS_NORTH),

    new GmosParameters(
      FilterSouth.g_G0325,
      DisperserSouth.MIRROR,
      500.0,
      GmosParameters.NO_SLIT,
      1,
      1,
      "",
      0.0,
      0.0,
      0.3,
      "2",                        // HAMAMATSU CCD
      GmosParameters.GMOS_SOUTH)
  ))

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new GmosParameters(
      FilterNorth.g_G0301,
      DisperserNorth.R150_G5306,
      500.0,
      GmosParameters.SLIT1_0,
      1,
      1,
      "singleIFU",
      0.0,
      0.0,
      0.3,
      "0",                        // EEV ED; still supported?
      GmosParameters.GMOS_NORTH),
    new GmosParameters(
      FilterNorth.g_G0301,
      DisperserNorth.R400_G5305,
      500.0,
      GmosParameters.IFU,
      1,
      1,
      GmosParameters.SINGLE_IFU,
      0.0,
      0.0,
      0.3,
      "1",                        // EEV legacy; still supported?
      GmosParameters.GMOS_NORTH),
    new GmosParameters(
      FilterNorth.g_G0301,
      DisperserNorth.R150_G5306,
      500.0,
      GmosParameters.IFU,
      1,
      1,
      GmosParameters.RADIAL_IFU,
      0.0,
      0.0,
      0.3,
      "2",                        // HAMAMATSU CCD
      GmosParameters.GMOS_NORTH)
  ))

}

