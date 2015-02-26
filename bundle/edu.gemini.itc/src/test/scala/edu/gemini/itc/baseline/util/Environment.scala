package edu.gemini.itc.baseline.util

import edu.gemini.itc.altair.AltairParameters
import edu.gemini.itc.parameters.TeleParameters.{Coating, Wfs}
import edu.gemini.itc.parameters._
import edu.gemini.itc.shared._
import edu.gemini.spModel.gemini.altair.AltairParams.{FieldLens, GuideStarType}
import edu.gemini.spModel.telescope.IssPort

/**
 * Representation of an environment to be used in ITC recipe execution.
 * @param src the light source
 * @param ocp the conditions
 * @param tep the telescope configuration
 * @param pdp the plotting detail settings
 */
case class Environment(
                        src: SourceDefinitionParameters,
                        ocp: ObservingConditionParameters,
                        tep: TeleParameters,
                        pdp: PlottingDetailsParameters) {
  // plotting details only impact gifs which are not part of the hashed result, intentionally ignored
  val hash = Hash.calc(src) + Hash.calc(ocp) + Hash.calc(tep)
}

/**
 * Definition of some environments that can be used for testing.
 */
object Environment {

  // ================
  // Defines a set of relevant weather conditions (not all combinations make sense)
  import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._
  private val weatherConditions = List[Tuple3[ImageQuality,CloudCover,WaterVapor]](
    (ImageQuality.PERCENT_20, CloudCover.PERCENT_50, WaterVapor.PERCENT_50),  // IQ20,CC50,WV50
    (ImageQuality.PERCENT_70, CloudCover.PERCENT_50, WaterVapor.ANY),         // IQ70,CC50,Any
    (ImageQuality.ANY,        CloudCover.ANY,        WaterVapor.ANY)          // Any ,-   ,-
  )
  // ================

  // Different sources
  lazy val PointSources = List(
    // point source defined by magnitude
    new SourceDefinitionParameters(
      PointSource(15.0, SourceDefinitionParameters.BrightnessUnit.MAG),
      LibraryStar(null, SourceDefinitionParameters.STELLAR_LIB + "/k0iii.nm"),
      WavebandDefinition.H,
      0.3),

    // point source defined by W/m2/um
    new SourceDefinitionParameters(
      PointSource(2E-17, SourceDefinitionParameters.BrightnessUnit.WATTS),
      LibraryNonStar(null, SourceDefinitionParameters.NON_STELLAR_LIB + "/elliptical-galaxy.nm"),
      WavebandDefinition.K,               // normalisation band
      1.0),                               // redshift

    // black body spectral distribution
    new SourceDefinitionParameters(
      UniformSource(19.0, SourceDefinitionParameters.BrightnessUnit.WATTS),
      BlackBody(10000.0),
      WavebandDefinition.L,               // normalisation band
      0.5)                                // redshift

  )

  lazy val GmosSources = PointSources ++ List(
    // emission line spectral distribution
    new SourceDefinitionParameters(
      GaussianSource(20.0, SourceDefinitionParameters.BrightnessUnit.WATTS, .35),
      EmissionLine(0.656, 500.0, 5e-17, SourceDefinitionParameters.ERGS_FLUX, 1e-17, SourceDefinitionParameters.ERGS_FLUX),
      WavebandDefinition.R,                   // normalisation band
      1.0),                                   // redshift

    // TODO get power law to work with other instruments(?)
    //power law spectral distribution
    new SourceDefinitionParameters(
      GaussianSource(20.0, SourceDefinitionParameters.BrightnessUnit.MAG, .35),
      PowerLaw(-1),
      WavebandDefinition.R,               // normalisation band
      1.5)
  )

  lazy val NearIRSources = PointSources ++ List(
    // emission line spectral distribution
    new SourceDefinitionParameters(
      GaussianSource(20.0, SourceDefinitionParameters.BrightnessUnit.WATTS, .35),
      EmissionLine(2.2, 100.0, 5e-19, SourceDefinitionParameters.WATTS_FLUX, 1e-16, SourceDefinitionParameters.WATTS_FLUX),
      WavebandDefinition.J,                   // normalisation band
      0.7)                                    // redshift
  )

  lazy val MidIRSources = PointSources ++ List(
    // emission line spectral distribution
    new SourceDefinitionParameters(
      GaussianSource(20.0, SourceDefinitionParameters.BrightnessUnit.WATTS, .35),
      EmissionLine(12.8, 500.0, 5e-19, SourceDefinitionParameters.WATTS_FLUX, 1e-16, SourceDefinitionParameters.WATTS_FLUX),
      WavebandDefinition.J,                   // normalisation band
      1.0)                                    // redshift
  )

  // Defines a set of relevant observing conditions; total 9*4*3=108 conditions
  import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.SkyBackground._
  lazy val ObservingConditions =
    for {
      (iq, cc, wv)  <- weatherConditions
      sb            <- List(PERCENT_50, PERCENT_80) // SB20=1, SB50=2, SB80=3, ANY=4
      am            <- List(1.5)                    // airmass 1.0, 1.5, 2.0 (relevant levels: < 1.26; 1.26..1.75, > 1.75)
    } yield new ObservingConditionParameters(iq, cc, wv, sb, am)

  // Defines a set of relevant telescope configurations; total 1*2*2=4 configurations
  // NOTE: looking at TeleParameters.getWFS() it seems that AOWFS is always replaced with OIWFS
  lazy val TelescopeConfigurations =
    for {
      coating       <- List(Coating.SILVER)       // don't test aluminium coating
      port          <- IssPort.values()
      wfs           <- List(Wfs.OIWFS, Wfs.PWFS)  // don't use AOWFS (?)
    } yield new TeleParameters(coating, port, wfs)

  lazy val PlottingParameters = List(
    new PlottingDetailsParameters(PlottingDetailsParameters.PlotLimits.AUTO, .3, .6)
  )

  lazy val AltairConfigurations = List(
    new AltairParameters(0.0,  0.0, FieldLens.OUT,  GuideStarType.NGS, false),  // no altair
    new AltairParameters(4.0,  9.0, FieldLens.IN,   GuideStarType.NGS, true),   // altair with NGS and field lens
    new AltairParameters(5.0, 10.0, FieldLens.OUT,  GuideStarType.NGS, true),   // altair with NGS w/o field lens
    new AltairParameters(6.0, 10.0, FieldLens.IN,   GuideStarType.LGS, true)    // altair with LGS (must have field lens in)
  )

  lazy val NoAltair =
    new AltairParameters(0.0,  0.0,  FieldLens.OUT,  GuideStarType.NGS, false)   // use this for spectroscopy

}
