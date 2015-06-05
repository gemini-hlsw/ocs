package edu.gemini.itc.web

import javax.servlet.http.HttpServletRequest

import edu.gemini.itc.base._
import edu.gemini.itc.gnirs.GnirsParameters
import edu.gemini.itc.michelle.MichelleParameters
import edu.gemini.itc.nifs.NifsParameters
import edu.gemini.itc.shared.SourceDefinition.{Recession, Distribution, Profile}
import edu.gemini.itc.shared._
import edu.gemini.itc.trecs.TRecsParameters
import edu.gemini.spModel.core.{Site, Wavelength}
import edu.gemini.spModel.gemini.acqcam.AcqCamParams
import edu.gemini.spModel.gemini.altair.AltairParams
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.GmosCommonType.DetectorManufacturer
import edu.gemini.spModel.gemini.gmos.GmosNorthType.{DisperserNorth, FPUnitNorth, FilterNorth}
import edu.gemini.spModel.gemini.gmos.GmosSouthType.{DisperserSouth, FPUnitSouth, FilterSouth}
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.michelle.MichelleParams
import edu.gemini.spModel.gemini.niri.Niri
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.EmissionLine.{Continuum, Flux}
import edu.gemini.spModel.target._
import edu.gemini.spModel.telescope.IssPort

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
        case e: NumberFormatException => throw new IllegalArgumentException(s"$i is not a valid integer number value for parameter $name")
      }
  }

  /** Gets the named value as a double. */
  def doubleParameter(name: String): Double = parameter(name).trim() match {
    case ""   => 0.0
    case d    =>
      try d.toDouble catch {
        case e: NumberFormatException => throw new IllegalArgumentException(s"$d is not a valid double number value for parameter $name")
      }
  }

  /** Gets the user SED text file from the request.
    * Only multipart HTTP requests will support this. */
  def userSpectrum(): Option[String]

}

/**
 * Utility object that allows to translate different objects into ITC requests.
 */
object ITCRequest {

  def from(request: HttpServletRequest): ITCRequest = new ITCRequest {
    override def parameter(name: String): String = request.getParameter(name)
    override def userSpectrum(): Option[String] = None
  }
  def from(request: ITCMultiPartParser): ITCRequest = new ITCRequest {
    override def parameter(name: String): String = request.getParameter(name)
    override def userSpectrum(): Option[String] = Some(request.getTextFile("specUserDef"))
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
    new ObservingConditions(iq, cc, wv, sb, airmass)
  }

  def acqCamParameters(r: ITCRequest): AcquisitionCamParameters = {
    val colorFilter = r.enumParameter(classOf[AcqCamParams.ColorFilter])
    val ndFilter    = r.enumParameter(classOf[AcqCamParams.NDFilter])

    new AcquisitionCamParameters(colorFilter, ndFilter)
  }

  def flamingos2Parameters(r: ITCRequest): Flamingos2Parameters = {
    val filter      = r.enumParameter(classOf[Flamingos2.Filter])
    val grism       = r.enumParameter(classOf[Flamingos2.Disperser])
    val readMode    = r.enumParameter(classOf[Flamingos2.ReadMode])
    val fpMask      = r.enumParameter(classOf[Flamingos2.FPUnit])
    new Flamingos2Parameters(filter, grism, fpMask, readMode)
  }

  def gmosParameters(r: ITCRequest): GmosParameters = {
    val site        = r.enumParameter(classOf[Site])
    val filter      = if (site.equals(Site.GN)) r.enumParameter(classOf[FilterNorth],    "instrumentFilter")    else r.enumParameter(classOf[FilterSouth],    "instrumentFilter")
    val grating     = if (site.equals(Site.GN)) r.enumParameter(classOf[DisperserNorth], "instrumentDisperser") else r.enumParameter(classOf[DisperserSouth], "instrumentDisperser")
    val spatBinning = r.intParameter("spatBinning")
    val specBinning = r.intParameter("specBinning")
    val ccdType     = r.enumParameter(classOf[DetectorManufacturer])
    val centralWl   = Wavelength.fromNanometers(r.doubleParameter("instrumentCentralWavelength"))
    val fpMask      = if (site.equals(Site.GN)) r.enumParameter(classOf[FPUnitNorth],    "instrumentFPMask")   else r.enumParameter(classOf[FPUnitSouth],      "instrumentFPMask")
    val ifuMethod: Option[IfuMethod]   = if (fpMask.isIFU) {
      r.parameter("ifuMethod") match {
        case "singleIFU" => Some(IfuSingle(r.doubleParameter("ifuOffset")))
        case "radialIFU" => Some(IfuRadial(r.doubleParameter("ifuMinOffset"), r.doubleParameter("ifuMaxOffset")))
        case _ => throw new IllegalArgumentException()
      }} else {
      None
    }

    GmosParameters(filter, grating, centralWl, fpMask, None, spatBinning, specBinning, ifuMethod, ccdType, site)
  }

