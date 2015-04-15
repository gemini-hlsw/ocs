package edu.gemini.itc.shared

import edu.gemini.spModel.config2.{Config, ItemKey}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.altair.AltairParams
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.acqcam.AcqCamParams
import edu.gemini.spModel.gemini.gmos.GmosCommonType
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.niri.Niri

import scala.reflect.ClassTag
import scalaz.Scalaz._
import scalaz._

/**
 * A helper class that translates OT instrument configs into parameter objects that can be consumed by the ITC.
 */
object ConfigExtractor {

  private val InstrumentKey       = new ItemKey("instrument:instrument")
  private val FilterKey           = new ItemKey("instrument:filter")
  private val ColorFilterKey      = new ItemKey("instrument:colorFilter")
  private val NdFilterKey         = new ItemKey("instrument:ndFilter")
  private val FpuKey              = new ItemKey("instrument:fpu")
  private val DisperserKey        = new ItemKey("instrument:disperser")
  private val CcdXBinKey          = new ItemKey("instrument:ccdXBinning")
  private val CcdYBinKey          = new ItemKey("instrument:ccdYBinning")
  private val ReadModeKey         = new ItemKey("instrument:readMode")
  private val CcdManufacturerKey  = new ItemKey("instrument:detectorManufacturer")
  private val CameraKey           = new ItemKey("instrument:camera")
  private val WellDepthKey        = new ItemKey("instrument:wellDepth")
  private val MaskKey             = new ItemKey("instrument:mask")
  private val ObsWavelengthKey    = new ItemKey("instrument:observingWavelength")

  private val AoSystemKey         = new ItemKey("adaptive optics:aoSystem")
  private val AoFieldLensKey      = new ItemKey("adaptive optics:fieldLens")
  private val AoGuideStarTypeKey  = new ItemKey("adaptive optics:guideStarType")

  def extractAcqCam(c: Config): \/[Throwable, AcquisitionCamParameters] = {
    import AcqCamParams._
    for {
      colorFilter <- extract[ColorFilter]   (c, ColorFilterKey)
      ndFilter    <- extract[NDFilter]      (c, NdFilterKey)
    } yield AcquisitionCamParameters(colorFilter, ndFilter)
  }

  def extractF2(c: Config): \/[Throwable, Flamingos2Parameters] = {
    import Flamingos2._
    for {
      filter      <- extract[Filter]        (c, FilterKey)
      grism       <- extract[Disperser]     (c, DisperserKey)
      mask        <- extract[FPUnit]        (c, FpuKey)
      readMode    <- extract[ReadMode]      (c, ReadModeKey)
    } yield Flamingos2Parameters(filter, grism, mask, readMode)
  }

  def extractGmos(c: Config): \/[Throwable, GmosParameters] = {
    import GmosCommonType._
    for {
      filter      <- extract[Filter]        (c, FilterKey)
      grating     <- extract[Disperser]     (c, DisperserKey)
      fpmask      <- extract[FPUnit]        (c, FpuKey)
      spatBin     <- extract[Binning]       (c, CcdXBinKey)
      specBin     <- extract[Binning]       (c, CcdYBinKey)
      ccdType     <- extract[DetectorManufacturer](c, CcdManufacturerKey)
      siteString  <- extract[String]        (c, InstrumentKey)
      wavelen     <- extractObservingWavelength(c)
    } yield {
      val ifuMethod   =  None // TODO
      val site = if (siteString.equals("GMOS-N")) Site.GN else Site.GS
      GmosParameters(filter, grating, wavelen, fpmask, spatBin.getValue, specBin.getValue, ifuMethod, ccdType, site)
    }

  }

  def extractGsaoi(c: Config): \/[Throwable, GsaoiParameters] = {
    import Gsaoi._
    for {
      filter      <- extract[Filter]        (c, FilterKey)
      readMode    <- extract[ReadMode]      (c, ReadModeKey)
    } yield {
      val gems = GemsParameters(0.3, "K") // TODO: gems
      GsaoiParameters(filter, readMode, gems)
    }
  }

  def extractNiri(c: Config): \/[Throwable, NiriParameters] = {
    import Niri._
    for {
      filter      <- extract[Filter]        (c, FilterKey)
      grism       <- extract[Disperser]     (c, DisperserKey)
      camera      <- extract[Camera]        (c, CameraKey)
      readMode    <- extract[ReadMode]      (c, ReadModeKey)
      wellDepth   <- extract[WellDepth]     (c, WellDepthKey)
      mask        <- extract[Mask]          (c, MaskKey)
      altair      <- extractAltair          (c)
    } yield NiriParameters(filter, grism, camera, readMode, wellDepth, mask, altair)
  }

  def extractAltair(c: Config): \/[Throwable, Option[AltairParameters]] = {
    import AltairParams._
    if (c.containsItem(AoSystemKey) && extract[String](c, AoSystemKey).equals("Altair")) {
      for {
        fieldLens <- extract[FieldLens]     (c, AoFieldLensKey)
        wfsMode   <- extract[GuideStarType] (c, AoGuideStarTypeKey)
      } yield {
        val guideStarSeparation = 4.0  // TODO
        val guideStarMagnitude  = 9.0  // TODO
        Some(AltairParameters(guideStarSeparation, guideStarMagnitude, fieldLens, wfsMode))
      }
    } else {
      None.right
    }
  }
  
  def extractObservingWavelength(c: Config): \/[Throwable, Double] =
    // Note: observing wavelength will only be available if instrument is configured for spectroscopy
    if (c.containsItem(ObsWavelengthKey)) {
      for {
        s <- extract[String](c, ObsWavelengthKey)
      } yield s.toDouble
    } else {
      0.0.right
    }

  // Helper method that enforces that whatever we get from the config
  // for the given key is not null and matches the type we expect.
  private def extract[A](c: Config, key: ItemKey)(implicit ev: ClassTag[A]): \/[Throwable, A] = {
    Option(c.getItemValue(key)).fold(nullValue[A]) { v =>
      \/.fromTryCatch(ev.runtimeClass.cast(v).asInstanceOf[A])
    }
  }

  private def nullValue[A]: \/[Throwable, A] = new NullPointerException().left

}
