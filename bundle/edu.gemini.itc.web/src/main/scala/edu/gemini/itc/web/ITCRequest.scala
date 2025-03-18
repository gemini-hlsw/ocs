package edu.gemini.itc.web

import java.util.logging.Logger
import javax.servlet.http.HttpServletRequest
import edu.gemini.itc.shared._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core.WavelengthConversions._
import edu.gemini.spModel.core._
import edu.gemini.spModel.data.YesNoType
import edu.gemini.spModel.gemini.acqcam.AcqCamParams
import edu.gemini.spModel.gemini.altair.AltairParams
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.GmosCommonType
import edu.gemini.spModel.gemini.gmos.GmosCommonType.{AmpGain, AmpReadMode, DetectorManufacturer}
import edu.gemini.spModel.gemini.gmos.GmosNorthType.{DisperserNorth, FPUnitNorth, FilterNorth}
import edu.gemini.spModel.gemini.ghost.{GhostBinning, GhostReadNoiseGain}
import edu.gemini.spModel.gemini.gmos.GmosSouthType.{DisperserSouth, FPUnitSouth, FilterSouth}
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.michelle.MichelleParams
import edu.gemini.spModel.gemini.nifs.NIFSParams
import edu.gemini.spModel.gemini.niri.Niri
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.trecs.TReCSParams
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.telescope.IssPort
import squants.motion.KilometersPerSecond
import squants.motion.VelocityConversions._
import squants.radio.IrradianceConversions._
import squants.radio.SpectralIrradianceConversions._
import scalaz.{Enum => _, _}
import Scalaz._
import edu.gemini.spModel.target.env.ResolutionMode

/**
 * ITC requests define a generic mechanism to look up values by their parameter names.
 * The different values are either enums, ints or doubles. For enums the simple name of the class is used
 * as the parameter name. This convention allows for a mostly mechanical translation of typed values from
 * a request with string based parameters, i.e. parameters in HttpServletRequests for example.
 *
 * Note: Error handling / validation is done using exceptions (for now). All exceptions are caught in {{{ITCServlet}}}
 * and the exception message is displayed to the user on the result page. This is a bit arcane but is in sync with
 * how currently validation and error handling is done throughout the code.
 */
sealed abstract class ITCRequest {

  /** Gets the value of the parameter with this name. */
  def parameter(name: String): String

  /** Gets the value of a parameter with this name, if it is defined. */
  def optionalParameter(name: String): Option[String] =
    Option(parameter(name))

  /** Gets the named value as an enum of type {{{Class[T]}}} using the simple name of the class as the parameter name. */
  def enumParameter[T <: Enum[T]](c: Class[T]): T =
    enumParameter(c, c.getSimpleName)

  /** Gets the named value as an enum of type {{{Class[T]}}} using the given name as the parameter name. */
  def enumParameter[T <: Enum[T]](c: Class[T], n: String): T =
    optionalEnumParameter(c, n)
      .getOrElse(throw new IllegalArgumentException(s"Missing '$n' enum parameter"))

  def optionalEnumParameter[T <: Enum[T]](c: Class[T]): Option[T] =
    optionalEnumParameter(c, c.getSimpleName)

  def optionalEnumParameter[T <: Enum[T]](c: Class[T], n: String): Option[T] =
    optionalParameter(n).map(Enum.valueOf(c, _))

  /** Gets the named value as an integer. */
  def intParameter(name: String): Int = parameter(name).trim() match {
    case ""   => 0
    case i    =>
      try i.toInt catch {
        case _: NumberFormatException => throw new IllegalArgumentException(s"$i is not a valid integer number value for parameter $name")
      }
  }

  /** Gets the named value as a boolean, it accepts several types of strings as true/false. */
  def booleanParameter(name: String): Boolean = java.lang.Boolean.parseBoolean(parameter(name).trim())

  /** Gets the named value as a double. */
  def doubleParameter(name: String): Double =
    optionalDoubleParameter(name)
      .getOrElse(throw new IllegalArgumentException(s"Missing '$name' double parameter"))

