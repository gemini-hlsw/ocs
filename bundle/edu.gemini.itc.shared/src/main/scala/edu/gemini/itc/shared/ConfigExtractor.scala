package edu.gemini.itc.shared

import java.time.Instant

import edu.gemini.pot.ModelConverters._
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.spModel.config2.{Config, ItemKey}
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.acqcam.AcqCamParams
import edu.gemini.spModel.gemini.altair.AltairParams
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.{GmosCommonType, GmosNorthType, GmosSouthType, InstGmosNorth}
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.nifs.NIFSParams
import edu.gemini.spModel.gemini.niri.Niri
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.rich.shared.immutable.asScalaOpt
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.{Asterism, GuideProbeTargets, TargetEnvironment}
import edu.gemini.spModel.telescope.IssPort
import edu.gemini.spModel.core.WavelengthConversions._
import edu.gemini.shared.util.immutable.{Option => GOption}

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
  private val AmpReadModeKey      = new ItemKey("instrument:ampReadMode")
  private val AmpGainKey          = new ItemKey("instrument:gainChoice")
  private val CcdXBinKey          = new ItemKey("instrument:ccdXBinning")           // X aka spectral binning
  private val CcdYBinKey          = new ItemKey("instrument:ccdYBinning")           // Y aka spatial binning
  private val ReadModeKey         = new ItemKey("instrument:readMode")
  private val CcdManufacturerKey  = new ItemKey("instrument:detectorManufacturer")
  private val CameraKey           = new ItemKey("instrument:camera")
  private val WellDepthKey        = new ItemKey("instrument:wellDepth")
  private val MaskKey             = new ItemKey("instrument:mask")
  private val ObsWavelengthKey    = new ItemKey("instrument:observingWavelength")
  private val PortKey             = new ItemKey("instrument:port")
  private val CustomSlitWidthKey  = new ItemKey("instrument:customSlitWidth")
  private val PixelScaleKey       = new ItemKey("instrument:pixelScale")
  private val CrossDispersedKey   = new ItemKey("instrument:crossDispersed")
  private val SlitWidthKey        = new ItemKey("instrument:slitWidth")

  private val AoSystemKey         = new ItemKey("adaptive optics:aoSystem")
  private val AoFieldLensKey      = new ItemKey("adaptive optics:fieldLens")
  private val AoGuideStarTypeKey  = new ItemKey("adaptive optics:guideStarType")

  def extractInstrumentDetails(instrument: SPInstObsComp, probe: GuideProbe, targetEnv: TargetEnvironment, when: GOption[java.lang.Long], c: Config, cond: ObservingConditions): String \/ InstrumentDetails =
    instrument.getType match {
      case INSTRUMENT_ACQCAM                      => extractAcqCam(c)
      case INSTRUMENT_FLAMINGOS2                  => extractF2(c)
      case INSTRUMENT_GNIRS                       => extractGnirs(targetEnv, probe, when, c)
      case INSTRUMENT_GMOS | INSTRUMENT_GMOSSOUTH => extractGmos(c)
      case INSTRUMENT_GSAOI                       => extractGsaoi(c, cond)
      case INSTRUMENT_NIFS                        => extractNifs(targetEnv, probe, when, c)
      case INSTRUMENT_NIRI                        => extractNiri(targetEnv, probe, when, c)
      case _                                      => "Instrument is not supported".left
    }

  def extractTelescope(port: IssPort, probe: GuideProbe, targetEnv: TargetEnvironment, c: Config): String \/ TelescopeDetails = {
    import TelescopeDetails._
    new TelescopeDetails(Coating.SILVER, port, probe.getType).right
  }

  private def extractAcqCam(c: Config): String \/ AcquisitionCamParameters = {
    import AcqCamParams._
    for {
      colorFilter <- extract[ColorFilter]   (c, ColorFilterKey)
      ndFilter    <- extract[NDFilter]      (c, NdFilterKey)
    } yield AcquisitionCamParameters(colorFilter, ndFilter)
  }

  private def extractF2(c: Config): String \/ Flamingos2Parameters = {
    import Flamingos2._

    // Gets the optional custom slit width (only set for custom mask)
    def extractCustomSlitWidth(mask: FPUnit): String \/ Option[Flamingos2.CustomSlitWidth] = mask match {
      case FPUnit.CUSTOM_MASK => extract[Flamingos2.CustomSlitWidth](c, CustomSlitWidthKey).map(Some(_))
      case _                  => None.right
    }

    for {
      filter      <- extract[Filter]        (c, FilterKey)
      grism       <- extract[Disperser]     (c, DisperserKey)
      mask        <- extract[FPUnit]        (c, FpuKey)
      customSlit  <- extractCustomSlitWidth(mask)
      readMode    <- extract[ReadMode]      (c, ReadModeKey)
    } yield Flamingos2Parameters(filter, grism, mask, customSlit, readMode)
  }

  private def extractGnirs(targetEnv: TargetEnvironment, probe: GuideProbe, when: GOption[java.lang.Long], c: Config): String \/ GnirsParameters = {
    import GNIRSParams._

    def extractDisperser: String \/ Option[GNIRSParams.Disperser] =
      extract[GNIRSParams.Disperser](c, DisperserKey).map(Some(_))

    def extractFilter: String \/ Option[GNIRSParams.Filter] =
      if (c.containsItem(FilterKey)) extract[GNIRSParams.Filter](c, FilterKey).map(Some(_)) else None.right

    def extractCamera: String \/ Option[GNIRSParams.Camera] =
       extract[GNIRSParams.Camera](c, CameraKey).map(Some(_))

    for {
      pixelScale  <- extract[PixelScale]        (c, PixelScaleKey)
      filter      <- extractFilter
      readMode    <- extract[ReadMode]          (c, ReadModeKey)
      xDisp       <- extract[CrossDispersed]    (c, CrossDispersedKey)
      slitWidth   <- extract[SlitWidth]         (c, SlitWidthKey)
      grating     <- extractDisperser
      camera      <- extractCamera
      wellDepth   <- extract[WellDepth]         (c, WellDepthKey)
      altair      <- extractAltair             (targetEnv, probe, when, c)
      wavelen     <- extractObservingWavelength(c)
    } yield GnirsParameters(pixelScale, filter, grating, readMode, xDisp, wavelen, slitWidth, camera, wellDepth, altair)
  }

  private def extractGmos(c: Config): String \/ GmosParameters = {
    import GmosCommonType._

    // Gets the site this GMOS belongs to
    def extractSite: String \/ Site =
      extract[String](c, InstrumentKey).map(s => if (s.equals(InstGmosNorth.INSTRUMENT_NAME_PROP)) Site.GN else Site.GS)

    // Gets the custom mask for the given site
    def customMask(s: Site): FPUnit = s match {
      case Site.GN => GmosNorthType.FPUnitNorth.CUSTOM_MASK
      case Site.GS => GmosSouthType.FPUnitSouth.CUSTOM_MASK
    }

    // Gets the mask, supplying the appropriate custom mask for the site if empty
    def extractMask(s: Site): String \/ FPUnit =
      if (c.containsItem(FpuKey)) extract[FPUnit](c, FpuKey) else customMask(s).right

    // Gets the optional custom slit width
    def extractCustomSlit: String \/ Option[CustomSlitWidth] =
      if (c.containsItem(FpuKey)) None.right else extract[CustomSlitWidth](c, CustomSlitWidthKey).map(Some(_))

    for {
      site        <- extractSite
      mask        <- extractMask(site)
      customSlit  <- extractCustomSlit
      filter      <- extract[Filter]        (c, FilterKey)
      grating     <- extract[Disperser]     (c, DisperserKey)
      gain        <- extract[AmpGain]       (c, AmpGainKey)
      readMode    <- extract[AmpReadMode]   (c, AmpReadModeKey)
      specBin     <- extract[Binning]       (c, CcdXBinKey)
      spatBin     <- extract[Binning]       (c, CcdYBinKey)
      ccdType     <- extract[DetectorManufacturer](c, CcdManufacturerKey)
      wavelen     <- extractObservingWavelength(c)
    } yield {
      GmosParameters(filter, grating, wavelen, mask, gain, readMode, customSlit, spatBin.getValue, specBin.getValue, ccdType, site)
    }

  }

  private def extractGsaoi(c: Config, cond: ObservingConditions): String \/ GsaoiParameters = {

    import Gsaoi._
    import SPSiteQuality._

    val error: String \/ GemsParameters = "GSAOI filter with unknown band".left

    def closestBand(band: MagnitudeBand) =
      // pick the closest band that's supported by ITC
      List(MagnitudeBand.J, MagnitudeBand.H, MagnitudeBand.K).minBy(b => Math.abs(b.center.toNanometers - band.center.toNanometers))


    // a rudimentary approximation for the expected GeMS performance
    // http://www.gemini.edu/sciops/instruments/gems/gems-performance
    // TODO: here we should use the avg Strehl values calculated by the Mascot / AGS algorithms for better results
    def extractGems(filter: Filter): String \/ GemsParameters =
      filter.getCatalogBand.asScalaOpt.fold(error) { band =>
        (band, cond.iq) match {
          case (SingleBand(MagnitudeBand.J), ImageQuality.PERCENT_20) => GemsParameters(0.10, "J").right
          case (SingleBand(MagnitudeBand.J), ImageQuality.PERCENT_70) => GemsParameters(0.05, "J").right
          case (SingleBand(MagnitudeBand.J), ImageQuality.PERCENT_85) => GemsParameters(0.02, "J").right
          case (SingleBand(MagnitudeBand.H), ImageQuality.PERCENT_20) => GemsParameters(0.15, "H").right
          case (SingleBand(MagnitudeBand.H), ImageQuality.PERCENT_70) => GemsParameters(0.10, "H").right
          case (SingleBand(MagnitudeBand.H), ImageQuality.PERCENT_85) => GemsParameters(0.05, "H").right
          case (SingleBand(MagnitudeBand.K), ImageQuality.PERCENT_20) => GemsParameters(0.30, "K").right
          case (SingleBand(MagnitudeBand.K), ImageQuality.PERCENT_70) => GemsParameters(0.15, "K").right
          case (SingleBand(MagnitudeBand.K), ImageQuality.PERCENT_85) => GemsParameters(0.10, "K").right
          case (_, ImageQuality.ANY)             => "GeMS cannot be used in IQ=Any conditions".left
          case _                                 => "ITC GeMS only supports J, H and K band".left
        }
    }

    for {
      filter      <- extract[Filter]        (c, FilterKey)
      readMode    <- extract[ReadMode]      (c, ReadModeKey)
      gems        <- extractGems            (filter)
    } yield {
      GsaoiParameters(filter, readMode, gems)
    }
  }

  private def extractNifs(targetEnv: TargetEnvironment, probe: GuideProbe, when: GOption[java.lang.Long], c: Config): String \/ NifsParameters = {

    import NIFSParams._

    for {
      filter      <- extract[Filter]        (c, FilterKey)
      grating     <- extract[Disperser]     (c, DisperserKey)
      readMode    <- extract[ReadMode]      (c, ReadModeKey)
      altair      <- extractAltair          (targetEnv, probe, when, c)
      wavelen     <- extractObservingWavelength(c)
    } yield {
      NifsParameters(filter, grating, readMode, wavelen, altair)
    }
  }

  private def extractNiri(targetEnv: TargetEnvironment, probe: GuideProbe, when: GOption[java.lang.Long], c: Config): String \/ NiriParameters = {
    import Niri._
    for {
      filter      <- extract[Filter]        (c, FilterKey)
      grism       <- extract[Disperser]     (c, DisperserKey)
      camera      <- extract[Camera]        (c, CameraKey)
      readMode    <- extract[ReadMode]      (c, ReadModeKey)
      wellDepth   <- extract[WellDepth]     (c, WellDepthKey)
      mask        <- extract[Mask]          (c, MaskKey)
      altair      <- extractAltair          (targetEnv, probe, when, c)
    } yield NiriParameters(filter, grism, camera, readMode, wellDepth, mask, altair)
  }

  private def extractAltair(targetEnv: TargetEnvironment, probe: GuideProbe, when: GOption[java.lang.Long], c: Config): String \/ Option[AltairParameters] = {
    import AltairParams._

    def altairIsPresent =
      c.containsItem(AoSystemKey) && extract[String](c, AoSystemKey).map("Altair".equals).getOrElse(false)

    def extractGroup =
      targetEnv.getPrimaryGuideProbeTargets(probe).asScalaOpt.fold("No guide star selected".left[GuideProbeTargets])(_.right)

    def extractGuideStar(targets: GuideProbeTargets) =
      targets.getPrimary.asScalaOpt.fold("No guide star selected".left[SPTarget])(_.right)

    def extractMagnitude(guideStar: SPTarget) = {
      val r  = guideStar.getMagnitude(MagnitudeBand._r)
      val R  = guideStar.getMagnitude(MagnitudeBand.R)
      val UC = guideStar.getMagnitude(MagnitudeBand.UC)
      if      (r.isDefined)   r.map(_.value).get.right
      else if (R.isDefined)   R.map(_.value).get.right
      else if (UC.isDefined) UC.map(_.value).get.right
      else "No r, R or UC magnitude defined for guide star".left
    }

    if (altairIsPresent) {
      for {
        group     <- extractGroup
        guideStar <- extractGuideStar(group)
        magnitude <- extractMagnitude(guideStar)
        fieldLens <- extract[FieldLens]     (c, AoFieldLensKey)
        wfsMode   <- extract[GuideStarType] (c, AoGuideStarTypeKey)
      } yield {
        // TODO:ASTERISM: may be wrong, see https://github.com/gemini-hlsw/ocs/pull/1222#discussion_r115356730
        val separation = distance(targetEnv.getAsterism, guideStar.getTarget, when)
        Some(AltairParameters(separation, magnitude, fieldLens, wfsMode))
      }
    } else {
      None.right
    }
  }

  // Gets the observing wavelength from the configuration.
  // For imaging this corresponds to the mid point of the selected filter, for spectroscopy the value
  // is defined by the user. Unit is microns [um]. Note that for Acq Cam the observing wavelength
  // is not defined, instead we need to get the wavelength from the color filter. Some special magic
  // is needed for GNIRS where the wavelength value can be either a string or (in case of GNIRS sequence
  // iterators) a GNIRSParams.Wavelength. Fascinating.
  def extractObservingWavelength(c: Config): String \/ Wavelength = {
    val instrument = extract[String](c, InstrumentKey).getOrElse("")
    (instrument match {
      case "AcqCam" =>
        extract[AcqCamParams.ColorFilter](c, ColorFilterKey).map(_.getCentralWavelength.toDouble)
      case "GNIRS" if extractWithThrowable[String](c, ObsWavelengthKey).isRight =>
        extract[String](c, ObsWavelengthKey).map(_.toDouble)
      case "GNIRS" =>
        extract[GNIRSParams.Wavelength](c, ObsWavelengthKey).map(_.doubleValue())
      case _ =>
        if (c.containsItem(ObsWavelengthKey)) extractDoubleFromString(c, ObsWavelengthKey)
        else "Observing wavelength is not defined (missing filter?)".left

    }).map(_.microns)
  }


  // Extract an optional integer, values in the configuration are Java objects
  def extractOptionalInteger(c: Config, key: ItemKey): Option[Int] =
    extract[java.lang.Integer](c, key).map(_.toInt).toOption

  // Extract a value of the given type from the configuration
  def extract[A](c: Config, key: ItemKey)(implicit clazz: ClassTag[A]): String \/ A =
    extractWithThrowable[A](c, key).leftMap(_.getMessage)

  // Extract a double value from a string in the configuration
  def extractDoubleFromString(c: Config, key: ItemKey): String \/ Double = {
    val v = for {
      s <- extractWithThrowable[String](c, key)
      d <- \/.fromTryCatchNonFatal(s.toDouble)
    } yield d
    v.leftMap(_.getMessage)
   }

  // Helper method that enforces that whatever we get from the config
  // for the given key is not null and matches the type we expect.
  private def extractWithThrowable[A](c: Config, key: ItemKey)(implicit clazz: ClassTag[A]): Throwable \/ A = {

    def missingKey[A](key: ItemKey): \/[Throwable, A] =
      new Error(s"Missing config value for key ${key.getPath}").left[A]

    Option(c.getItemValue(key)).fold(missingKey[A](key)) { v =>
      \/.fromTryCatchNonFatal(clazz.runtimeClass.cast(v).asInstanceOf[A])
    }

  }

  // Calculate distance between two coordinates in arc seconds
  private def distance(a: Asterism, t1: Target, when: GOption[java.lang.Long]) = {
    val c0 = a.basePosition(when.asScalaOpt.map(Instant.ofEpochSecond(_))).getOrElse(Coordinates.zero)
    val c1 = t1.coords(when.asScalaOpt.map(_.toLong)) getOrElse Coordinates.zero
    Coordinates.difference(c0, c1).distance.toArcsecs
  }

}
