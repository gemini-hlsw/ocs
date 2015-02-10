package edu.gemini.itc.baseline.util

import edu.gemini.itc.altair.AltairParameters
import edu.gemini.itc.parameters.TeleParameters.{Coating, Wfs}
import edu.gemini.itc.parameters._
import edu.gemini.spModel.gemini.altair.AltairParams.{GuideStarType, FieldLens}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
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
  import SPSiteQuality._
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
      SourceDefinitionParameters.POINT_SOURCE,
      SourceDefinitionParameters.UNIFORM,
      15.0,
      SourceDefinitionParameters.MAG,
      .35,
      SourceDefinitionParameters.FILTER,
      "H",
      0.0,
      0.3,
      SourceDefinitionParameters.STELLAR_LIB + "/k0iii.nm",
      0.0,                                // black body temp        (N/A)
      0.0,                                // eline wavelength       (N/A)
      0.0,                                // eline width            (N/A)
      0.0,                                // eline flux             (N/A)
      0.0,                                // eline cont flux        (N/A)
      "",                                 // eline flux units       (N/A)
      "",                                 // eline cont flux units  (N/A)
      0,                                  // plaw index             (N/A)
      SourceDefinitionParameters.LIBRARY_STAR),

    // point source defined by W/m2/um
    new SourceDefinitionParameters(
      SourceDefinitionParameters.POINT_SOURCE,
      SourceDefinitionParameters.UNIFORM,
      2E-17,
      SourceDefinitionParameters.WATTS,
      .35,
      SourceDefinitionParameters.FILTER,
      "K",
      0.0,
      1.0,                                // redshift
      SourceDefinitionParameters.NON_STELLAR_LIB + "/elliptical-galaxy.nm",
      0.0,                                // black body temp        (N/A)
      0.0,                                // eline wavelength       (N/A)
      0.0,                                // eline width            (N/A)
      0.0,                                // eline flux             (N/A)
      0.0,                                // eline cont flux        (N/A)
      "",                                 // eline flux units       (N/A)
      "",                                 // eline cont flux units  (N/A)
      0,                                  // plaw index             (N/A)
      SourceDefinitionParameters.LIBRARY_NON_STAR),

    // black body spectral distribution
    new SourceDefinitionParameters(
      SourceDefinitionParameters.EXTENDED_SOURCE,
      SourceDefinitionParameters.UNIFORM,
      19.0,
      SourceDefinitionParameters.WATTS,
      .35,
      SourceDefinitionParameters.FILTER,
      "L",                                // band
      0.0,                                // norm wavelength
      0.5,                                // redshift
      SourceDefinitionParameters.BBODY,   // spectrum resource
      8000.0,                             // black body temp
      0.0,                                // eline wavelength       (N/A)
      0.0,                                // eline width            (N/A)
      0.0,                                // eline flux             (N/A)
      0.0,                                // eline cont flux        (N/A)
      "",                                 // eline flux units       (N/A)
      "",                                 // eline cont flux units  (N/A)
      0,                                  // plaw index             (N/A)
      SourceDefinitionParameters.BBODY)

  )

  lazy val GmosSources = PointSources ++ List(
    // emission line spectral distribution
    new SourceDefinitionParameters(
      SourceDefinitionParameters.EXTENDED_SOURCE,
      SourceDefinitionParameters.GAUSSIAN,
      20.0,
      SourceDefinitionParameters.WATTS,
      .35,
      SourceDefinitionParameters.FILTER,
      "R",                                    // band
      0.0,                                    // norm wavelength
      1.0,                                    // redshift
      SourceDefinitionParameters.ELINE,       // spectrum resource
      0.0,                                    // black body temp [K]  (N/A)
      0.656,                                  // eline wavelength
      500.0,                                  // eline width
      5e-17,                                  // eline flux
      1e-17,                                  // eline continuum flux
      SourceDefinitionParameters.ERGS_FLUX,   // eline flux units
      SourceDefinitionParameters.ERGS_FLUX,   // eline continuum flux units
      0,                                      // plaw index           (N/A)
      SourceDefinitionParameters.ELINE),      // source spec

    // TODO get power law to work with other instruments(?)
    //power law spectral distribution
    new SourceDefinitionParameters(
      SourceDefinitionParameters.EXTENDED_SOURCE,
      SourceDefinitionParameters.GAUSSIAN,
      20.0,
      SourceDefinitionParameters.MAG,
      .35,
      SourceDefinitionParameters.FILTER,
      "R",
      0.0,
      1.5,
      SourceDefinitionParameters.PLAW,
      0.0,                                // black body temp        (N/A)
      0.0,                                // eline wavelength       (N/A)
      0.0,                                // eline width            (N/A)
      0.0,                                // eline flux             (N/A)
      0.0,                                // eline cont flux        (N/A)
      "",                                 // eline flux units       (N/A)
      "",                                 // eline cont flux units  (N/A)
      -1,                                 // plaw index
      SourceDefinitionParameters.PLAW)

  )

  lazy val NiciSources = PointSources ++ List(
    // emission line spectral distribution
    new SourceDefinitionParameters(
      SourceDefinitionParameters.EXTENDED_SOURCE,
      SourceDefinitionParameters.GAUSSIAN,
      20.0,
      SourceDefinitionParameters.WATTS,
      .35,
      SourceDefinitionParameters.FILTER,
      "R",                                    // band
      0.0,                                    // norm wavelength
      1.0,                                    // redshift
      SourceDefinitionParameters.ELINE,       // spectrum resource
      0.0,                                    // black body temp [K]  (N/A)
      0.656,                                  // eline wavelength
      500.0,                                  // eline width
      5e-19,                                  // eline flux
      1e-16,                                  // eline continuum flux
      SourceDefinitionParameters.WATTS_FLUX,  // eline flux units
      SourceDefinitionParameters.WATTS_FLUX,  // eline continuum flux units
      0,                                      // plaw index           (N/A)
      SourceDefinitionParameters.ELINE)       // source spec
  )

  lazy val NearIRSources = PointSources ++ List(
    // emission line spectral distribution
    new SourceDefinitionParameters(
      SourceDefinitionParameters.EXTENDED_SOURCE,
      SourceDefinitionParameters.GAUSSIAN,
      20.0,
      SourceDefinitionParameters.WATTS,
      .35,
      SourceDefinitionParameters.FILTER,
      "J",                                    // band
      0.0,                                    // norm wavelength
      0.7,                                    // redshift
      SourceDefinitionParameters.ELINE,       // spectrum resource
      0.0,                                    // black body temp [K]  (N/A)
      2.2,                                    // eline wavelength
      100.0,                                  // eline width
      5e-19,                                  // eline flux
      1e-16,                                  // eline continuum flux
      SourceDefinitionParameters.WATTS_FLUX,  // eline flux units
      SourceDefinitionParameters.WATTS_FLUX,  // eline continuum flux units
      0,                                      // plaw index           (N/A)
      SourceDefinitionParameters.ELINE)       // source spec
  )

  lazy val MidIRSources = PointSources ++ List(
    // emission line spectral distribution
    new SourceDefinitionParameters(
      SourceDefinitionParameters.EXTENDED_SOURCE,
      SourceDefinitionParameters.GAUSSIAN,
      20.0,
      SourceDefinitionParameters.WATTS,
      .35,
      SourceDefinitionParameters.FILTER,
      "J",                                    // band
      0.0,                                    // norm wavelength
      1.0,                                    // redshift
      SourceDefinitionParameters.ELINE,       // spectrum resource
      0.0,                                    // black body temp [K]  (N/A)
      12.8,                                   // eline wavelength
      500.0,                                  // eline width
      5e-19,                                  // eline flux
      1e-16,                                  // eline continuum flux
      SourceDefinitionParameters.WATTS_FLUX,  // eline flux units
      SourceDefinitionParameters.WATTS_FLUX,  // eline continuum flux units
      0,                                      // plaw index           (N/A)
      SourceDefinitionParameters.ELINE)       // source spec
  )

  // Defines a set of relevant observing conditions; total 9*4*3=108 conditions
  import SPSiteQuality.SkyBackground._
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