  def optionalDoubleParameter(name: String): Option[Double] =
    optionalDoubleParameter(name, Some(0.0))

  def optionalDoubleParameter(name: String, empty: Option[Double]): Option[Double] =
    optionalParameter(name).map(_.trim).flatMap {
      case "" => empty
      case d  =>
        try Some(d.toDouble) catch {
          case _: NumberFormatException => throw new IllegalArgumentException(s"$d is not a valid double number value for parameter $name")
        }
    }

  /** Gets the central wavelength in microns. */
  def centralWavelengthInMicrons():    Wavelength = doubleParameter("instrumentCentralWavelength").microns

  /** Gets the central wavelength in nanometers. */
  def centralWavelengthInNanometers(): Wavelength = doubleParameter("instrumentCentralWavelength").nanometers

  /** Gets the user SED text file from the request.
    * Only multipart HTTP requests will support this. */
  def userSpectrum(): Option[UserDefined]

}

/**
 * Utility object that allows to translate different objects into ITC requests.
 */
object ITCRequest {
  private val Log = Logger.getLogger(classOf[ITCRequest].getName)

  def from(request: HttpServletRequest): ITCRequest = new ITCRequest {
    override def parameter(name: String): String = request.getParameter(name)
    override def userSpectrum(): Option[UserDefined] = None
  }
  def from(request: ITCMultiPartParser): ITCRequest = new ITCRequest {
    override def parameter(name: String): String = request.getParameter(name)
    override def userSpectrum(): Option[UserDefined] = Some(UserDefinedSpectrum(request.getRemoteFileName("specUserDef"), request.getTextFile("specUserDef")))
  }

  def teleParameters(r: ITCRequest): TelescopeDetails = {
    val coating = r.enumParameter(classOf[TelescopeDetails.Coating])
    val port    = r.enumParameter(classOf[IssPort])
    val wfs     = r.enumParameter(classOf[GuideProbe.Type])
    new TelescopeDetails(coating, port, wfs)
  }


  def obsConditionParameters(r: ITCRequest): ObservingConditions = {
    def either[A <: Enum[A]](exactName: String, c: Class[A]): Double \/ A = {
      val msg: String =
        s"Specify one of either '${c.getSimpleName}' bins or else '$exactName' with a floating point value."

      r.optionalParameter(c.getSimpleName) match {
        case Some("EXACT") | None =>
          r.optionalDoubleParameter(exactName, None)
           .toLeftDisjunction[A](throw new IllegalArgumentException(msg))

        case _                    =>
          r.optionalEnumParameter(c)
           .toRightDisjunction[Double](throw new IllegalArgumentException(msg))
      }
    }

    def iq: ExactIq \/ SPSiteQuality.ImageQuality =
      either("ExactIQ", classOf[SPSiteQuality.ImageQuality])
        .leftMap(ExactIq.fromArcsecOrException)

    val cc: ExactCc \/ SPSiteQuality.CloudCover =
      either("ExactCC", classOf[SPSiteQuality.CloudCover])
        .leftMap(ExactCc.fromExtinctionOrException)

    val wvEnum  = r.enumParameter(classOf[SPSiteQuality.WaterVapor])
    val sbEnum  = r.enumParameter(classOf[SPSiteQuality.SkyBackground])
    val airmass = r.doubleParameter("Airmass")

    ObservingConditions(iq, cc, wvEnum, sbEnum, airmass)
  }

  def instrumentName(r: ITCRequest): String =
    r.parameter("Instrument")

