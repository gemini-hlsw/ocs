package edu.gemini.ags.gems

import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.pot.ModelConverters._
import edu.gemini.catalog.api._
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gems.{CanopusWfs, GemsInstrument}
import edu.gemini.spModel.gemini.gsaoi.{Gsaoi, GsaoiOdgw}
import edu.gemini.spModel.gems.GemsGuideStarType
import edu.gemini.spModel.guide.{GuideSpeed, GuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions

import edu.gemini.skycalc
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/**
 * A magnitude table defined in the same way as we have done since before 2015A
 * (that is, with predefined magnitude limits).
 */
object GemsMagnitudeTable extends MagnitudeTable {

  private def magLimits(bands: BandsList, fl: Double, sl: Double): MagnitudeConstraints =
    MagnitudeConstraints(bands, FaintnessConstraint(fl), SaturationConstraint(sl).some)

  def apply(ctx: ObsContext, probe: GuideProbe): Option[MagnitudeCalc] = {
    def mc(nominalLimits: MagnitudeConstraints): MagnitudeCalc = new MagnitudeCalc() {
      def apply(conds: Conditions, speed: GuideSpeed): MagnitudeConstraints =
        conds.adjust(nominalLimits)
    }

    def lookup(site: Site): Option[MagnitudeCalc] =
      ((site, probe) match {
        case (Site.GS, odgw: GsaoiOdgw)  =>
          Some(GsaoiOdgwMagnitudeLimitsCalculator.gemsMagnitudeConstraint(GemsGuideStarType.flexure, MagnitudeBand.H.some))

        case (Site.GS, can: CanopusWfs) =>
          Some(CanopusWfsMagnitudeLimitsCalculator.gemsMagnitudeConstraint(GemsGuideStarType.tiptilt, MagnitudeBand.R.some))

        case _                           =>
          None
      }).map(mc)

    ctx.getSite.asScalaOpt.flatMap(lookup)
  }

  /**
   * GSAOI, Canopus, and F2 require special handling for magnitude limits for GeMS.
   */
  trait LimitsCalculator {
    def gemsMagnitudeConstraint(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand]): MagnitudeConstraints

    def adjustGemsMagnitudeConstraintForJava(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand], conditions: Conditions): MagnitudeConstraints =
      conditions.adjust(gemsMagnitudeConstraint(starType, nirBand))

    def searchCriterionBuilder(name: String, radiusLimit: skycalc.Angle, instrument: GemsInstrument, magConstraint: MagnitudeConstraints, posAngles: java.util.Set[Angle]): CatalogSearchCriterion = {
      val radiusConstraint = RadiusConstraint.between(Angle.zero, radiusLimit.toNewModel)
      val searchOffset = instrument.getOffset.asScalaOpt.map(_.toNewModel)
      val searchPA = posAngles.asScala.headOption
      CatalogSearchCriterion(name, radiusConstraint, magConstraint, searchOffset, searchPA)
    }

  }

  /**
   * Unfortunately, we need a lookup table for the Mascot algorithm to map GemsInstruments to GemsMagnitudeLimitsCalculators.
   * We cannot include this in the GemsInstrument as this would cause dependency issues and we want to decouple these.
   */
  lazy val GemsInstrumentToMagnitudeLimitsCalculator = Map[GemsInstrument, LimitsCalculator](
    GemsInstrument.gsaoi      -> GsaoiOdgwMagnitudeLimitsCalculator,
    GemsInstrument.flamingos2 -> Flamingos2OiwfsMagnitudeLimitsCalculator
  )

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

  // Values provided by science are at:
  //      SB Any, CC 70, IQ 70.
  // Add 0.8 to faintness / brightness limits to get values for the calculator, which assumes:
  //      SB  20, CC 50, IQ 70.
  lazy val CanopusWfsMagnitudeLimitsCalculator = new CanopusWfsCalculator {
    private val FaintLimit  = 17.8
    private val BrightLimit = 11.3

    override def gemsMagnitudeConstraint(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand]): MagnitudeConstraints =
      magLimits(RBandsList, FaintLimit, BrightLimit)

    override def getNominalMagnitudeConstraints(cwfs: CanopusWfs): MagnitudeConstraints =
      magLimits(RBandsList, FaintLimit, BrightLimit)
  }

  private lazy val Flamingos2OiwfsMagnitudeLimitsCalculator = new LimitsCalculator {
    override def gemsMagnitudeConstraint(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand]) =
      magLimits(RBandsList, 18.0, 9.5)
  }
}