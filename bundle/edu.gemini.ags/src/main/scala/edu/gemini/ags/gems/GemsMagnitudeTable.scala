package edu.gemini.ags.gems

import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.ags.api.agsBandExtractor
import edu.gemini.pot.ModelConverters._
import edu.gemini.catalog.api._
import edu.gemini.spModel.core.{Angle, MagnitudeBand, Site}
import edu.gemini.spModel.gemini.gems.{Canopus, GemsInstrument}
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

  private def faint(band: MagnitudeBand, fl: Double): MagnitudeConstraints=
    MagnitudeConstraints(band, agsBandExtractor(band), FaintnessConstraint(fl), none)

  private def magLimits(band: MagnitudeBand, fl: Double, sl: Double): MagnitudeConstraints =
    MagnitudeConstraints(band, agsBandExtractor(band), FaintnessConstraint(fl), SaturationConstraint(sl).some)

  def apply(ctx: ObsContext, probe: GuideProbe): Option[MagnitudeCalc] = {
    def mc(nominalLimits: MagnitudeConstraints): MagnitudeCalc = new MagnitudeCalc() {
      def apply(conds: Conditions, speed: GuideSpeed): MagnitudeConstraints =
        conds.adjust(nominalLimits)
    }

    def ft(band: MagnitudeBand, fl: Double): Option[MagnitudeConstraints] =
      Some(faint(band, fl))

    def ml(band: MagnitudeBand, fl: Double, sl: Double): Option[MagnitudeConstraints] =
      Some(magLimits(band, fl, sl))

    def lookup(site: Site): Option[MagnitudeCalc] =
      ((site, probe) match {
        case (Site.GS, odgw: GsaoiOdgw) =>
          Some(GsaoiOdgwMagnitudeLimitsCalculator.gemsMagnitudeRange(GemsGuideStarType.flexure, MagnitudeBand.H.some))

        case (Site.GS, can: Canopus.Wfs) =>
          Some(CanopusWfsMagnitudeLimitsCalculator.gemsMagnitudeRange(GemsGuideStarType.tiptilt, MagnitudeBand.R.some))

        case _                                               => None
      }).map(mc)

    ctx.getSite.asScalaOpt.flatMap(lookup)
  }

  /**
   * GSAOI, Canopus, and F2 require special handling for magnitude limits for GeMS.
   */
  trait LimitsCalculator {
    def gemsMagnitudeRange(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand]): MagnitudeConstraints

    def adjustGemsMagnitudeRangeForJava(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand], conditions: Conditions): MagnitudeConstraints =
      conditions.adjust(gemsMagnitudeRange(starType, nirBand))

    def searchCriterionBuilder(name: String, radiusLimit: skycalc.Angle, instrument: GemsInstrument, magConstraint: MagnitudeConstraints, posAngles: java.util.Set[Angle]): CatalogSearchCriterion = {
      val radiusConstraint = RadiusConstraint.between(Angle.zero, radiusLimit.toNewModel)
      val searchOffset = instrument.getOffset.asScalaOpt.map(_.toNewModel)
      val searchPA = posAngles.asScala.headOption
      CatalogSearchCriterion(name, radiusConstraint, MagnitudeConstraints(magConstraint.referenceBand, agsBandExtractor(magConstraint.referenceBand), magConstraint.faintnessConstraint, magConstraint.saturationConstraint), searchOffset, searchPA)
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

  private lazy val GsaoiOdgwMagnitudeLimitsCalculator = new LimitsCalculator {
    /**
     * The map formerly in Gsaoi.Filter.
     */
    private val MagnitudeLimitsMap = Map[Pair[GemsGuideStarType, MagnitudeBand], MagnitudeConstraints](
      (GemsGuideStarType.flexure, MagnitudeBand.J) -> magLimits(MagnitudeBand.J, 17.2, 8.0),
      (GemsGuideStarType.flexure, MagnitudeBand.H) -> magLimits(MagnitudeBand.H, 17.0, 8.0),
      (GemsGuideStarType.flexure, MagnitudeBand.K) -> magLimits(MagnitudeBand.K, 18.2, 8.0),
      (GemsGuideStarType.tiptilt, MagnitudeBand.J) -> magLimits(MagnitudeBand.J, 14.2, 7.1),
      (GemsGuideStarType.tiptilt, MagnitudeBand.H) -> magLimits(MagnitudeBand.H, 14.5, 7.3),
      (GemsGuideStarType.tiptilt, MagnitudeBand.K) -> magLimits(MagnitudeBand.K, 13.5, 6.5)
    )

    override def gemsMagnitudeRange(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand]): MagnitudeConstraints= {
      val filter = nirBand.fold(Gsaoi.Filter.H)(band => Gsaoi.Filter.getFilter(band.toOldModel, Gsaoi.Filter.H))
      MagnitudeLimitsMap((starType, filter.getCatalogBand.getValue.toNewModel))
    }
  }

  /**
   * Since Canopus is not explicitly listed in GemsInstrument, it must be visible outside of the table in order to
   * be used directly by Mascot, since it cannot be looked up through the GemsInstrumentToMagnitudeLimitsCalculator map.
   */
  trait CanopusWfsCalculator extends LimitsCalculator {
    def getNominalMagnitudeConstraints(cwfs: Canopus.Wfs): MagnitudeConstraints
  }

  lazy val CanopusWfsMagnitudeLimitsCalculator = new CanopusWfsCalculator {
    override def gemsMagnitudeRange(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand]) =
      magLimits(MagnitudeBand.R, 15.5, 8.0)

    override def getNominalMagnitudeConstraints(cwfs: Canopus.Wfs): MagnitudeConstraints =
      magLimits(MagnitudeBand.R, 15.5, 8.0)
  }

  private lazy val Flamingos2OiwfsMagnitudeLimitsCalculator = new LimitsCalculator {
    override def gemsMagnitudeRange(starType: GemsGuideStarType, nirBand: Option[MagnitudeBand]) =
      magLimits(MagnitudeBand.R, 18.0, 9.5)
  }
}