  def instrumentParameters(r: ITCRequest): InstrumentDetails = {
    import SPComponentType._
    val i = instrumentName(r)
    if      (i == INSTRUMENT_ACQCAM.readableStr)     acqCamParameters(r)
    else if (i == INSTRUMENT_FLAMINGOS2.readableStr) flamingos2Parameters(r)
    else if (i == INSTRUMENT_GHOST.readableStr)      ghostParameters(r)
    else if (i == INSTRUMENT_GMOS.readableStr)       gmosParameters(r)
    else if (i == INSTRUMENT_GMOSSOUTH.readableStr)  gmosParameters(r)
    else if (i == INSTRUMENT_GNIRS.readableStr)      gnirsParameters(r)
    else if (i == INSTRUMENT_GSAOI.readableStr)      gsaoiParameters(r)
    else if (i == INSTRUMENT_IGRINS2.readableStr)    igrins2Parameters(r)
    else if (i == INSTRUMENT_MICHELLE.readableStr)   michelleParameters(r)
    else if (i == INSTRUMENT_NIFS.readableStr)       nifsParameters(r)
    else if (i == INSTRUMENT_NIRI.readableStr)       niriParameters(r)
    else if (i == INSTRUMENT_TRECS.readableStr)      trecsParameters(r)
    else    sys.error(s"invalid instrument $i")
  }

  def acqCamParameters(r: ITCRequest): AcquisitionCamParameters = {
    val colorFilter = r.enumParameter(classOf[AcqCamParams.ColorFilter])
    val ndFilter    = r.enumParameter(classOf[AcqCamParams.NDFilter])
    AcquisitionCamParameters(colorFilter, ndFilter)
  }

  def flamingos2Parameters(r: ITCRequest): Flamingos2Parameters = {
    val filter      = r.enumParameter(classOf[Flamingos2.Filter])
    val grism       = r.enumParameter(classOf[Flamingos2.Disperser])
    val readMode    = r.enumParameter(classOf[Flamingos2.ReadMode])
    val fpMask      = r.enumParameter(classOf[Flamingos2.FPUnit])
    Flamingos2Parameters(filter, grism, fpMask, None, readMode)
  }

  // GHOST
  def ghostParameters(r: ITCRequest): GhostParameters = {

    val binning       = r.enumParameter(classOf[GhostBinning],"binning")
    val centralWl     = r.centralWavelengthInNanometers()
    val readMode      = r.enumParameter(classOf[GhostReadNoiseGain], "ReadMode")
    val resolution    = r.enumParameter(classOf[ResolutionMode],"instResolution")
    val nSkyMicrolens = ghostGetNumSky(r)

    GhostParameters(centralWl, nSkyMicrolens, resolution, readMode, binning)
  }

  def gmosParameters(r: ITCRequest): GmosParameters = {
    val site                              = r.enumParameter(classOf[Site])
    val filter: GmosCommonType.Filter     = if (site.equals(Site.GN)) r.enumParameter(classOf[FilterNorth],    "instrumentFilter")    else r.enumParameter(classOf[FilterSouth],    "instrumentFilter")
    val grating: GmosCommonType.Disperser = if (site.equals(Site.GN)) r.enumParameter(classOf[DisperserNorth], "instrumentDisperser") else r.enumParameter(classOf[DisperserSouth], "instrumentDisperser")
    val spatBinning                       = r.intParameter("spatBinning")
    val specBinning                       = if (r.parameter("instrumentDisperser") == "MIRROR") r.intParameter("spatBinning") else r.intParameter("specBinning")
    val ccdType                           = r.enumParameter(classOf[DetectorManufacturer])
    val centralWl                         = r.centralWavelengthInNanometers()
    val fpMask                            = fpMaskParameters(r)
    val customSlitWidth                   = customSlitWidthParameters(r)
    val ampGain                           = r.enumParameter(classOf[AmpGain])
    val ampReadMode                       = r.enumParameter(classOf[AmpReadMode])
    val builtinROI                        = r.enumParameter(classOf[GmosCommonType.BuiltinROI])
    GmosParameters(filter, grating, centralWl, fpMask, ampGain, ampReadMode, customSlitWidth, spatBinning, specBinning, ccdType, builtinROI, site)
  }

