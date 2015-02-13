package edu.gemini.itc.web

import javax.servlet.http.HttpServletRequest

import edu.gemini.itc.altair.AltairParameters
import edu.gemini.itc.parameters.SourceDefinitionParameters._
import edu.gemini.itc.parameters.{ObservingConditionParameters, PlottingDetailsParameters, SourceDefinitionParameters, TeleParameters}
import edu.gemini.itc.shared._
import edu.gemini.spModel.gemini.altair.AltairParams
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

  def teleParameters(r: ITCMultiPartParser): TeleParameters = {
    val pc      = ITCRequest.from(r)
    val coating = pc.enumParameter(classOf[TeleParameters.Coating])
    val port    = pc.enumParameter(classOf[IssPort])
    val wfs     = pc.enumParameter(classOf[TeleParameters.Wfs])
    new TeleParameters(coating, port, wfs)
  }

  def obsConditionParameters(r: ITCMultiPartParser): ObservingConditionParameters = {
    val pc      = ITCRequest.from(r)
    val iq      = pc.enumParameter(classOf[SPSiteQuality.ImageQuality])
    val cc      = pc.enumParameter(classOf[SPSiteQuality.CloudCover])
    val wv      = pc.enumParameter(classOf[SPSiteQuality.WaterVapor])
    val sb      = pc.enumParameter(classOf[SPSiteQuality.SkyBackground])
    val airmass = pc.doubleParameter("Airmass")
    new ObservingConditionParameters(iq, cc, wv, sb, airmass)
  }

  def plotParamters(r: ITCMultiPartParser): PlottingDetailsParameters = {
    val pc      = ITCRequest.from(r)
    val limits  = pc.enumParameter(classOf[PlottingDetailsParameters.PlotLimits])
    val lower   = pc.doubleParameter("plotWavelengthL")
    val upper   = pc.doubleParameter("plotWavelengthU")
    new PlottingDetailsParameters(limits, lower, upper)
  }

  def altairParameters(r: ITCMultiPartParser): AltairParameters = {
    val pc      = ITCRequest.from(r)
    val guideStarSeperation  = pc.doubleParameter("guideSep")
    val guideStarMagnitude   = pc.doubleParameter("guideMag")
    val fieldLens            = pc.enumParameter(classOf[AltairParams.FieldLens])
    val wfsMode              = pc.enumParameter(classOf[AltairParams.GuideStarType])
    val wfs                  = pc.enumParameter(classOf[TeleParameters.Wfs])
    val altairUsed           = wfs eq TeleParameters.Wfs.AOWFS
    new AltairParameters(guideStarSeperation, guideStarMagnitude, fieldLens, wfsMode, altairUsed)
  }

  def sourceDefinitionParameters(r: ITCMultiPartParser): SourceDefinitionParameters = {
    val itcR = ITCRequest.from(r)

    // Get the source geometry and type
    import Profile._
    val spatialProfile = itcR.enumParameter(classOf[Profile]) match {
      case POINT    =>
        val norm  = itcR.doubleParameter("psSourceNorm")
        val units = itcR.enumParameter(classOf[BrightnessUnit], "psSourceUnits")
        PointSource(norm, units)
      case GAUSSIAN =>
        val norm  = itcR.doubleParameter("gaussSourceNorm")
        val units = itcR.enumParameter(classOf[BrightnessUnit], "gaussSourceUnits")
        val fwhm  = itcR.doubleParameter("gaussFwhm")
        GaussianSource(norm, units, fwhm)
      case UNIFORM  =>
        val norm  = itcR.doubleParameter("usbSourceNorm")
        val units = itcR.enumParameter(classOf[BrightnessUnit], "usbSourceUnits")
        UniformSource(norm, units)
    }

    // Get Normalization info
    val normBand = itcR.enumParameter(classOf[WavebandDefinition])

    // Get Spectrum Resource
    import Distribution._
    val sourceSpec = itcR.enumParameter(classOf[Distribution])
    val sourceDefinition = sourceSpec match {
      case LIBRARY_STAR =>
        val st = itcR.parameter("stSpectrumType")
        LibraryStar(st, STELLAR_LIB + "/" + st.toLowerCase + SED_FILE_EXTENSION)
      case LIBRARY_NON_STAR =>
        val st = itcR.parameter("nsSpectrumType")
        LibraryNonStar(st, NON_STELLAR_LIB + "/" + st + SED_FILE_EXTENSION)
      case ELINE =>
        EmissionLine(
          itcR.doubleParameter("lineWavelength"),
          itcR.doubleParameter("lineWidth"),
          itcR.doubleParameter("lineFlux"),
          itcR.parameter("lineFluxUnits"),
          itcR.doubleParameter("lineContinuum"),
          itcR.parameter("lineContinuumUnits"))
      case BBODY =>
        BlackBody(itcR.doubleParameter("BBTemp"))
      case PLAW =>
        PowerLaw(itcR.doubleParameter("powerIndex"))
      case USER_DEFINED =>
        UserDefined(itcR.userSpectrumName().get, itcR.userSpectrum().get)
    }

    //Get Redshift
    import Recession._
    val recession = itcR.enumParameter(classOf[Recession])
    val redshift = recession match {
      case REDSHIFT => itcR.doubleParameter("z")
      case VELOCITY => itcR.doubleParameter("v") / ITCConstants.C
    }

    // WOW, finally we've got everything in place..
    new SourceDefinitionParameters(spatialProfile, sourceDefinition, normBand, redshift)
  }




}
