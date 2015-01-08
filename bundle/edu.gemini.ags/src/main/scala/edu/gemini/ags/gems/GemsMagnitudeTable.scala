package edu.gemini.ags.gems

import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.ags.impl._
import edu.gemini.catalog.api.{SaturationConstraint, FaintnessConstraint, MagnitudeConstraints, MagnitudeLimits}
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.Magnitude.Band.{R, J, H, K}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.gems.{Canopus, GemsInstrument}
import edu.gemini.spModel.gemini.gsaoi.{Gsaoi, GsaoiOdgw}
import edu.gemini.spModel.gems.GemsGuideStarType
import edu.gemini.spModel.guide.{GuideSpeed, GuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions

import scalaz._
import Scalaz._

/**
 * A magnitude table defined in the same way as we have done since before 2015A
 * (that is, with predefined magnitude limits).
 */
object GemsMagnitudeTable extends MagnitudeTable {

  private def faint(band: Magnitude.Band, fl: Double): MagnitudeConstraints=
    MagnitudeConstraints(band.toNewModel, FaintnessConstraint(fl), none)

  private def magLimits(band: Magnitude.Band, fl: Double, sl: Double): MagnitudeConstraints =
    MagnitudeConstraints(band.toNewModel, FaintnessConstraint(fl), SaturationConstraint(sl).some)

  def apply(ctx: ObsContext, probe: GuideProbe): Option[MagnitudeCalc] = {
    def mc(nominalLimits: MagnitudeConstraints): MagnitudeCalc = new MagnitudeCalc() {
      def apply(conds: Conditions, speed: GuideSpeed): MagnitudeConstraints =
        nominalLimits.map(m => conds.magAdjustOp().apply(m.toOldModel).toNewModel)
    }

    def ft(band: Magnitude.Band, fl: Double): Option[MagnitudeConstraints] =
      Some(faint(band, fl))

    def ml(band: Magnitude.Band, fl: Double, sl: Double): Option[MagnitudeConstraints] =
      Some(magLimits(band, fl, sl))

    def lookup(site: Site): Option[MagnitudeCalc] =
      ((site, probe) match {
        case (Site.GS, odgw) if odgw.isInstanceOf[GsaoiOdgw] =>
          Some(GsaoiOdgwMagnitudeLimitsCalculator.getGemsMagnitudeConstraints(GemsGuideStarType.flexure, Some(H)))

        case (Site.GS, can) if can.isInstanceOf[Canopus.Wfs] =>
          Some(CanopusWfsMagnitudeLimitsCalculator.getGemsMagnitudeConstraints(GemsGuideStarType.tiptilt, Some(R)))

        case _                                               => None
      }).map(mc)

    ctx.getSite.asScalaOpt.flatMap(lookup)
  }

  /**
   * GSAOI, Canopus, and F2 require special handling for magnitude limits for GeMS.
   */
  trait LimitsCalculator {
    def getGemsMagnitudeConstraints(starType: GemsGuideStarType, nirBand: Option[Magnitude.Band]): MagnitudeConstraints
    def adjustGemsMagnitudeLimitsForJava(starType: GemsGuideStarType, nirBand: edu.gemini.shared.util.immutable.Option[Magnitude.Band], conditions: Conditions): MagnitudeLimits =
      getGemsMagnitudeConstraints(starType, nirBand.asScalaOpt).map(m => conditions.magAdjustOp.apply(m.toOldModel).toNewModel).toMagnitudeLimits
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
    private val MagnitudeLimitsMap = Map[Pair[GemsGuideStarType, Magnitude.Band], MagnitudeConstraints](
      (GemsGuideStarType.flexure, J) -> magLimits(J, 17.2, 8.0),
      (GemsGuideStarType.flexure, H) -> magLimits(H, 17.0, 8.0),
      (GemsGuideStarType.flexure, K) -> magLimits(K, 18.2, 8.0),
      (GemsGuideStarType.tiptilt, J) -> magLimits(J, 14.2, 7.1),
      (GemsGuideStarType.tiptilt, H) -> magLimits(H, 14.5, 7.3),
      (GemsGuideStarType.tiptilt, K) -> magLimits(K, 13.5, 6.5)
    )

    override def getGemsMagnitudeConstraints(starType: GemsGuideStarType, nirBand: Option[Magnitude.Band]): MagnitudeConstraints= {
      val filter = nirBand.fold(Gsaoi.Filter.H)(band => Gsaoi.Filter.getFilter(band, Gsaoi.Filter.H))
      MagnitudeLimitsMap((starType, filter.getCatalogBand.getValue))
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
    override def getGemsMagnitudeConstraints(starType: GemsGuideStarType, nirBand: Option[Magnitude.Band]) =
      magLimits(R, 15.5, 8.0)

    override def getNominalMagnitudeConstraints(cwfs: Canopus.Wfs): MagnitudeConstraints =
      magLimits(R, 15.5, 8.0)
  }

  private lazy val Flamingos2OiwfsMagnitudeLimitsCalculator = new LimitsCalculator {
    override def getGemsMagnitudeConstraints(starType: GemsGuideStarType, nirBand: Option[Magnitude.Band]) =
      magLimits(R, 18.0, 9.5)
  }
}