  /**
  * "null" values for grating in imaging mode and Filter in spectroscopy mode are introduced in order to
  * avoid "NONE" values for Disperser and Filter in GNIRSParams.java (since "NONE" wouldn't reflect
  * real instrument configuration)
  */
  def gnirsParameters(r: ITCRequest): GnirsParameters = {
    val grating     = r.parameter("Disperser") match {
      case "imaging" => None
      case _ => Some(r.enumParameter(classOf[GNIRSParams.Disperser])) }
    val filter      = r.parameter("Filter") match {
      case "spectroscopy" => None
      case _ => Some(r.enumParameter(classOf[GNIRSParams.Filter])) }
    val pixelScale = r.enumParameter(classOf[GNIRSParams.PixelScale])
    val xDisp       = r.enumParameter(classOf[GNIRSParams.CrossDispersed])
    val readMode    = r.enumParameter(classOf[GNIRSParams.ReadMode])
    val centralWl   = r.centralWavelengthInMicrons()
    val fpMask      = r.enumParameter(classOf[GNIRSParams.SlitWidth])
    val wellDepth   = r.enumParameter(classOf[GNIRSParams.WellDepth])
    val camera      = None                            //    are selected automatically and not controlled by user
    val altair      = altairParameters(r)
    GnirsParameters(pixelScale, filter, grating, readMode, xDisp, centralWl, fpMask, camera, wellDepth, altair)
  }

  def gsaoiParameters(r: ITCRequest): GsaoiParameters = {
    val filter      = r.enumParameter(classOf[Gsaoi.Filter])
    val readMode    = r.enumParameter(classOf[Gsaoi.ReadMode])
    val iq          = obsConditionParameters(r).iq
    val largeSkyOffset = r.intParameter("largeSkyOffset")
    ConfigExtractor.createGsaoiParameters(filter, readMode, iq, largeSkyOffset) match {
      case \/-(p) => p
      case -\/(t) => throw new IllegalArgumentException(t)
    }
  }

  def igrins2Parameters(r: ITCRequest): Igrins2Parameters = {
    val altair      = altairParameters(r)
    Igrins2Parameters(altair)
  }

  def michelleParameters(r: ITCRequest): MichelleParameters = {
    val filter      = r.enumParameter(classOf[MichelleParams.Filter])
    val grating     = r.enumParameter(classOf[MichelleParams.Disperser])
    val centralWl   = r.centralWavelengthInMicrons()
    val fpMask      = r.enumParameter(classOf[MichelleParams.Mask])
    val polarimetry = r.enumParameter(classOf[YesNoType], "polarimetry")
    MichelleParameters(filter, grating, centralWl, fpMask, polarimetry)
  }

  def niriParameters(r: ITCRequest): NiriParameters = {
    val filter      = r.enumParameter(classOf[Niri.Filter])
    val grism       = r.enumParameter(classOf[Niri.Disperser])
    val camera      = r.enumParameter(classOf[Niri.Camera])
    val readNoise   = r.enumParameter(classOf[Niri.ReadMode])
    val wellDepth   = r.enumParameter(classOf[Niri.WellDepth])
    val fpMask      = r.enumParameter(classOf[Niri.Mask])
    val builtinROI  = r.enumParameter(classOf[Niri.BuiltinROI])
    val altair      = altairParameters(r)
    NiriParameters(filter, grism, camera, readNoise, wellDepth, fpMask, builtinROI, altair)
  }

  def nifsParameters(r: ITCRequest): NifsParameters = {
    val filter      = r.enumParameter(classOf[NIFSParams.Filter])
    val grating     = r.enumParameter(classOf[NIFSParams.Disperser])
    val readNoise   = r.enumParameter(classOf[NIFSParams.ReadMode])
    val centralWl   = r.centralWavelengthInMicrons()
    val altair      = altairParameters(r)
    NifsParameters(filter, grating, readNoise, centralWl, altair)
   }

  def trecsParameters(r: ITCRequest): TRecsParameters = {
    val filter      = r.enumParameter(classOf[TReCSParams.Filter])
    val window      = r.enumParameter(classOf[TReCSParams.WindowWheel])
    val grating     = r.enumParameter(classOf[TReCSParams.Disperser])
    val centralWl   = r.centralWavelengthInMicrons()
    val fpMask      = r.enumParameter(classOf[TReCSParams.Mask])
    TRecsParameters(filter, window, grating, centralWl, fpMask)
  }