  def gnirsParameters(r: ITCRequest): GnirsParameters = {
    val grating     = r.enumParameter(classOf[GNIRSParams.Disperser])
    val camera      = r.enumParameter(classOf[GNIRSParams.PixelScale])
    val xDisp       = r.parameter("xdisp")
    val readMode   = r.enumParameter(classOf[GNIRSParams.ReadMode])
    val centralWl   = Wavelength.fromMicrons(r.doubleParameter("instrumentCentralWavelength"))
    val fpMask      = r.parameter("instrumentFPMask")
    new GnirsParameters(camera, grating, readMode, xDisp, centralWl, fpMask)
  }

  def gsaoiParameters(r: ITCRequest): GsaoiParameters = {
    val filter      = r.enumParameter(classOf[Gsaoi.Filter])
    val readMode    = r.enumParameter(classOf[Gsaoi.ReadMode])
    val gems        = gemsParameters(r)
    new GsaoiParameters(filter, readMode, gems)
  }

  def michelleParameters(r: ITCRequest): MichelleParameters = {
    val filter      = r.parameter("instrumentFilter")
    val grating     = r.parameter("instrumentDisperser")
    val centralWl   = Wavelength.fromMicrons(r.doubleParameter("instrumentCentralWavelength"))
    val fpMask      = r.enumParameter(classOf[MichelleParams.Mask])
    val polarimetry = r.parameter("polarimetry")
    new MichelleParameters(filter, grating, centralWl, fpMask, polarimetry)
  }

  def niriParameters(r: ITCRequest): NiriParameters = {
    val filter      = r.enumParameter(classOf[Niri.Filter])
    val grism       = r.enumParameter(classOf[Niri.Disperser])
    val camera      = r.enumParameter(classOf[Niri.Camera])
    val readNoise   = r.enumParameter(classOf[Niri.ReadMode])
    val wellDepth   = r.enumParameter(classOf[Niri.WellDepth])
    val fpMask      = r.enumParameter(classOf[Niri.Mask])
    val altair      = altairParameters(r)
    new NiriParameters(filter, grism, camera, readNoise, wellDepth, fpMask, altair)
  }

  def nifsParameters(r: ITCRequest): NifsParameters = {
    val filter      = r.parameter("instrumentFilter")
    val grating     = r.parameter("instrumentDisperser")
    val readNoise   = r.parameter("readNoise")
    val centralWl   = Wavelength.fromMicrons(r.doubleParameter("instrumentCentralWavelength"))
    val ifuMethod   = r.parameter("ifuMethod")
    val (offset, min, max, numX, numY, centerX, centerY) = ifuMethod match {
      case "singleIFU"  =>
        val offset = r.parameter("ifuOffset")
        (offset, "", "", "", "", "", "")
      case "radialIFU"  =>
        val min = r.parameter("ifuMinOffset")
        val max = r.parameter("ifuMaxOffset")
        ("", min, max, "", "", "", "")
      case "summedApertureIFU" =>
        val numX = r.parameter("ifuNumX")
        val numY = r.parameter("ifuNumY")
        val cenX = r.parameter("ifuCenterX")
        val cenY = r.parameter("ifuCenterY")
        ("", "", "", numX, numY, cenX, cenY)
    }
    val altair = altairParameters(r)
    new NifsParameters(filter, grating, readNoise, centralWl, ifuMethod, offset, min, max, numX, numY, centerX, centerY, altair)
   }

