package edu.gemini.ags.gems

import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.pot.ModelConverters._
import edu.gemini.catalog.api._
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gems.CanopusWfs
import edu.gemini.spModel.gemini.gsaoi.{Gsaoi, GsaoiOdgw}
import edu.gemini.spModel.gems.GemsGuideStarType
import edu.gemini.spModel.guide.{GuideProbe, GuideSpeed}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{Conditions, CloudCover, ImageQuality, SkyBackground, WaterVapor}
import edu.gemini.skycalc

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/**
 * A magnitude table defined in the same way as we have done since before 2015A
 * (that is, with predefined magnitude limits).
 */
object GemsMagnitudeTable extends MagnitudeTable {

  import ImageQuality.{  PERCENT_20 => IQ20, PERCENT_70 => IQ70, PERCENT_85 => IQ85, ANY => IQAny }
  import SkyBackground.{ PERCENT_20 => SB20, PERCENT_50 => SB50, PERCENT_80 => SB80, ANY => SBAny }

  private val cc50 = Map(
    ((IQ20,  SB20 ), (18.5, 10.5)),
    ((IQ20,  SB50 ), (18.5, 10.5)),
    ((IQ20,  SB80 ), (18.4, 10.2)),
    ((IQ20,  SBAny), (18.3, 10.0)),
    ((IQ70,  SB20 ), (18.2, 10.0)),
    ((IQ70,  SB50 ), (18.2, 10.0)),
    ((IQ70,  SB80 ), (18.1,  9.7)),
    ((IQ70,  SBAny), (18.0,  9.5)),
    ((IQ85,  SB20 ), (17.9,  9.5)),
    ((IQ85,  SB50 ), (17.9,  9.5)),
    ((IQ85,  SB80 ), (17.8,  9.2)),
    ((IQ85,  SBAny), (17.7,  9.0)),
    ((IQAny, SB20 ), (17.6,  9.0)),
    ((IQAny, SB50 ), (17.3,  9.0)),
    ((IQAny, SB80 ), (17.0,  9.0)),
    ((IQAny, SBAny), (16.7,  8.7))
  )

  def cwfsConstraintsForCc50(iq: ImageQuality, sb: SkyBackground): MagnitudeConstraints = {
    val (f, b) = cc50((iq, sb))
    MagnitudeConstraints(RBandsList, FaintnessConstraint(f), Some(SaturationConstraint(b)))
  }

  val CwfsConstraints: Conditions => MagnitudeConstraints = { c =>
    c.cc.adjust(cwfsConstraintsForCc50(c.iq, c.sb))
  }

  val OtherAdjust: MagnitudeConstraints => Conditions => MagnitudeConstraints =
    mc => conds => conds.adjust(mc)

  private def magLimits(bands: BandsList, fl: Double, sl: Double): MagnitudeConstraints =
    MagnitudeConstraints(bands, FaintnessConstraint(fl), SaturationConstraint(sl).some)

  def apply(ctx: ObsContext, probe: GuideProbe): Option[MagnitudeCalc] = {

    def magCalc(f: Conditions => MagnitudeConstraints): MagnitudeCalc =
      new MagnitudeCalc() {
        def apply(conds: Conditions, speed: GuideSpeed): MagnitudeConstraints =
          f(conds)
      }

    val odgwCalc: MagnitudeConstraints => MagnitudeCalc = mc =>
      magCalc(OtherAdjust(mc))

    def lookup(site: Site): Option[MagnitudeCalc] =
      (site, probe) match {
        case (Site.GS, odgw: GsaoiOdgw)  =>
          Some(odgwCalc(GsaoiOdgwMagnitudeLimitsCalculator.gemsMagnitudeConstraint(GemsGuideStarType.flexure, MagnitudeBand.H.some)))

        case (Site.GS, can: CanopusWfs) =>
          Some(magCalc(CwfsConstraints))

        case _                           =>
          None
      }

    ctx.getSite.asScalaOpt.flatMap(lookup)
  }

  /**
   * GSAOI, Canopus, and F2 require special handling for magnitude limits for GeMS.
   */
  trait LimitsCalculator {
    def gemsMagnitudeConstraint(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand]): MagnitudeConstraints

    def adjustGemsMagnitudeConstraintForJava(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand], conditions: Conditions): MagnitudeConstraints
  }

  lazy val GsaoiOdgwMagnitudeLimitsCalculator = new LimitsCalculator {
    /**
     * The map formerly in Gsaoi.Filter.
     */
    private val MagnitudeLimitsMap = Map[Tuple2[GemsGuideStarType, BandsList], MagnitudeConstraints](
      (GemsGuideStarType.flexure, SingleBand(MagnitudeBand.J)) -> magLimits(SingleBand(MagnitudeBand.J), 17.2, 8.0),
      (GemsGuideStarType.flexure, SingleBand(MagnitudeBand.H)) -> magLimits(SingleBand(MagnitudeBand.H), 17.0, 8.0),
      (GemsGuideStarType.flexure, SingleBand(MagnitudeBand.K)) -> magLimits(SingleBand(MagnitudeBand.K), 18.2, 8.0),
      (GemsGuideStarType.tiptilt, SingleBand(MagnitudeBand.J)) -> magLimits(SingleBand(MagnitudeBand.J), 14.2, 7.1),
      (GemsGuideStarType.tiptilt, SingleBand(MagnitudeBand.H)) -> magLimits(SingleBand(MagnitudeBand.H), 14.5, 7.3),
      (GemsGuideStarType.tiptilt, SingleBand(MagnitudeBand.K)) -> magLimits(SingleBand(MagnitudeBand.K), 13.5, 6.5)
    )

    override def adjustGemsMagnitudeConstraintForJava(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand], conditions: Conditions): MagnitudeConstraints =
      OtherAdjust(gemsMagnitudeConstraint(starType, nirBand))(conditions)

    override def gemsMagnitudeConstraint(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand]): MagnitudeConstraints= {
      val filter = nirBand.fold(Gsaoi.Filter.H)(band => Gsaoi.Filter.getFilter(band, Gsaoi.Filter.H))
      MagnitudeLimitsMap((starType, filter.getCatalogBand.getValue))
    }
  }

  /**
   * Since Canopus is not explicitly listed in GemsInstrument, it must be visible outside of the table in order to
   * be used directly by Mascot, since it cannot be looked up through the GemsInstrumentToMagnitudeLimitsCalculator map.
   */
  trait CanopusWfsCalculator extends LimitsCalculator {
    def getNominalMagnitudeConstraints(cwfs: CanopusWfs): MagnitudeConstraints
  }

  lazy val CanopusWfsMagnitudeLimitsCalculator = new CanopusWfsCalculator {
    override def adjustGemsMagnitudeConstraintForJava(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand], conditions: Conditions): MagnitudeConstraints =
      CwfsConstraints(conditions)

    override def gemsMagnitudeConstraint(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand]): MagnitudeConstraints =
      CwfsConstraints(Conditions.NOMINAL)

    override def getNominalMagnitudeConstraints(cwfs: CanopusWfs): MagnitudeConstraints =
      CwfsConstraints(Conditions.NOMINAL)
  }

  private lazy val Flamingos2OiwfsMagnitudeLimitsCalculator = new LimitsCalculator {
    override def adjustGemsMagnitudeConstraintForJava(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand], conditions: Conditions): MagnitudeConstraints =
      OtherAdjust(gemsMagnitudeConstraint(starType, nirBand))(conditions)

    override def gemsMagnitudeConstraint(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand]) =
      magLimits(RBandsList, 18.0, 9.5)
  }
}