  def plotParameters(r: ITCRequest): PlottingDetails = {
    val limits  = r.enumParameter(classOf[PlottingDetails.PlotLimits])
    val lower   = r.doubleParameter("plotWavelengthL") * 1000 // microns -> nm
    val upper   = r.doubleParameter("plotWavelengthU") * 1000 // microns -> nm
    new PlottingDetails(limits, lower, upper)
  }

  def altairParameters(r: ITCRequest): Option[AltairParameters] = {
    val wfs                     = r.enumParameter(classOf[GuideProbe.Type])
    wfs match {
      case GuideProbe.Type.AOWFS =>
        val guideStarSeparation = r.doubleParameter("guideSep")
        val guideStarMagnitude  = r.doubleParameter("guideMag")
        val fieldLens           = r.enumParameter(classOf[AltairParams.FieldLens])
        val wfsMode             = r.enumParameter(classOf[AltairParams.GuideStarType])
        val altair              = AltairParameters(guideStarSeparation, guideStarMagnitude, fieldLens, wfsMode)
        Some(altair)

      case _ =>
        None
    }
  }

  def gemsParameters(r: ITCRequest): GemsParameters = {
    val avgStrehl  = r.doubleParameter("avgStrehl") / 100.0
    val strehlBand = r.parameter("strehlBand")
    GemsParameters(avgStrehl, strehlBand)
  }

  def fpMaskParameters(r: ITCRequest): GmosCommonType.FPUnit = {
    val site = r.enumParameter(classOf[Site])
    val fpMask = r.parameter("instrumentFPMask")
    fpMask match {
      case fpMask if fpMask.startsWith("CUSTOM_WIDTH") =>
        site match {
          case Site.GN => FPUnitNorth.CUSTOM_MASK.asInstanceOf[GmosCommonType.FPUnit]
          case Site.GS => FPUnitSouth.CUSTOM_MASK.asInstanceOf[GmosCommonType.FPUnit]
        }
      case _ =>
        site match {
          case Site.GN => r.enumParameter(classOf[FPUnitNorth], "instrumentFPMask")
          case Site.GS => r.enumParameter(classOf[FPUnitSouth], "instrumentFPMask")
        }
    }
  }

  def customSlitWidthParameters(r: ITCRequest): Option[GmosCommonType.CustomSlitWidth] = {
    val fpMask = r.parameter("instrumentFPMask")
    fpMask match {
      case fpMask if fpMask.startsWith("CUSTOM_WIDTH") =>
        val slitwidth = r.enumParameter(classOf[GmosCommonType.CustomSlitWidth], "instrumentFPMask")
        some(slitwidth)
      case _ =>
        None
    }
  }

  sealed trait CoaddsType
  case object CoaddsA extends CoaddsType
  case object CoaddsC extends CoaddsType
  case object CoaddsD extends CoaddsType
  case object CoaddsE extends CoaddsType

  def coadds(r: ITCRequest, t: CoaddsType): Option[Int] = {
    try {
      val ret = t match {
        case CoaddsA => r.intParameter("numCoaddsA")
        case CoaddsC => r.intParameter("numCoaddsC")
        case CoaddsD => r.intParameter("numCoaddsD")
        case CoaddsE => r.intParameter("numCoaddsE")
      }
      Some(ret)
    } catch {
      // We get this exception thrown if no coadds parameter was passed with the request
      case _: IllegalArgumentException =>
        Log.warning("No coadds parameter was passed with the request; using None.")
        None
    }
  }

