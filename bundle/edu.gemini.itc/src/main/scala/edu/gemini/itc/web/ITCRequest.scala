package edu.gemini.itc.web

import javax.servlet.http.HttpServletRequest

import edu.gemini.itc.altair.AltairParameters
import edu.gemini.itc.service.SourceDefinition._
import edu.gemini.itc.service._
import edu.gemini.itc.shared._
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.altair.AltairParams
import edu.gemini.spModel.gemini.gmos.GmosCommonType.DetectorManufacturer
import edu.gemini.spModel.gemini.gmos.GmosNorthType.{DisperserNorth, FPUnitNorth, FilterNorth}
import edu.gemini.spModel.gemini.gmos.GmosSouthType.{DisperserSouth, FPUnitSouth, FilterSouth}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.telescope.IssPort

/**
 * ITC requests define a generic mechanism to look up values by their parameter names.
 * The different values are either enums, ints or doubles. For enums the simple name of the class is used
 * as the parameter name. This convention allows for a mostly mechanical translation of typed values from
 * a request with string based parameters, i.e. parameters in HttpServletRequests for example.
 */
sealed abstract class ITCRequest {
  def parameter(name: String): String

  /** Gets the named value as an enum of type {{{Class[T]}}} using the simple name of the class as the parameter name. */
  def enumParameter[T <: Enum[T]](c: Class[T]): T = enumParameter(c, c.getSimpleName)

  /** Gets the named value as an enum of type {{{Class[T]}}} using the given name as the parameter name. */
  def enumParameter[T <: Enum[T]](c: Class[T], n: String): T = Enum.valueOf(c, parameter(n))

  /** Gets the named value as an integer. */
  def intParameter(name: String): Int = parameter(name).toInt

  /** Gets the named value as a double. */
  def doubleParameter(name: String): Double = parameter(name).toDouble

  /** Gets the user SED text file from the request.
    * Only multipart HTTP requests will support this. */
  def userSpectrum(): Option[String]

  /** Gets the user SED text file name from the request.
    * Only multipart HTTP requests will support this. */
  // TODO: Can we get rid of this method?
   def userSpectrumName(): Option[String]
}

/**
 * Utility object that allows to translate different objects into ITC requests.
 */
object ITCRequest {

  def from(request: HttpServletRequest): ITCRequest = new ITCRequest {
    override def parameter(name: String): String = request.getParameter(name)
    override def userSpectrum(): Option[String] = None
    override def userSpectrumName(): Option[String] = None
  }
  def from(request: ITCMultiPartParser): ITCRequest = new ITCRequest {
    override def parameter(name: String): String = request.getParameter(name)
    override def userSpectrum(): Option[String] = Some(request.getTextFile("specUserDef"))
    override def userSpectrumName(): Option[String] = Some(request.getRemoteFileName("specUserDef"))
  }

  def teleParameters(r: ITCMultiPartParser): TelescopeDetails = {
    val pc      = ITCRequest.from(r)
    val coating = pc.enumParameter(classOf[TelescopeDetails.Coating])
    val port    = pc.enumParameter(classOf[IssPort])
    val wfs     = pc.enumParameter(classOf[TelescopeDetails.Wfs])
    new TelescopeDetails(coating, port, wfs)
  }

  def obsConditionParameters(r: ITCMultiPartParser): ObservingConditions = {
    val pc      = ITCRequest.from(r)
    val iq      = pc.enumParameter(classOf[SPSiteQuality.ImageQuality])
    val cc      = pc.enumParameter(classOf[SPSiteQuality.CloudCover])
    val wv      = pc.enumParameter(classOf[SPSiteQuality.WaterVapor])
    val sb      = pc.enumParameter(classOf[SPSiteQuality.SkyBackground])
    val airmass = pc.doubleParameter("Airmass")
    new ObservingConditions(iq, cc, wv, sb, airmass)
  }

  def gmosParameters(r: ITCMultiPartParser): GmosParameters = {
    val pc          = ITCRequest.from(r)
    val site        = pc.enumParameter(classOf[Site])
    val filter      = if (site.equals(Site.GN)) pc.enumParameter(classOf[FilterNorth],    "instrumentFilter")    else pc.enumParameter(classOf[FilterSouth],    "instrumentFilter")
    val grating     = if (site.equals(Site.GN)) pc.enumParameter(classOf[DisperserNorth], "instrumentDisperser") else pc.enumParameter(classOf[DisperserSouth], "instrumentDisperser")
    val spatBinning = pc.intParameter("spatBinning")
    val specBinning = pc.intParameter("specBinning")
    val ccdType     = pc.enumParameter(classOf[DetectorManufacturer])
    val centralWavelength = if (pc.parameter("instrumentCentralWavelength").trim.isEmpty) 0.0 else pc.doubleParameter("instrumentCentralWavelength")
    val fpMask      = if (site.equals(Site.GN)) pc.enumParameter(classOf[FPUnitNorth],    "instrumentFPMask")   else pc.enumParameter(classOf[FPUnitSouth],      "instrumentFPMask")
    val ifuMethod: Option[IfuMethod]   = if (fpMask.isIFU) {
      pc.parameter("ifuMethod") match {
        case "singleIFU" => Some(IfuSingle(pc.doubleParameter("ifuOffset")))
        case "radialIFU" => Some(IfuRadial(pc.doubleParameter("ifuMinOffset"), pc.doubleParameter("ifuMaxOffset")))
        case _ => throw new IllegalArgumentException()
      }} else {
      None
    }

    new GmosParameters(filter, grating, centralWavelength, fpMask, spatBinning, specBinning, ifuMethod, ccdType, site)
  }

