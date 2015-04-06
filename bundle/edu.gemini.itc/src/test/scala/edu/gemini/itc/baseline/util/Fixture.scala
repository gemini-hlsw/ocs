package edu.gemini.itc.baseline.util

import edu.gemini.itc.altair.AltairParameters
import edu.gemini.itc.gems.GemsParameters
import edu.gemini.itc.shared.TelescopeDetails.{Coating, Wfs}
import edu.gemini.itc.shared._
import edu.gemini.spModel.gemini.altair.AltairParams.{FieldLens, GuideStarType}
import edu.gemini.spModel.telescope.IssPort

/**
 * Definition of test fixtures which hold all input parameters needed to execute different ITC recipes.
 * TODO: Altair and gems configuration should probably become a part of the instrument configuration(?).
 */
case class Fixture[T <: InstrumentDetails](
                    ins: T,
                    src: SourceDefinition,
                    odp: ObservationDetails,
                    ocp: ObservingConditions,
                    tep: TelescopeDetails,
                    alt: Option[AltairParameters],
                    gem: Option[GemsParameters],
                    pdp: PlottingDetails
                       ) {
  val hash = Hash.calc(ins) + Hash.calc(src) + Hash.calc(ocp) + Hash.calc(odp) + Hash.calc(tep) + alt.fold(0)(Hash.calc) + gem.fold(0)(Hash.calc) + Hash.calc(pdp)
}

object Fixture {

  // ==== Create fixtures by putting together matching sources, modes and configurations and mixing in conditions and telescope configurations