  def observationParameters(r: ITCRequest, i: InstrumentDetails): ObservationDetails = {
    val calcMethod = r.parameter("calcMethod")
    val calculationMethod = calcMethod match {
      case "intTime"  if InstrumentDetails.isImaging(i)     =>
        ImagingExpCount(
          r.doubleParameter("sigmaC"),
          r.doubleParameter("expTimeC"),
          coadds(r, CoaddsC),
          r.doubleParameter("fracOnSourceC"),
          r.doubleParameter("offset")
        )
      case "expTime"  if InstrumentDetails.isImaging(i)     =>
        ImagingInt(
          r.doubleParameter("sigmaD"),
          coadds(r, CoaddsD),
          r.doubleParameter("fracOnSourceD"),
          r.doubleParameter("offset")
        )
      case "s2n"      if InstrumentDetails.isImaging(i)     =>
        ImagingS2N(
          r.intParameter("numExpA"),
          coadds(r, CoaddsA),
          r.doubleParameter("expTimeA"),
          r.doubleParameter("fracOnSourceA"),
          r.doubleParameter("offset")
        )
      case "s2n"      if InstrumentDetails.isSpectroscopy(i) =>
        SpectroscopyS2N(
          r.intParameter("numExpA"),
          coadds(r, CoaddsA),
          r.doubleParameter("expTimeA"),
          r.doubleParameter("fracOnSourceA"),
          r.doubleParameter("offset"),
          None
        )
      case "intTimeSpec" if InstrumentDetails.isSpectroscopy(i) =>
        SpectroscopyInt(
          r.doubleParameter("sigmaE"),
          r.doubleParameter("wavelengthE"),
          coadds(r, CoaddsE),
          r.doubleParameter("fracOnSourceE"),
          r.doubleParameter("offset")
        )

      case _ => throw new IllegalArgumentException(
        "An incompatible calculation method is selected.\n" +
        "Please select a spectroscopic calculation method for\n" +
        "spectroscopy or an imaging calculation method for imaging.")
    }

    ObservationDetails(calculationMethod, analysisMethod(r))

  }

  def sourceDefinitionParameters(r: ITCRequest): SourceDefinition = {

    def magnitudeSystemFor(s: String) = s match {
      case "MAG"                  => MagnitudeSystem.Vega
      case "ABMAG"                => MagnitudeSystem.AB
      case "JY"                   => MagnitudeSystem.Jy
      case "WATTS"                => MagnitudeSystem.Watts
      case "ERGS_WAVELENGTH"      => MagnitudeSystem.ErgsWavelength
      case "ERGS_FREQUENCY"       => MagnitudeSystem.ErgsFrequency
    }
    def surfaceBrightnessFor(s: String) = s match {
      case "MAG_PSA"              => SurfaceBrightness.Vega
      case "ABMAG_PSA"            => SurfaceBrightness.AB
      case "JY_PSA"               => SurfaceBrightness.Jy
      case "WATTS_PSA"            => SurfaceBrightness.Watts
      case "ERGS_WAVELENGTH_PSA"  => SurfaceBrightness.ErgsWavelength
      case "ERGS_FREQUENCY_PSA"   => SurfaceBrightness.ErgsFrequency
    }

    // Get the source geometry and type
    val profileName = r.parameter("Profile")
    val (spatialProfile, norm, units) = profileName match {
      case "POINT"    =>
        val norm  = r.doubleParameter("psSourceNorm")
        val units = magnitudeSystemFor(r.parameter("psSourceUnits"))
        (PointSource, norm, units)
      case "GAUSSIAN" =>
        val norm  = r.doubleParameter("gaussSourceNorm")
        val units = magnitudeSystemFor(r.parameter("gaussSourceUnits"))
        val fwhm  = r.doubleParameter("gaussFwhm")
        (GaussianSource(fwhm), norm, units)
      case "UNIFORM"  =>
        val norm  = r.doubleParameter("usbSourceNorm")
        val units = surfaceBrightnessFor(r.parameter("usbSourceUnits"))
        (UniformSource, norm, units)
      case _          =>
        throw new NoSuchElementException(s"Unknown SpatialProfile $profileName")
    }

    // Get Normalization info
    val bandName = r.parameter("WavebandDefinition")
    val normBand = MagnitudeBand.all.
      find(_.name == bandName).
      getOrElse(sys.error(s"Unsupported wave band $bandName"))

    // Get Spectrum Resource
    val distributionName = r.parameter("Distribution")
    val sourceDefinition = distributionName match {
      case "BBODY"            => BlackBody(r.doubleParameter("BBTemp"))
      case "PLAW"             => PowerLaw(r.doubleParameter("powerIndex"))
      case "USER_DEFINED"     => r.userSpectrum().get
      case "LIBRARY_STAR"     => LibraryStar.findByName(r.parameter("stSpectrumType")).get
      case "LIBRARY_NON_STAR" => LibraryNonStar.findByName(r.parameter("nsSpectrumType")).get
      case "ELINE"            =>
        val flux = r.doubleParameter("lineFlux")
        val cont = r.doubleParameter("lineContinuum")
        EmissionLine(
          r.doubleParameter("lineWavelength").microns,
          r.doubleParameter("lineWidth").kps,
          if (r.parameter("lineFluxUnits") == "watts_flux") flux.wattsPerSquareMeter else flux.ergsPerSecondPerSquareCentimeter,
          if (r.parameter("lineContinuumUnits") == "watts_fd_wavelength") cont.wattsPerSquareMeterPerMicron else cont.ergsPerSecondPerSquareCentimeterPerAngstrom
        )
      case _                  =>
        throw new NoSuchElementException(s"Unknown SpectralDistribution $distributionName")
    }

    //Get Redshift
    val redshiftName = r.parameter("Recession")
    val redshift = redshiftName match {
      case "REDSHIFT" => Redshift(r.doubleParameter("z"))
      case "VELOCITY" => Redshift.fromApparentRadialVelocity(KilometersPerSecond(r.doubleParameter("v")))
      case _          => throw new NoSuchElementException(s"Unknown Recession $redshiftName")
    }

    // WOW, finally we've got everything in place..
    SourceDefinition(spatialProfile, sourceDefinition, norm, units, normBand, redshift)
  }

