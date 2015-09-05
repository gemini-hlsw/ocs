package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.{GmosParameters, IfuSingle}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.gmos.GmosCommonType.DetectorManufacturer
import edu.gemini.spModel.gemini.gmos.GmosNorthType.{DisperserNorth, FPUnitNorth, FilterNorth}
import edu.gemini.spModel.gemini.gmos.GmosSouthType.{DisperserSouth, FPUnitSouth, FilterSouth}
import edu.gemini.spModel.core.WavelengthConversions._

/**
 * GMOS baseline test fixtures.
 */
object BaselineGmos {


  lazy val Fixtures = RBandImaging ++ KBandSpectroscopy

  // === IMAGING

  private lazy val RBandImaging = Fixture.rBandImgFixtures(List(

    // GMOS-N
    GmosParameters(
      FilterNorth.i_G0302,
      DisperserNorth.MIRROR,
      500.nm,                       // central wavelength
      FPUnitNorth.FPU_NONE,
      None,
      1,
      1,
      None,                         // IFU method
      DetectorManufacturer.E2V,
      Site.GN),
    GmosParameters(
      FilterNorth.i_G0302,
      DisperserNorth.MIRROR,
      500.nm,                       // central wavelength
      FPUnitNorth.FPU_NONE,
      None,
      2,
      2,
      None,                         // IFU method
      DetectorManufacturer.HAMAMATSU,
      Site.GN),

    // GMOS-S
    GmosParameters(
      FilterSouth.g_G0325,
      DisperserSouth.MIRROR,
      500.nm,
      FPUnitSouth.FPU_NONE,
      None,
      2,
      4,
      None,
      DetectorManufacturer.E2V,
      Site.GS),
    GmosParameters(
      FilterSouth.g_G0325,
      DisperserSouth.MIRROR,
      500.nm,
      FPUnitSouth.FPU_NONE,
      None,
      4,
      4,
      None,
      DetectorManufacturer.HAMAMATSU,
      Site.GS)

  ))

  // === SPECTROSCOPY

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(

    // GMOS-N
    GmosParameters(
      FilterNorth.g_G0301,
      DisperserNorth.R150_G5306,
      500.nm,
      FPUnitNorth.LONGSLIT_2,
      None,
      1,
      1,
      None,
      DetectorManufacturer.E2V,
      Site.GN),
    GmosParameters(
      FilterNorth.g_G0301,
      DisperserNorth.R400_G5305,
      500.nm,
      FPUnitNorth.IFU_2,
      None,
      2,
      2,
      Some(IfuSingle(0.0)),
      DetectorManufacturer.HAMAMATSU,
      Site.GN),
    GmosParameters(
      FilterNorth.g_G0301,
      DisperserNorth.R400_G5305,
      500.nm,
      FPUnitNorth.IFU_3,
      None,
      2,
      2,
      Some(IfuSingle(0.3)),
      DetectorManufacturer.HAMAMATSU,
      Site.GN),

    // GMOS-S
    GmosParameters(
      FilterSouth.g_G0325,
      DisperserSouth.R150_G5326,
      500.nm,
      FPUnitSouth.LONGSLIT_2,
      None,
      2,
      4,
      None,
      DetectorManufacturer.E2V,
      Site.GS)//,
//    GmosParameters(
//      FilterSouth.g_G0325,
//      DisperserSouth.R400_G5325,
//      Wavelength.fromNanometers(500.0),
//      FPUnitSouth.IFU_1,
//      4,
//      4,
//      Some(IfuRadial(0.0, 0.3)),
//      DetectorManufacturer.HAMAMATSU,
//      Site.GS)
  ))

}

