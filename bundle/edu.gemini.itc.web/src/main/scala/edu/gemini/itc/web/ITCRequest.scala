package edu.gemini.itc.web

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
import edu.gemini.spModel.gemini.gmos.GmosCommonType.{BuiltinROI, AmpGain, AmpReadMode, DetectorManufacturer}
import edu.gemini.spModel.gemini.gmos.GmosNorthType.{DisperserNorth, FPUnitNorth, FilterNorth}
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

import scalaz.{-\/, \/-}

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

  /** Gets the named value as an enum of type {{{Class[T]}}} using the simple name of the class as the parameter name. */
  def enumParameter[T <: Enum[T]](c: Class[T]): T = enumParameter(c, c.getSimpleName)

  /** Gets the named value as an enum of type {{{Class[T]}}} using the given name as the parameter name. */
  def enumParameter[T <: Enum[T]](c: Class[T], n: String): T = Enum.valueOf(c, parameter(n))

  /** Gets the named value as an integer. */
  def intParameter(name: String): Int = parameter(name).trim() match {
    case ""   => 0
    case i    =>
      try i.toInt catch {
        case _: NumberFormatException => throw new IllegalArgumentException(s"$i is not a valid integer number value for parameter $name")
      }
  }

  /** Gets the named value as an integer. */
  def intParameterCoadd(name: String): Int = parameter(name).trim() match {
    case ""   => 0
    case i    =>
      try i.toInt catch {
        case _: IllegalArgumentException => 1
      }
  }

  /** Gets the named value as a boolean, it accepts several types of strings as true/false. */
  def booleanParameter(name: String): Boolean = java.lang.Boolean.parseBoolean(parameter(name).trim())

  /** Gets the named value as a double. */
  def doubleParameter(name: String): Double = parameter(name).trim() match {
    case ""   => 0.0
    case d    =>
      try d.toDouble catch {
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
    val iq      = r.enumParameter(classOf[SPSiteQuality.ImageQuality])
    val cc      = r.enumParameter(classOf[SPSiteQuality.CloudCover])
    val wv      = r.enumParameter(classOf[SPSiteQuality.WaterVapor])
    val sb      = r.enumParameter(classOf[SPSiteQuality.SkyBackground])
    val airmass = r.doubleParameter("Airmass")
    ObservingConditions(iq, cc, wv, sb, airmass)
  }

  def instrumentName(r: ITCRequest): String =
    r.parameter("Instrument")

  def instrumentParameters(r: ITCRequest): InstrumentDetails = {
    import SPComponentType._
    val i = instrumentName(r)
    if      (i == INSTRUMENT_ACQCAM.readableStr)     acqCamParameters(r)
    else if (i == INSTRUMENT_FLAMINGOS2.readableStr) flamingos2Parameters(r)
    else if (i == INSTRUMENT_GMOS.readableStr)       gmosParameters(r)
    else if (i == INSTRUMENT_GMOSSOUTH.readableStr)  gmosParameters(r)
    else if (i == INSTRUMENT_GNIRS.readableStr)      gnirsParameters(r)
    else if (i == INSTRUMENT_GSAOI.readableStr)      gsaoiParameters(r)
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

  def gmosParameters(r: ITCRequest): GmosParameters = {
    val site                              = r.enumParameter(classOf[Site])
    val filter: GmosCommonType.Filter     = if (site.equals(Site.GN)) r.enumParameter(classOf[FilterNorth],    "instrumentFilter")    else r.enumParameter(classOf[FilterSouth],    "instrumentFilter")
    val grating: GmosCommonType.Disperser = if (site.equals(Site.GN)) r.enumParameter(classOf[DisperserNorth], "instrumentDisperser") else r.enumParameter(classOf[DisperserSouth], "instrumentDisperser")
    val spatBinning                       = r.intParameter("spatBinning")
    val specBinning                       = r.intParameter("specBinning")
    val ccdType                           = r.enumParameter(classOf[DetectorManufacturer])
    val centralWl                         = r.centralWavelengthInNanometers()
    val fpMask: GmosCommonType.FPUnit     = if (site.equals(Site.GN)) r.enumParameter(classOf[FPUnitNorth],    "instrumentFPMask")   else r.enumParameter(classOf[FPUnitSouth],      "instrumentFPMask")
    val ampGain                           = r.enumParameter(classOf[AmpGain])
    val ampReadMode                       = r.enumParameter(classOf[AmpReadMode])
    val builtinROI                        = r.enumParameter(classOf[GmosCommonType.BuiltinROI])
    GmosParameters(filter, grating, centralWl, fpMask, ampGain, ampReadMode, None, spatBinning, specBinning, ccdType, builtinROI, site)
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

  sealed trait CoaddsType
  case object CoaddsA extends CoaddsType
  case object CoaddsC extends CoaddsType

  def coadds(r: ITCRequest, t: CoaddsType): Option[Int] = {
    try {
      val ret = t match {
        case CoaddsA => r.intParameter("numCoaddsA")
        case CoaddsC => r.intParameter("numCoaddsC")
      }
      Some(ret)
    } catch {
      // We get this exception thrown if no coadds parameter was passed with the request
      case ex: IllegalArgumentException => None
    }
  }

  def observationParameters(r: ITCRequest, i: InstrumentDetails): ObservationDetails = {
    val calcMethod = r.parameter("calcMethod")
    val calculationMethod = calcMethod match {
      case "intTime"  if InstrumentDetails.isImaging(i)     =>
        ImagingInt(
          r.doubleParameter("sigmaC"),
          r.doubleParameter("expTimeC"),
          coadds(r, CoaddsC),
          r.doubleParameter("fracOnSourceC"),
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
          r.doubleParameter("offset")
        )
      case _ => throw new IllegalArgumentException("Total integration time to achieve a specific \nS/N ratio is not supported in spectroscopy mode.  \nPlease select the Total S/N method.")
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

  def analysisMethod(r: ITCRequest): AnalysisMethod = r.parameter("analysisMethod") match {
    case "autoAper"   => AutoAperture(r.doubleParameter("autoSkyAper"))
    case "userAper"   => UserAperture(r.doubleParameter("userAperDiam"), r.doubleParameter("userSkyAper"))
    case "singleIFU"  => IfuSingle(r.intParameter("ifuSkyFibres"), r.doubleParameter("ifuOffset"))
    case "radialIFU"  => IfuRadial(r.intParameter("ifuSkyFibres"), r.doubleParameter("ifuMinOffset"), r.doubleParameter("ifuMaxOffset"))
    case "summedIFU"  => IfuSummed(r.intParameter("ifuSkyFibres"), r.intParameter("ifuNumX"), r.intParameter("ifuNumY"), r.doubleParameter("ifuCenterX"), r.doubleParameter("ifuCenterY"))
    case "sumIFU"     => IfuSum(r.intParameter("ifuSkyFibres"), r.doubleParameter("ifuNum"), r.parameter("instrumentFPMask")=="IFU_1") // IFU_1 = IFU-2
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