  def ghostGetNumSky(r:ITCRequest): Int = {
    val res = r.enumParameter(classOf[ResolutionMode],"instResolution")
    if (res == ResolutionMode.GhostStandard)
      r.intParameter("nSkyMicrolens");
    else
      7
  }

  def analysisMethod(r: ITCRequest): AnalysisMethod = r.parameter("analysisMethod") match {
    case "autoAper"   => AutoAperture(r.doubleParameter("autoSkyAper"))
    case "userAper"   => UserAperture(r.doubleParameter("userAperDiam"), r.doubleParameter("userSkyAper"))
    case "singleIFU"  => IfuSingle(r.intParameter("ifuSkyFibres"), r.doubleParameter("ifuOffset"))
    case "radialIFU"  => IfuRadial(r.intParameter("ifuSkyFibres"), r.doubleParameter("ifuMinOffset"), r.doubleParameter("ifuMaxOffset"))
    case "summedIFU"  => IfuSummed(r.intParameter("ifuSkyFibres"), r.intParameter("ifuNumX"), r.intParameter("ifuNumY"), r.doubleParameter("ifuCenterX"), r.doubleParameter("ifuCenterY"))
    case "sumIFU"     => IfuSum(r.intParameter("ifuSkyFibres"), r.doubleParameter("ifuNum"), r.parameter("instrumentFPMask")=="IFU_1") // IFU_1 = IFU-2
    case "ifuSky"     => Ifu(ghostGetNumSky(r)) // This new class is created to the first aproximation to Ghost
                                                // where the number of fibers to sky depending of the Number of sky microlens choose.
                                                // This value is only taken in account in the finalS2N function as a element of the noise factor. noiseFactor = 1 + (1 / skyAper);
    case _            => throw new NoSuchElementException(s"Unknown analysis method ${r.parameter("analysisMethod")}")
  }

  def parameters(r: ITCRequest, i: InstrumentDetails): ItcParameters = {
    val source        = ITCRequest.sourceDefinitionParameters(r)
    val observation   = ITCRequest.observationParameters(r, i)
    val conditions    = ITCRequest.obsConditionParameters(r)
    val telescope     = ITCRequest.teleParameters(r)
    ItcParameters(source, observation, conditions, telescope, i)
  }

}
