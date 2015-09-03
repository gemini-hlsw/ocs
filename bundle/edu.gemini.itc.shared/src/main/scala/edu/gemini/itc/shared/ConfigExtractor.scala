package edu.gemini.itc.shared

import edu.gemini.pot.ModelConverters._
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.shared.skyobject.Magnitude.Band
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
import edu.gemini.spModel.target.env.{GuideProbeTargets, TargetEnvironment}
import edu.gemini.spModel.target.system.ITarget
import edu.gemini.spModel.telescope.IssPort
import edu.gemini.spModel.core.WavelengthConversions._

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

  def extractInstrumentDetails(instrument: SPInstObsComp, probe: GuideProbe, targetEnv: TargetEnvironment, c: Config, cond: ObservingConditions): String \/ InstrumentDetails =
    instrument.getType match {
      case INSTRUMENT_ACQCAM                      => extractAcqCam(c)
      case INSTRUMENT_FLAMINGOS2                  => extractF2(c)
      case INSTRUMENT_GNIRS                       => extractGnirs(c)
      case INSTRUMENT_GMOS | INSTRUMENT_GMOSSOUTH => extractGmos(c)
      case INSTRUMENT_GSAOI                       => extractGsaoi(c, cond)
      case INSTRUMENT_NIFS                        => extractNifs(targetEnv, probe, c)
      case INSTRUMENT_NIRI                        => extractNiri(targetEnv, probe, c)
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
    for {
      filter      <- extract[Filter]        (c, FilterKey)
      grism       <- extract[Disperser]     (c, DisperserKey)
      mask        <- extract[FPUnit]        (c, FpuKey)
      readMode    <- extract[ReadMode]      (c, ReadModeKey)
    } yield Flamingos2Parameters(filter, grism, mask, readMode)
  }

  private def extractGnirs(c: Config): String \/ GnirsParameters = {
    import GNIRSParams._
    for {
      pixelScale  <- extract[PixelScale]     (c, PixelScaleKey)
      grating     <- extract[Disperser]      (c, DisperserKey)
      readMode    <- extract[ReadMode]       (c, ReadModeKey)
      xDisp       <- extract[CrossDispersed] (c, CrossDispersedKey)
      slitWidth   <- extract[SlitWidth]      (c, SlitWidthKey)
      wavelen     <- extractObservingWavelength(c)
    } yield GnirsParameters(pixelScale, grating, readMode, xDisp, wavelen, slitWidth)
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

    // Note: In the future we will support more options, for now only single on-axis is supported.
    def extractIfu(mask: GmosCommonType.FPUnit): Option[IfuMethod] =
      if (mask.isIFU) Some(IfuSingle(0.0)) else None

    for {
      site        <- extractSite
      mask        <- extractMask(site)
      customSlit  <- extractCustomSlit
      filter      <- extract[Filter]        (c, FilterKey)
      grating     <- extract[Disperser]     (c, DisperserKey)
      specBin     <- extract[Binning]       (c, CcdXBinKey)
      spatBin     <- extract[Binning]       (c, CcdYBinKey)
      ccdType     <- extract[DetectorManufacturer](c, CcdManufacturerKey)
      wavelen     <- extractObservingWavelength(c)
    } yield {
      val ifuMethod = extractIfu(mask)
      GmosParameters(filter, grating, wavelen, mask, customSlit, spatBin.getValue, specBin.getValue, ifuMethod, ccdType, site)
    }

  }

  private def extractGsaoi(c: Config, cond: ObservingConditions): String \/ GsaoiParameters = {

    import Gsaoi._
    import SPSiteQuality._

    val error: String \/ GemsParameters = "GSAOI filter with unknown band".left

    def closestBand(band: Band) =
      // pick the closest band that's supported by ITC
      List(Band.J, Band.H, Band.K).minBy(b => Math.abs(b.getWavelengthMidPoint.getValue.toNanometers - band.getWavelengthMidPoint.getValue.toNanometers))


    // a rudimentary approximation for the expected GeMS performance
    // http://www.gemini.edu/sciops/instruments/gems/gems-performance
    // TODO: here we should use the avg Strehl values calculated by the Mascot / AGS algorithms for better results
    def extractGems(filter: Filter): String \/ GemsParameters =
      filter.getCatalogBand.asScalaOpt.fold(error) { band =>
        val iq = cond.iq
        (band, iq) match {
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

  private def extractNifs(targetEnv: TargetEnvironment, probe: GuideProbe, c: Config): String \/ NifsParameters = {

    import NIFSParams._

    for {
      filter      <- extract[Filter]        (c, FilterKey)
      grating     <- extract[Disperser]     (c, DisperserKey)
      readMode    <- extract[ReadMode]      (c, ReadModeKey)
      altair      <- extractAltair          (targetEnv, probe, c)
      wavelen     <- extractObservingWavelength(c)
    } yield {
      // Note: In the future we will support more options, for now only single on-axis is supported.
      val ifu = IfuSingle(0.0)

      NifsParameters(filter, grating, readMode, wavelen, ifu, altair)
    }
  }

  private def extractNiri(targetEnv: TargetEnvironment, probe: GuideProbe, c: Config): String \/ NiriParameters = {
    import Niri._
    for {
      filter      <- extract[Filter]        (c, FilterKey)
      grism       <- extract[Disperser]     (c, DisperserKey)
      camera      <- extract[Camera]        (c, CameraKey)
      readMode    <- extract[ReadMode]      (c, ReadModeKey)
      wellDepth   <- extract[WellDepth]     (c, WellDepthKey)
      mask        <- extract[Mask]          (c, MaskKey)
      altair      <- extractAltair          (targetEnv, probe, c)
    } yield NiriParameters(filter, grism, camera, readMode, wellDepth, mask, altair)
  }

  private def extractAltair(targetEnv: TargetEnvironment, probe: GuideProbe, c: Config): String \/ Option[AltairParameters] = {
    import AltairParams._

    def altairIsPresent =
      c.containsItem(AoSystemKey) && extract[String](c, AoSystemKey).map("Altair".equals).getOrElse(false)

    def extractGroup =
      targetEnv.getPrimaryGuideProbeTargets(probe).asScalaOpt.fold("No guide star selected".left[GuideProbeTargets])(_.right)

    def extractGuideStar(targets: GuideProbeTargets) =
      targets.getPrimary.asScalaOpt.fold("No guide star selected".left[SPTarget])(_.right)

    def extractMagnitude(guideStar: ITarget) = {
      val r  = guideStar.getMagnitude(Band.r)
      val R  = guideStar.getMagnitude(Band.R)
      val UC = guideStar.getMagnitude(Band.UC)
      if      (r.isDefined)  r.getValue.getBrightness.right
      else if (R.isDefined)  R.getValue.getBrightness.right
      else if (UC.isDefined) UC.getValue.getBrightness.right
      else "No r, R or UC magnitude defined for guide star".left
    }

    if (altairIsPresent) {
      for {
        group     <- extractGroup
        guideStar <- extractGuideStar(group)
        magnitude <- extractMagnitude(guideStar.getTarget)
        fieldLens <- extract[FieldLens]     (c, AoFieldLensKey)
        wfsMode   <- extract[GuideStarType] (c, AoGuideStarTypeKey)
      } yield {
        val separation = distance(targetEnv.getBase, guideStar)
        Some(AltairParameters(separation, magnitude, fieldLens, wfsMode))
      }
    } else {
      None.right
    }
  }

  // Gets the observing wavelength from the configuration.
  // For imaging this corresponds to the mid point of the selected filter, for spectroscopy the value
  // is defined by the user. Unit is microns [um]. Note that for Acq Cam the observing wavelength
  // is not defined, instead we need to get the wavelength from the color filter. Also some special
  // magic is needed for GNIRS.
  def extractObservingWavelength(c: Config): String \/ Wavelength = {
    val instrument = extract[String](c, InstrumentKey).getOrElse("")
    (instrument match {
      case "AcqCam" =>
        extract[AcqCamParams.ColorFilter](c, ColorFilterKey).map(_.getCentralWavelength.toDouble)
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
      d <- \/.fromTryCatch(s.toDouble)
    } yield d
    v.leftMap(_.getMessage)
   }

  // Helper method that enforces that whatever we get from the config
  // for the given key is not null and matches the type we expect.
  private def extractWithThrowable[A](c: Config, key: ItemKey)(implicit clazz: ClassTag[A]): Throwable \/ A = {

    def missingKey[A](key: ItemKey): \/[Throwable, A] =
      new Error(s"Missing config value for key ${key.getPath}").left[A]

    Option(c.getItemValue(key)).fold(missingKey[A](key)) { v =>
      \/.fromTryCatch(clazz.runtimeClass.cast(v).asInstanceOf[A])
    }

  }

  // Calculate distance between two coordinates in arc seconds
  private def distance(t0: SPTarget, t1: SPTarget) = {
    val c0 = t0.toNewModel.coordinates
    val c1 = t1.toNewModel.coordinates
    Coordinates.difference(c0, c1).distance.toArcsecs
  }

}