  def plotParamters(r: ITCMultiPartParser): PlottingDetails = {
    val pc      = ITCRequest.from(r)
    val limits  = pc.enumParameter(classOf[PlottingDetails.PlotLimits])
    val lower   = pc.doubleParameter("plotWavelengthL")
    val upper   = pc.doubleParameter("plotWavelengthU")
    new PlottingDetails(limits, lower, upper)
  }

  def altairParameters(r: ITCMultiPartParser): AltairParameters = {
    val pc      = ITCRequest.from(r)
    val guideStarSeperation  = pc.doubleParameter("guideSep")
    val guideStarMagnitude   = pc.doubleParameter("guideMag")
    val fieldLens            = pc.enumParameter(classOf[AltairParams.FieldLens])
    val wfsMode              = pc.enumParameter(classOf[AltairParams.GuideStarType])
    val wfs                  = pc.enumParameter(classOf[TelescopeDetails.Wfs])
    val altairUsed           = wfs eq TelescopeDetails.Wfs.AOWFS
    new AltairParameters(guideStarSeperation, guideStarMagnitude, fieldLens, wfsMode, altairUsed)
  }

  def observationParameters(r: ITCMultiPartParser): ObservationDetails = {
    val pc = ITCRequest.from(r)

    val calcMode   = pc.parameter("calcMode")
    val calcMethod = pc.parameter("calcMethod")
    val calculationMethod = (calcMode, calcMethod) match {
      case ("imaging", "intTime")     =>
        ImagingInt(
          pc.doubleParameter("sigmaC"),
          pc.doubleParameter("expTimeC"),
          pc.doubleParameter("fracOnSourceC")
        )
      case ("imaging", "s2n")         =>
        ImagingSN(
          pc.intParameter("numExpA"),
          pc.doubleParameter("expTimeA"),
          pc.doubleParameter("fracOnSourceA")
        )
      case ("spectroscopy", "s2n")    =>
        SpectroscopySN(
          pc.intParameter("numExpA"),
          pc.doubleParameter("expTimeA"),
          pc.doubleParameter("fracOnSourceA")
        )
      case _ => throw new IllegalArgumentException("Total integration time to achieve a specific \nS/N ratio is not supported in spectroscopy mode.  \nPlease select the Total S/N method.")
    }

    val analysisMethod = pc.parameter("aperType") match {
      case "autoAper" => AutoAperture(pc.doubleParameter("autoSkyAper"))
      case "userAper" => UserAperture(pc.doubleParameter("userAperDiam"), pc.doubleParameter("userSkyAper"))
    }

    new ObservationDetails(calculationMethod, analysisMethod)

  }

  def sourceDefinitionParameters(r: ITCMultiPartParser): SourceDefinition = {
    val pc = ITCRequest.from(r)

    // Get the source geometry and type
    import edu.gemini.itc.service.SourceDefinition.Profile._
    val spatialProfile = pc.enumParameter(classOf[Profile]) match {
      case POINT    =>
        val norm  = pc.doubleParameter("psSourceNorm")
        val units = pc.enumParameter(classOf[BrightnessUnit], "psSourceUnits")
        PointSource(norm, units)
      case GAUSSIAN =>
        val norm  = pc.doubleParameter("gaussSourceNorm")
        val units = pc.enumParameter(classOf[BrightnessUnit], "gaussSourceUnits")
        val fwhm  = pc.doubleParameter("gaussFwhm")
        GaussianSource(norm, units, fwhm)
      case UNIFORM  =>
        val norm  = pc.doubleParameter("usbSourceNorm")
        val units = pc.enumParameter(classOf[BrightnessUnit], "usbSourceUnits")
        UniformSource(norm, units)
    }

    // Get Normalization info
    val normBand = pc.enumParameter(classOf[WavebandDefinition])

    // Get Spectrum Resource
    import edu.gemini.itc.service.SourceDefinition.Distribution._
    val sourceSpec = pc.enumParameter(classOf[Distribution])
    val sourceDefinition = sourceSpec match {
      case BBODY =>             BlackBody(pc.doubleParameter("BBTemp"))
      case PLAW =>              PowerLaw(pc.doubleParameter("powerIndex"))
      case USER_DEFINED =>      UserDefined(pc.userSpectrumName().get, pc.userSpectrum().get)
      case LIBRARY_STAR =>      LibraryStar(pc.parameter("stSpectrumType"))
      case LIBRARY_NON_STAR =>  LibraryNonStar(pc.parameter("nsSpectrumType"))
      case ELINE =>
        EmissionLine(
          pc.doubleParameter("lineWavelength"),
          pc.doubleParameter("lineWidth"),
          pc.doubleParameter("lineFlux"),
          pc.parameter("lineFluxUnits"),
          pc.doubleParameter("lineContinuum"),
          pc.parameter("lineContinuumUnits"))
    }

    //Get Redshift
    import edu.gemini.itc.service.SourceDefinition.Recession._
    val recession = pc.enumParameter(classOf[Recession])
    val redshift = recession match {
      case REDSHIFT => pc.doubleParameter("z")
      case VELOCITY => pc.doubleParameter("v") / ITCConstants.C
    }

    // WOW, finally we've got everything in place..
    new SourceDefinition(spatialProfile, sourceDefinition, normBand, redshift)
  }




}
