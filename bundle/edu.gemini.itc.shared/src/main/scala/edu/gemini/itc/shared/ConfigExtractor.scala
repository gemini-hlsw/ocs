package edu.gemini.itc.shared

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.spModel.config2.{Config, ItemKey}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.acqcam.AcqCamParams
import edu.gemini.spModel.gemini.altair.AltairParams
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.GmosCommonType
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.niri.Niri
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.IssPort

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
  private val PortKey             = new ItemKey("instrument:port")

  private val AoSystemKey         = new ItemKey("adaptive optics:aoSystem")
  private val AoFieldLensKey      = new ItemKey("adaptive optics:fieldLens")
  private val AoGuideStarTypeKey  = new ItemKey("adaptive optics:guideStarType")

  def extractInstrumentDetails(instrument: SPComponentType, c: Config): \/[String, InstrumentDetails] =
    instrument match {
      case INSTRUMENT_ACQCAM                      => ConfigExtractor.extractAcqCam(c)
      case INSTRUMENT_FLAMINGOS2                  => ConfigExtractor.extractF2(c)
      case INSTRUMENT_GMOS | INSTRUMENT_GMOSSOUTH => ConfigExtractor.extractGmos(c)
      case INSTRUMENT_GSAOI                       => ConfigExtractor.extractGsaoi(c)
      case INSTRUMENT_NIRI                        => ConfigExtractor.extractNiri(c)
      case _                                      => "Instrument is not supported".left
    }

  def extractTelescope(port: IssPort, probe: GuideProbe.Type, targetEnv: TargetEnvironment, c: Config): \/[String, TelescopeDetails] = {
    import TelescopeDetails._
    new TelescopeDetails(Coating.SILVER, port, probe).right
  }

  private def extractAcqCam(c: Config): \/[String, AcquisitionCamParameters] = {
    import AcqCamParams._
    for {
      colorFilter <- extract[ColorFilter]   (c, ColorFilterKey)
      ndFilter    <- extract[NDFilter]      (c, NdFilterKey)
    } yield AcquisitionCamParameters(colorFilter, ndFilter)
  }

  private def extractF2(c: Config): \/[String, Flamingos2Parameters] = {
    import Flamingos2._
    for {
      filter      <- extract[Filter]        (c, FilterKey)
      grism       <- extract[Disperser]     (c, DisperserKey)
      mask        <- extract[FPUnit]        (c, FpuKey)
      readMode    <- extract[ReadMode]      (c, ReadModeKey)
    } yield Flamingos2Parameters(filter, grism, mask, readMode)
  }

  private def extractGmos(c: Config): \/[String, GmosParameters] = {
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

  private def extractGsaoi(c: Config): \/[String, GsaoiParameters] = {
    import Gsaoi._
    for {
      filter      <- extract[Filter]        (c, FilterKey)
      readMode    <- extract[ReadMode]      (c, ReadModeKey)
    } yield {
      val gems = GemsParameters(0.3, "K") // TODO: gems
      GsaoiParameters(filter, readMode, gems)
    }
  }

  private def extractNiri(c: Config): \/[String, NiriParameters] = {
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

  private def extractAltair(c: Config): \/[String, Option[AltairParameters]] = {
    import AltairParams._

    def altairIsPresent =
      c.containsItem(AoSystemKey) && extract[String](c, AoSystemKey).rightMap("Altair".equals).getOrElse(false)

    if (altairIsPresent) {
      for {
        fieldLens <- extract[FieldLens]     (c, AoFieldLensKey)
        wfsMode   <- extract[GuideStarType] (c, AoGuideStarTypeKey)
      } yield {
        val guideStarSeparation = 4.0 // TODO
        val guideStarMagnitude = 9.0 // TODO
        Some(AltairParameters(guideStarSeparation, guideStarMagnitude, fieldLens, wfsMode))
      }
    } else {
      None.right
    }
  }

  private def extractObservingWavelength(c: Config): \/[String, Double] =
    // Note: observing wavelength will only be available if instrument is configured for spectroscopy
    if (c.containsItem(ObsWavelengthKey)) extractDoubleFromString(c, ObsWavelengthKey) else 0.0.right

  // Extract a value of the given type from the configuration
  private def extract[A](c: Config, key: ItemKey)(implicit clazz: ClassTag[A]): \/[String, A] =
    extractWithThrowable[A](c, key).leftMap(_.getMessage)

  // Extract a double value from a string in the configuration
  private def extractDoubleFromString(c: Config, key: ItemKey): \/[String, Double] = {
    val v = for {
      s <- extractWithThrowable[String](c, key)
      d <- \/.fromTryCatch(s.toDouble)
    } yield d
    v.leftMap(_.getMessage)
   }

  // Helper method that enforces that whatever we get from the config
  // for the given key is not null and matches the type we expect.
  private def extractWithThrowable[A](c: Config, key: ItemKey)(implicit clazz: ClassTag[A]): \/[Throwable, A] = {

    def missingKey[A](key: ItemKey): \/[Throwable, A] =
      new Error("Missing config value for key ${key.getPath}").left[A]

    Option(c.getItemValue(key)).fold(missingKey[A](key)) { v =>
      \/.fromTryCatch(clazz.runtimeClass.cast(v).asInstanceOf[A])
    }

  }

}
