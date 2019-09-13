package edu.gemini.itc.baseline.util

import edu.gemini.itc.shared.TelescopeDetails.Coating
import edu.gemini.itc.shared._
import edu.gemini.spModel.core.{LibraryNonStar, LibraryStar, EmissionLine, PowerLaw, BlackBody, GaussianSource, UniformSource, PointSource, Redshift, SurfaceBrightness, MagnitudeSystem, MagnitudeBand}
import edu.gemini.spModel.core.WavelengthConversions._
import edu.gemini.spModel.gemini.altair.AltairParams.{FieldLens, GuideStarType}
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target._
import edu.gemini.spModel.telescope.IssPort
import squants.motion.VelocityConversions._
import squants.radio.IrradianceConversions._
import squants.radio.SpectralIrradianceConversions._

/**
 * Definition of test fixtures which hold all input parameters needed to execute different ITC recipes.
 */
case class Fixture[T <: InstrumentDetails](
                    ins: T,
                    src: SourceDefinition,
                    odp: ObservationDetails,
                    ocp: ObservingConditions,
                    tep: TelescopeDetails,
                    pdp: PlottingDetails
                       ) {
  val hash: Int = Hash.calc(ins) + Hash.calc(src) + Hash.calc(ocp) + Hash.calc(odp) + Hash.calc(tep) + Hash.calc(pdp)
}

object Fixture {

  // ==== Create fixtures by putting together matching sources, modes and configurations and mixing in conditions and telescope configurations

