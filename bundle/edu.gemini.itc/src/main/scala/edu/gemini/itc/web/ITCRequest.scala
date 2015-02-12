package edu.gemini.itc.web

import javax.servlet.http.HttpServletRequest

import edu.gemini.itc.altair.AltairParameters
import edu.gemini.itc.parameters.SourceDefinitionParameters._
import edu.gemini.itc.parameters.{SourceDefinitionParameters, ObservingConditionParameters, PlottingDetailsParameters, TeleParameters}
import edu.gemini.itc.shared.{ITCConstants, WavebandDefinition, ITCMultiPartParser}
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
    val sourceGeom: SourceDefinitionParameters.SourceGeometry = itcR.enumParameter(classOf[SourceDefinitionParameters.SourceGeometry])
    val extSourceType: SourceDefinitionParameters.ExtSourceType = itcR.enumParameter(classOf[SourceDefinitionParameters.ExtSourceType])
    // TODO: introduce specific types for different source geometries
    val (sourceType, fwhm, sourceNorm, units) = sourceGeom match {
      case SourceGeometry.POINT =>
        (SourceType.POINT, 0.0, itcR.doubleParameter("psSourceNorm"), itcR.enumParameter(classOf[SourceDefinitionParameters.BrightnessUnit], "psSourceUnits"))
      case SourceGeometry.EXTENDED =>
        extSourceType match {
          case ExtSourceType.GAUSSIAN => (SourceType.EXTENDED_GAUSSIAN, itcR.doubleParameter("gaussFwhm"), itcR.doubleParameter("gaussSourceNorm"), itcR.enumParameter(classOf[SourceDefinitionParameters.BrightnessUnit], "gaussSourceUnits"))
          case ExtSourceType.UNIFORM =>  (SourceType.EXTENDED_UNIFORM, 0.0, itcR.doubleParameter("usbSourceNorm"), itcR.enumParameter(classOf[SourceDefinitionParameters.BrightnessUnit], "usbSourceUnits"))
        }
    }

    // Get Normalization info
    val normBand = itcR.enumParameter(classOf[WavebandDefinition])

    // Get Spectrum Resource
    val sourceSpec = itcR.enumParameter(classOf[SourceDefinitionParameters.SpectralDistribution])
    // TODO: introduce specific types for different spectra resources
    val (specType, sedSpectrum, eLineWavelength, eLineWidth, eLineFlux, eLineContinuumFlux, eLineFluxUnits, eLineContinuumFluxUnits, bbTemp, plawIndex, userDefined) = sourceSpec match {
      case SpectralDistribution.LIBRARY_STAR =>
        val st = itcR.parameter("stSpectrumType")
        (st,
         STELLAR_LIB + "/" + st.toLowerCase + SED_FILE_EXTENSION,
         0.0, // N/A
         0.0, // N/A
         0.0, // N/A
         0.0, // N/A
         "",  // N/A
         "",  // N/A
         0.0, // N/A
         0.0, // N/A
         "")  // N/A

      case SpectralDistribution.LIBRARY_NON_STAR =>
        val st = itcR.parameter("nsSpectrumType")
        (st,
        NON_STELLAR_LIB + "/" + st + SED_FILE_EXTENSION,
          0.0, // N/A
          0.0, // N/A
          0.0, // N/A
          0.0, // N/A
          "",  // N/A
          "",  // N/A
          0.0, // N/A
          0.0, // N/A
          "")  // N/A
      case SpectralDistribution.ELINE =>
        (
          "",  // N/A
          "",  // N/A
          itcR.doubleParameter("lineWavelength"),
          itcR.doubleParameter("lineWidth"),
          itcR.doubleParameter("lineFlux"),
          itcR.doubleParameter("lineContinuum"),
          itcR.parameter("lineFluxUnits"),
          itcR.parameter("lineContinuumUnits"),
          0.0, // N/A
          0.0, // N/A
          "")  // N/A
      case SpectralDistribution.BBODY =>
        (
          "",  // N/A
          "",  // N/A
          0.0, // N/A
          0.0, // N/A
          0.0, // N/A
          0.0, // N/A
          "",  // N/A
          "",  // N/A
          itcR.doubleParameter("BBTemp"), // BBTEMP
          0.0, // N/A
          "")  // N/A
      case SpectralDistribution.PLAW =>
        (
          "",  // N/A
          "",  // N/A
          0.0, // N/A
          0.0, // N/A
          0.0, // N/A
          0.0, // N/A
          "",  // N/A
          "",  // N/A
          0.0, // N/A
          itcR.doubleParameter("powerIndex"),
          "")  // N/A
      case SpectralDistribution.USER_DEFINED =>
        (
          "",  // N/A
          itcR.userSpectrumName().get, // TODO: needed??
          0.0, // N/A
          0.0, // N/A
          0.0, // N/A
          0.0, // N/A
          "",  // N/A
          "",  // N/A
          0.0, // N/A
          0.0, // N/A
          itcR.userSpectrum().get)  // N/A
    }

    //Get Redshift
    val recession: SourceDefinitionParameters.Recession = itcR.enumParameter(classOf[SourceDefinitionParameters.Recession])
    val redshift = recession match {
      case Recession.REDSHIFT => itcR.doubleParameter("z")
      case Recession.VELOCITY => itcR.doubleParameter("v") / ITCConstants.C
    }

    // WOW, finally we've got everything in place..
    new SourceDefinitionParameters(
      sourceType, sourceNorm, units, fwhm, normBand, redshift,
      sedSpectrum,
      bbTemp,
      eLineWavelength, eLineWidth, eLineFlux, eLineContinuumFlux, eLineFluxUnits, eLineContinuumFluxUnits,
      plawIndex,
      sourceSpec,
      userDefined, specType)
  }




}