  def trecsParameters(r: ITCRequest): TRecsParameters = {
    val filter      = r.parameter("instrumentFilter")
    val window      = r.parameter("instrumentWindow")
    val grating     = r.parameter("instrumentDisperser")
    val centralWl   = Wavelength.fromMicrons(r.doubleParameter("instrumentCentralWavelength"))
    val fpMask      = r.parameter("instrumentFPMask")
    new TRecsParameters(filter, window, grating, centralWl, fpMask)
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
        val altair              = new AltairParameters(guideStarSeparation, guideStarMagnitude, fieldLens, wfsMode)
        new Some(altair)

      case _ =>
        None
    }
  }

  def gemsParameters(r: ITCRequest): GemsParameters = {
    val avgStrehl  = r.doubleParameter("avgStrehl") / 100.0
    val strehlBand = r.parameter("strehlBand")
    new GemsParameters(avgStrehl, strehlBand)
  }

  def observationParameters(r: ITCRequest): ObservationDetails = {
    val calcMode   = r.parameter("calcMode")
    val calcMethod = r.parameter("calcMethod")
    val calculationMethod = (calcMode, calcMethod) match {
      case ("imaging", "intTime")     =>
        ImagingInt(
          r.doubleParameter("sigmaC"),
          r.doubleParameter("expTimeC"),
          r.doubleParameter("fracOnSourceC")
        )
      case ("imaging", "s2n")         =>
        ImagingSN(
          r.intParameter("numExpA"),
          r.doubleParameter("expTimeA"),
          r.doubleParameter("fracOnSourceA")
        )
      case ("spectroscopy", "s2n")    =>
        SpectroscopySN(
          r.intParameter("numExpA"),
          r.doubleParameter("expTimeA"),
          r.doubleParameter("fracOnSourceA")
        )
      case _ => throw new IllegalArgumentException("Total integration time to achieve a specific \nS/N ratio is not supported in spectroscopy mode.  \nPlease select the Total S/N method.")
    }

    val analysisMethod = r.parameter("aperType") match {
      case "autoAper" => AutoAperture(r.doubleParameter("autoSkyAper"))
      case "userAper" => UserAperture(r.doubleParameter("userAperDiam"), r.doubleParameter("userSkyAper"))
    }

    new ObservationDetails(calculationMethod, analysisMethod)

  }

  def sourceDefinitionParameters(r: ITCRequest): SourceDefinition = {
    // Get the source geometry and type
    import SourceDefinition.Profile._
    val (spatialProfile, norm, units) = r.enumParameter(classOf[Profile]) match {
      case POINT    =>
        val norm  = r.doubleParameter("psSourceNorm")
        val units = r.enumParameter(classOf[BrightnessUnit], "psSourceUnits")
        (PointSource(), norm, units)
      case GAUSSIAN =>
        val norm  = r.doubleParameter("gaussSourceNorm")
        val units = r.enumParameter(classOf[BrightnessUnit], "gaussSourceUnits")
        val fwhm  = r.doubleParameter("gaussFwhm")
        (GaussianSource(fwhm), norm, units)
      case UNIFORM  =>
        val norm  = r.doubleParameter("usbSourceNorm")
        val units = r.enumParameter(classOf[BrightnessUnit], "usbSourceUnits")
        (UniformSource(), norm, units)
    }

    // Get Normalization info
    val normBand = r.enumParameter(classOf[WavebandDefinition])

    // Get Spectrum Resource
    import SourceDefinition.Distribution._
    val sourceSpec = r.enumParameter(classOf[Distribution])
    val sourceDefinition = sourceSpec match {
      case BBODY =>             BlackBody(r.doubleParameter("BBTemp"))
      case PLAW =>              PowerLaw(r.doubleParameter("powerIndex"))
      case USER_DEFINED =>      UserDefined(r.userSpectrum().get)
      case LIBRARY_STAR =>      LibraryStar.findByName(r.parameter("stSpectrumType")).get
      case LIBRARY_NON_STAR =>  LibraryNonStar.findByName(r.parameter("nsSpectrumType")).get
      case ELINE =>
        val flux = r.doubleParameter("lineFlux")
        val cont = r.doubleParameter("lineContinuum")
        EmissionLine(
          Wavelength.fromMicrons(r.doubleParameter("lineWavelength")),
          r.doubleParameter("lineWidth"),
          if (r.parameter("lineFluxUnits") == "watts_flux") Flux.fromWatts(flux) else Flux.fromErgs(flux),
          if (r.parameter("lineContinuumUnits") == "watts_fd_wavelength") Continuum.fromWatts(cont) else Continuum.fromErgs(cont)
        )
    }

    //Get Redshift
    import SourceDefinition.Recession._
    val recession = r.enumParameter(classOf[Recession])
    val redshift = recession match {
      case REDSHIFT => r.doubleParameter("z")
      case VELOCITY => r.doubleParameter("v") / ITCConstants.C
    }

    // WOW, finally we've got everything in place..
    new SourceDefinition(spatialProfile, sourceDefinition, norm, units, normBand, redshift)
  }

  def parameters(r: ITCRequest): Parameters = {
    val source        = ITCRequest.sourceDefinitionParameters(r)
    val observation   = ITCRequest.observationParameters(r)
    val conditions    = ITCRequest.obsConditionParameters(r)
    val telescope     = ITCRequest.teleParameters(r)
    Parameters(source, observation, conditions, telescope)
  }

}
