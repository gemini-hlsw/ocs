package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.gmos.{GmosParameters, GmosRecipe}
import edu.gemini.itc.shared.{IfuRadial, IfuSingle}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.gmos.GmosCommonType.DetectorManufacturer
import edu.gemini.spModel.gemini.gmos.GmosNorthType.{FPUnitNorth, DisperserNorth, FilterNorth}
import edu.gemini.spModel.gemini.gmos.GmosSouthType.{FPUnitSouth, DisperserSouth, FilterSouth}

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
      FPUnitNorth.FPU_NONE,
      1,
      1,
      None,                         // IFU method
      DetectorManufacturer.HAMAMATSU,
      Site.GN),

    new GmosParameters(
      FilterSouth.g_G0325,
      DisperserSouth.MIRROR,
      500.0,
      FPUnitSouth.FPU_NONE,
      1,
      1,
      None,
      DetectorManufacturer.HAMAMATSU,                        // HAMAMATSU CCD
      Site.GS)
  ))

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    new GmosParameters(
      FilterNorth.g_G0301,
      DisperserNorth.R150_G5306,
      500.0,
      FPUnitNorth.LONGSLIT_4,
      1,
      1,
      Some(IfuSingle(0.0)),
      DetectorManufacturer.E2V,
      Site.GN),
//    new GmosParameters(       //TODO active with next baseline, use E2V array, legacy not available for GN
//      FilterNorth.g_G0301,
//      DisperserNorth.R400_G5305,
//      500.0,
//      FPUnitNorth.IFU_1,
//      1,
//      1,
//      Some(IfuSingle(0.0)),
//      "1",                        // EEV legacy; still supported?
//      Site.GN),
    new GmosParameters(
      FilterNorth.g_G0301,
      DisperserNorth.R150_G5306,
      500.0,
      FPUnitNorth.IFU_1,
      1,
      1,
      Some(IfuRadial(0.0, 0.3)),
      DetectorManufacturer.HAMAMATSU,                        // HAMAMATSU CCD
      Site.GN)
  ))

}

