package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.{IfuRadial, GmosParameters, IfuSingle}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.gmos.GmosCommonType.{AmpReadMode, AmpGain, DetectorManufacturer, BuiltinROI}
import edu.gemini.spModel.gemini.gmos.GmosNorthType.{DisperserNorth, FPUnitNorth, FilterNorth}
import edu.gemini.spModel.gemini.gmos.GmosSouthType.{DisperserSouth, FPUnitSouth, FilterSouth}
import edu.gemini.spModel.core.WavelengthConversions._

/**
 * GMOS baseline test fixtures.
 */
object BaselineGmos {


  lazy val Fixtures = RBandImaging ++ KBandSpectroscopy ++ KBandIfuSpectroscopy

  // === IMAGING

  private lazy val RBandImaging = Fixture.rBandImgFixtures(List(

    // GMOS-N
    GmosParameters(
      FilterNorth.i_G0302,
      DisperserNorth.MIRROR,
      500.nm,                       // central wavelength
      FPUnitNorth.FPU_NONE,
      AmpGain.HIGH,
      AmpReadMode.SLOW,
      None,
      1,
      1,
      DetectorManufacturer.E2V,
      BuiltinROI.FULL_FRAME,
      Site.GN),

    GmosParameters(
      FilterNorth.i_G0302,
      DisperserNorth.MIRROR,
      500.nm,                       // central wavelength
      FPUnitNorth.FPU_NONE,
      AmpGain.LOW,
      AmpReadMode.FAST,
      None,
      2,
      2,
      DetectorManufacturer.HAMAMATSU,
      BuiltinROI.FULL_FRAME,
      Site.GN),

    // GMOS-S
    GmosParameters(
      FilterSouth.g_G0325,
      DisperserSouth.MIRROR,
      500.nm,
      FPUnitSouth.FPU_NONE,
      AmpGain.HIGH,
      AmpReadMode.SLOW,
      None,
      2,
      4,
      DetectorManufacturer.E2V,
      BuiltinROI.FULL_FRAME,
      Site.GS),

    GmosParameters(
      FilterSouth.g_G0325,
      DisperserSouth.MIRROR,
      500.nm,
      FPUnitSouth.FPU_NONE,
      AmpGain.LOW,
      AmpReadMode.SLOW,
      None,
      4,
      4,
      DetectorManufacturer.HAMAMATSU,
      BuiltinROI.FULL_FRAME,
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
      AmpGain.HIGH,
      AmpReadMode.SLOW,
      None,
      1,
      1,
      DetectorManufacturer.E2V,
      BuiltinROI.CENTRAL_SPECTRUM,
      Site.GN),

    // GMOS-S
    GmosParameters(
      FilterSouth.g_G0325,
      DisperserSouth.R150_G5326,
      500.nm,
      FPUnitSouth.LONGSLIT_2,
      AmpGain.HIGH,
      AmpReadMode.SLOW,
      None,
      2,
      4,
      DetectorManufacturer.E2V,
      BuiltinROI.CENTRAL_SPECTRUM,
      Site.GS)

  ))

  // === IFU SPECTROSCOPY

  private lazy val KBandIfuSpectroscopy = Fixture.kBandIfuGmosFixtures(List(

    // GMOS-N
    GmosParameters(
      FilterNorth.g_G0301,
      DisperserNorth.B480_G5309,
      500.nm,
      FPUnitNorth.IFU_1,
      AmpGain.HIGH,
      AmpReadMode.SLOW,
      None,
      1,
      1,
      DetectorManufacturer.HAMAMATSU,
      BuiltinROI.FULL_FRAME,
      Site.GN),

    GmosParameters(
      FilterNorth.g_G0301,
      DisperserNorth.R150_G5306,
      500.nm,
      FPUnitNorth.IFU_1,
      AmpGain.HIGH,
      AmpReadMode.SLOW,
      None,
      1,
      1,
      DetectorManufacturer.E2V,
      BuiltinROI.FULL_FRAME,
      Site.GN),

    GmosParameters(
      FilterNorth.NONE,
      DisperserNorth.R400_G5305,
      500.nm,
      FPUnitNorth.IFU_2,
      AmpGain.HIGH,
      AmpReadMode.SLOW,
      None,
      1,
      1,
      DetectorManufacturer.HAMAMATSU,
      BuiltinROI.FULL_FRAME,
      Site.GN),

    // GMOS-S
    GmosParameters(
      FilterSouth.NONE,
      DisperserSouth.R400_G5325,
      500.nm,
      FPUnitSouth.IFU_3,
      AmpGain.HIGH,
      AmpReadMode.SLOW,
      None,
      1,
      1,
      DetectorManufacturer.HAMAMATSU,
      BuiltinROI.FULL_FRAME,
      Site.GS)
  ))
}