  def rBandImgFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions, alt: List[AltairParameters] = List(), gem: List[GemsParameters] = List()) = fixtures(RBandSources, ImagingModes,      configs, conds, alt, gem)

  def kBandSpcFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions, alt: List[AltairParameters] = List(), gem: List[GemsParameters] = List()) = fixtures(KBandSources, SpectroscopyModes, configs, conds, alt, gem)

  def kBandImgFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions, alt: List[AltairParameters] = List(), gem: List[GemsParameters] = List()) = fixtures(KBandSources, ImagingModes,      configs, conds, alt, gem)

  def nBandSpcFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions, alt: List[AltairParameters] = List(), gem: List[GemsParameters] = List()) = fixtures(NBandSources, SpectroscopyModes, configs, conds, alt, gem)

  def nBandImgFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions, alt: List[AltairParameters] = List(), gem: List[GemsParameters] = List()) = fixtures(NBandSources, ImagingModes,      configs, conds, alt, gem)

  def qBandSpcFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions, alt: List[AltairParameters] = List(), gem: List[GemsParameters] = List()) = fixtures(QBandSources, SpectroscopyModes, configs, conds, alt, gem)

  // create fixtures from combinations of given input values
  private def fixtures[T <: InstrumentDetails](sources: List[SourceDefinition], modes: List[ObservationDetails], configs: List[T], conds: List[ObservingConditions], alt: List[AltairParameters], gem: List[GemsParameters]) = for {
      src   <- sources
      odp   <- modes
      ins   <- configs
      cond  <- conds
      tele  <- TelescopeConfigurations
      a     <- if (alt.isEmpty) List(None) else alt.map(Option(_))
      g     <- if (gem.isEmpty) List(None) else gem.map(Option(_))
    } yield Fixture(ins, src, odp, cond, tele, a, g, DummyPlottingParameters)


  // ==== IMAGING ANALYSIS MODES
  lazy val ImagingModes = List(
    new ObservationDetails(
      ImagingSN(10, 200.0, 0.5),
      AutoAperture(5.0)
    ),
    new ObservationDetails(
      ImagingSN(10, 200.0, 0.5),
      UserAperture(2.0, 5.0)
    ),
    new ObservationDetails(
      ImagingInt(5, 300.0, 1.0),
      AutoAperture(5.5)
    ),
    new ObservationDetails(
      ImagingInt(5, 300.0, 1.0),
      UserAperture(2.0, 5.0)
    )
  )

  // ==== SPECTROSCOPY ANALYSIS MODES
  lazy val SpectroscopyModes = List(
    new ObservationDetails(
      SpectroscopySN(6, 300.0, 0.5),
      AutoAperture(4.5)
    ),
    new ObservationDetails(
      SpectroscopySN(6, 300.0, 1.0),
      UserAperture(2.5, 6.0)
    )
  )

  // ==== SOURCES

  // ------- U - BAND
  lazy val UBandSources = List(
    new SourceDefinition(
      GaussianSource(1.0e-3, BrightnessUnit.MAG, 1.0),
      BlackBody(10000),
      WavebandDefinition.U,
      0.0
    ),
    new SourceDefinition(
      GaussianSource(1.0e-3, BrightnessUnit.MAG, 1.0),
      PowerLaw(-1.0),
      WavebandDefinition.U,
      0.5
    )
  )

  // ------- R - BAND
  lazy val RBandSources = List(
    new SourceDefinition(
      PointSource(20.0, BrightnessUnit.MAG),
      LibraryStar("A0V"),
      WavebandDefinition.R,
      0.0
    ),
    new SourceDefinition(
      GaussianSource(1.0e-3, BrightnessUnit.JY, 1.0),
      BlackBody(8000),
      WavebandDefinition.R,
      0.75
    )
  )

  // ------- K - BAND
  lazy val KBandSources = List(
    new SourceDefinition(
      PointSource(20.0, BrightnessUnit.MAG),
      LibraryStar("A0V"),
      WavebandDefinition.K,
      0.0
    ),
    new SourceDefinition(
      UniformSource(22.0, BrightnessUnit.MAG_PSA),
      EmissionLine(2.2, 250.0, 5.0e-19, "watts_flux", 1.0e-16, "watts_fd_wavelength"),
      WavebandDefinition.K,
      0.75
    )
  )

  // ------- N - BAND
  lazy val NBandSources = List(
    new SourceDefinition(
      PointSource(9.0, BrightnessUnit.ABMAG),
      LibraryNonStar("NGC1068"),
      WavebandDefinition.N,
      0.0
    ),
    new SourceDefinition(
      UniformSource(12.0, BrightnessUnit.MAG_PSA),
      EmissionLine(12.8, 500, 5.0e-19, "watts_flux", 1.0e-16, "watts_fd_wavelength"), // TODO: typed units instead of strings
      WavebandDefinition.N,
      1.5
    )
  )

  // ------- Q - BAND
  lazy val QBandSources = List(
    new SourceDefinition(
      GaussianSource(1.0e-3, BrightnessUnit.MAG, 1.0),
      BlackBody(10000),
      WavebandDefinition.Q,
      0.0
    ),
    new SourceDefinition(
      UniformSource(11.0, BrightnessUnit.MAG_PSA),
      PowerLaw(-1.0),
      WavebandDefinition.Q,
      2.0
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
  lazy val ObservingConditions =
    for {
      (iq, cc, wv)  <- weatherConditions
      sb            <- List(PERCENT_50, PERCENT_80) // SB20=1, SB50=2, SB80=3, ANY=4
      am            <- List(1.5)                    // airmass 1.0, 1.5, 2.0 (relevant levels: < 1.26; 1.26..1.75, > 1.75)
    } yield new ObservingConditions(iq, cc, wv, sb, am)

  // Defines a set of relevant telescope configurations; total 1*2*2=4 configurations
  // NOTE: looking at TeleParameters.getWFS() it seems that AOWFS is always replaced with OIWFS
  lazy val TelescopeConfigurations =
    for {
      coating       <- List(Coating.SILVER)       // don't test aluminium coating
      port          <- IssPort.values()
      wfs           <- List(Wfs.OIWFS, Wfs.PWFS)  // don't use AOWFS (?)
    } yield new TelescopeDetails(coating, port, wfs)

  lazy val AltairConfigurations = List(
    new AltairParameters(0.0,  0.0, FieldLens.OUT,  GuideStarType.NGS, false),  // no altair
    new AltairParameters(4.0,  9.0, FieldLens.IN,   GuideStarType.NGS, true),   // altair with NGS and field lens
    new AltairParameters(5.0, 10.0, FieldLens.OUT,  GuideStarType.NGS, true),   // altair with NGS w/o field lens
    new AltairParameters(6.0, 10.0, FieldLens.IN,   GuideStarType.LGS, true)    // altair with LGS (must have field lens in)
  )

  lazy val NoAltair = List(
    new AltairParameters(0.0,  0.0,  FieldLens.OUT,  GuideStarType.NGS, false)   // use this for spectroscopy
  )

  // ==== PLOTTING PARAMETERS
  // NOTE: These values only impact the resulting graphs which are not part of the baseline.
  // NOTE: Therefore it would be wasteful to define more than one plotting parameter object here!
  lazy val DummyPlottingParameters =
    new PlottingDetails(PlottingDetails.PlotLimits.AUTO, .3, .6)

}