  def rBandImgFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions): List[Fixture[T]] = fixtures(RBandSources, ImagingModes,      configs, conds)

  def kBandSpcFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions): List[Fixture[T]] = fixtures(KBandSources, SpectroscopyModes, configs, conds)

  def kBandIfuFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions): List[Fixture[T]] = fixtures(KBandSources, IfuModes,          configs, conds)

  def kBandIfuGmosFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions): List[Fixture[T]] = fixtures(KBandSources, IfuSingleModes, configs, conds)

  def kBandImgFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions): List[Fixture[T]] = fixtures(KBandSources, ImagingModes,      configs, conds)

  def nBandSpcFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions): List[Fixture[T]] = fixtures(NBandSources, SpectroscopyModes, configs, conds)

  def nBandImgFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions): List[Fixture[T]] = fixtures(NBandSources, ImagingModes,      configs, conds)

  def qBandSpcFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions): List[Fixture[T]] = fixtures(QBandSources, SpectroscopyModes, configs, conds)

  // create fixtures from combinations of given input values
  private def fixtures[T <: InstrumentDetails](sources: List[SourceDefinition], modes: List[ObservationDetails], configs: List[T], conds: List[ObservingConditions]) = for {
      src   <- sources
      odp   <- modes
      ins   <- configs
      cond  <- conds
      tele  <- TelescopeConfigurations
    } yield Fixture(ins, src, odp, cond, tele, DummyPlottingParameters)


  // ==== IMAGING ANALYSIS MODES
  lazy val ImagingModes = List(
    ObservationDetails(
      ImagingS2N(10, None, 200.0, 0.5, 5.0),
      AutoAperture(5.0)
    ),
    ObservationDetails(
      ImagingS2N(10, Some(2), 200.0, 0.5, 5.0),
      UserAperture(2.0, 5.0)
    ),
    ObservationDetails(
      ImagingInt(5, 300.0, None, 1.0, 5.0),
      AutoAperture(5.5)
    ),
    ObservationDetails(
      ImagingInt(5, 300.0, Some(5), 1.0, 5.0),
      UserAperture(2.0, 5.0)
    )
  )

  // ==== SPECTROSCOPY ANALYSIS MODES
  lazy val SpectroscopyModes = List(
    ObservationDetails(
      SpectroscopyS2N(6, Some(4), 300.0, 0.5, 10.0),
      AutoAperture(4.5)
    ),
    ObservationDetails(
      SpectroscopyS2N(6, None, 300.0, 1.0, 10.0),
      UserAperture(2.5, 6.0)
    )
  )

  // ==== IFU ANALYSIS MODES
  lazy val IfuModes: List[ObservationDetails] = IfuSingleModes ++ IfuSummedModes ++ IfuRadialModes

  lazy val IfuSingleModes = List(
    ObservationDetails(
      SpectroscopyS2N(10, None, 150.0, 0.5, 10.0),
      IfuSingle(1, 0.5)
    )
  )
  lazy val IfuSummedModes = List(
    ObservationDetails(
      SpectroscopyS2N(6, Some(10), 300.0, 1.0, 8.0),
      IfuSummed(1, 2, 5, 0.0, 0.0)
    )
  )
  lazy val IfuRadialModes = List(
    ObservationDetails(
      SpectroscopyS2N(3, None, 400.0, 1.0, 7.0),
      IfuRadial(1, 0.0, 0.0)
    ),
    ObservationDetails(
      SpectroscopyS2N(3, Some(4), 400.0, 1.0, 7.0),
      IfuRadial(1, 0.5, 1.0)
    ),
    ObservationDetails(
      SpectroscopyS2N(6, None, 300.0, 1.0, 6.0),
      IfuRadial(5, 0.0, 1.0)
    ),
    ObservationDetails(
      SpectroscopyS2N(6, Some(2), 300.0, 1.0, 6.0),
      IfuRadial(5, 1.0, 1.0)
    )
  )

  // ==== SOURCES

  // ------- U - BAND
  lazy val UBandSources = List(
    SourceDefinition(
      GaussianSource(1.0),
      BlackBody(10000),
      1.0e-3, MagnitudeSystem.Vega, MagnitudeBand.U,
      Redshift(0.0)
    ),
    SourceDefinition(
      GaussianSource(1.0),
      PowerLaw(-1.0),
      1.0e-3, MagnitudeSystem.Vega, MagnitudeBand.U,
      Redshift(0.5)
    )
  )

  // ------- R - BAND
  lazy val RBandSources = List(
    SourceDefinition(
      PointSource,
      LibraryStar.A0V,
      20.0, MagnitudeSystem.Vega, MagnitudeBand.R,
      Redshift(0.0)

    ),
    SourceDefinition(
      GaussianSource(1.0),
      BlackBody(8000),
      1.0e-3, MagnitudeSystem.Jy, MagnitudeBand.R,
      Redshift(0.75)

    )
  )

  // ------- K - BAND
  lazy val KBandSources = List(
    SourceDefinition(
      PointSource,
      LibraryStar.A0V,
      12.0, MagnitudeSystem.Vega, MagnitudeBand.K,
      Redshift(0.0)

    ),
    SourceDefinition(
      UniformSource,
      EmissionLine(2.2.microns, 250.0.kps, 5.0e-19.wattsPerSquareMeter, 1.0e-16.wattsPerSquareMeterPerMicron),
      22.0, SurfaceBrightness.Vega, MagnitudeBand.K,
      Redshift(0.75)

    )
  )

  // ------- N - BAND
  lazy val NBandSources = List(
    SourceDefinition(
      PointSource,
      LibraryNonStar.NGC1068,
      9.0, MagnitudeSystem.AB, MagnitudeBand.N,
      Redshift(0.0)

    ),
    SourceDefinition(
      UniformSource,
      EmissionLine(12.8.microns, 500.kps, 5.0e-19.wattsPerSquareMeter, 1.0e-16.wattsPerSquareMeterPerMicron),
      12.0, SurfaceBrightness.Vega, MagnitudeBand.N,
      Redshift(1.5)

    )
  )

  // ------- Q - BAND
  lazy val QBandSources = List(
    SourceDefinition(
      GaussianSource(1.0),
      BlackBody(10000),
      1.0e-3, MagnitudeSystem.Vega, MagnitudeBand.Q,
      Redshift(0.0)

    ),
    SourceDefinition(
      UniformSource,
      PowerLaw(-1.0),
      11.0, SurfaceBrightness.Vega, MagnitudeBand.Q,
      Redshift(2.0)

    )
  )


  // ================
  // Defines a set of relevant weather conditions (not all combinations make sense)
  import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._
  private val weatherConditions = List[Tuple3[ImageQuality,CloudCover,WaterVapor]](
    (ImageQuality.PERCENT_20, CloudCover.PERCENT_50, WaterVapor.PERCENT_50),  // IQ20,CC50,WV50
    (ImageQuality.PERCENT_70, CloudCover.PERCENT_50, WaterVapor.ANY),         // IQ70,CC50,Any
    (ImageQuality.ANY,        CloudCover.ANY,        WaterVapor.ANY)          // Any ,-   ,-
  )
  // ================

  // Defines a set of relevant observing conditions; total 9*4*3=108 conditions
  import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.SkyBackground._
  lazy val ObservingConditions: List[ObservingConditions] =
    for {
      (iq, cc, wv)  <- weatherConditions
      sb            <- List(PERCENT_50, PERCENT_80) // SB20=1, SB50=2, SB80=3, ANY=4
      am            <- List(1.5)                    // airmass 1.0, 1.5, 2.0 (relevant levels: < 1.26; 1.26..1.75, > 1.75)
    } yield new ObservingConditions(iq, cc, wv, sb, am)

  // Defines a set of relevant telescope configurations; total 1*2*2=4 configurations
  // NOTE: looking at TeleParameters.getWFS() it seems that AOWFS is always replaced with OIWFS
  lazy val TelescopeConfigurations: List[TelescopeDetails] =
    for {
      coating       <- List(Coating.SILVER)       // don't test aluminium coating
      port          <- IssPort.values()
      wfs           <- List(GuideProbe.Type.OIWFS, GuideProbe.Type.PWFS)  // don't use AOWFS (?)
    } yield new TelescopeDetails(coating, port, wfs)

  lazy val NoAltair: None.type = None                                                                     // use this for spectroscopy
  lazy val AltairNgsFL = Some(AltairParameters(4.0, 9.0, FieldLens.IN, GuideStarType.NGS)) // altair with NGS and field lens
  lazy val AltairNgs   = Some(AltairParameters(5.0, 10.0, FieldLens.OUT, GuideStarType.NGS)) // altair with NGS w/o field lens
  lazy val AltairLgs   = Some(AltairParameters(6.0, 10.0, FieldLens.IN, GuideStarType.LGS)) // altair with LGS (must have field lens in)

  // ==== PLOTTING PARAMETERS
  // NOTE: These values only impact the resulting graphs which are not part of the baseline.
  // NOTE: Therefore it would be wasteful to define more than one plotting parameter object here!
  lazy val DummyPlottingParameters =
    new PlottingDetails(PlottingDetails.PlotLimits.AUTO, .3, .6)

}