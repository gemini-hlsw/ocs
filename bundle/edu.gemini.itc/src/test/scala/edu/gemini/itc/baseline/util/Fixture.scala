package edu.gemini.itc.baseline.util

import edu.gemini.itc.gsaoi.GsaoiParameters
import edu.gemini.itc.nifs.NifsParameters
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
                    pdp: PlottingDetails
                       ) {
  val hash = Hash.calc(ins) + Hash.calc(src) + Hash.calc(ocp) + Hash.calc(odp) + Hash.calc(tep) + Fixture.altairHash(ins) + Fixture.gemsHash(ins) + Hash.calc(pdp)
}

object Fixture {

  // ===  TODO: this is temporary only to mimic old behavior
  def altairHash(ins: InstrumentDetails): Int = ins match {
    case i: NiriParameters => altairHash(i.altair)
    case i: NifsParameters => altairHash(i.getAltair)
    case _                 => 0
  }
  def altairHash(altair: Option[AltairParameters]): Int = altair match {
    case None    => Hash.calc(new AltairParameters(0.0,  0.0, FieldLens.OUT,  GuideStarType.NGS))
    case Some(a) => Hash.calc(a)
  }
  def gemsHash(ins: InstrumentDetails): Int = ins match {
    case i: GsaoiParameters => Hash.calc(i.getGems)
    case _                 => 0
  }
  // ===

  // ==== Create fixtures by putting together matching sources, modes and configurations and mixing in conditions and telescope configurations

  def rBandImgFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions) = fixtures(RBandSources, ImagingModes,      configs, conds)

  def kBandSpcFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions) = fixtures(KBandSources, SpectroscopyModes, configs, conds)

  def kBandImgFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions) = fixtures(KBandSources, ImagingModes,      configs, conds)

  def nBandSpcFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions) = fixtures(NBandSources, SpectroscopyModes, configs, conds)

  def nBandImgFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions) = fixtures(NBandSources, ImagingModes,      configs, conds)

  def qBandSpcFixtures[T <: InstrumentDetails](configs: List[T], conds: List[ObservingConditions] = ObservingConditions) = fixtures(QBandSources, SpectroscopyModes, configs, conds)

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

  lazy val NoAltair    = None                                                                     // use this for spectroscopy
  lazy val AltairNgsFL = Some(new AltairParameters(4.0,  9.0, FieldLens.IN,   GuideStarType.NGS)) // altair with NGS and field lens
  lazy val AltairNgs   = Some(new AltairParameters(5.0, 10.0, FieldLens.OUT,  GuideStarType.NGS)) // altair with NGS w/o field lens
  lazy val AltairLgs   = Some(new AltairParameters(6.0, 10.0, FieldLens.IN,   GuideStarType.LGS)) // altair with LGS (must have field lens in)

  // ==== PLOTTING PARAMETERS
  // NOTE: These values only impact the resulting graphs which are not part of the baseline.
  // NOTE: Therefore it would be wasteful to define more than one plotting parameter object here!
  lazy val DummyPlottingParameters =
    new PlottingDetails(PlottingDetails.PlotLimits.AUTO, .3, .6)

}