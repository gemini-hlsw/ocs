package edu.gemini.ags.gems

import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.catalog.api.MagnitudeLimits
import edu.gemini.catalog.api.MagnitudeLimits.{FaintnessLimit, SaturationLimit}
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


/**
 * A magnitude table defined in the same way as we have done since before 2015A
 * (that is, with predefined magnitude limits).
 */
object GemsMagnitudeTable extends MagnitudeTable {

  private def faint(band: Magnitude.Band, fl: Double): MagnitudeLimits =
    new MagnitudeLimits(band, new FaintnessLimit(fl), Option.empty[MagnitudeLimits.SaturationLimit].asGeminiOpt)

  private def magLimits(band: Magnitude.Band, fl: Double, sl: Double): MagnitudeLimits =
    new MagnitudeLimits(band, new FaintnessLimit(fl), new SaturationLimit(sl))

  def apply(ctx: ObsContext, probe: GuideProbe): Option[MagnitudeCalc] = {
    def mc(nominalLimits: MagnitudeLimits): MagnitudeCalc = new MagnitudeCalc() {
      def apply(conds: Conditions, speed: GuideSpeed): MagnitudeLimits =
        nominalLimits.mapMagnitudes(conds.magAdjustOp())
    }

    def ft(band: Magnitude.Band, fl: Double): Option[MagnitudeLimits] =
      Some(faint(band, fl))

    def ml(band: Magnitude.Band, fl: Double, sl: Double): Option[MagnitudeLimits] =
      Some(magLimits(band, fl, sl))

    def lookup(site: Site): Option[MagnitudeCalc] =
      ((site, probe) match {
        case (Site.GS, odgw) if odgw.isInstanceOf[GsaoiOdgw] =>
          Some(GsaoiOdgwMagnitudeLimitsCalculator.getGemsMagnitudeLimits(GemsGuideStarType.flexure, Some(H)))

        case (Site.GS, can) if can.isInstanceOf[Canopus.Wfs] =>
          Some(CanopusWfsMagnitudeLimitsCalculator.getGemsMagnitudeLimits(GemsGuideStarType.tiptilt, Some(R)))

        case _                                               => None
      }).map(mc)

    ctx.getSite.asScalaOpt.flatMap(lookup)
  }

  /**
   * GSAOI, Canopus, and F2 require special handling for magnitude limits for GeMS.
   */
  trait LimitsCalculator {
    def getGemsMagnitudeLimits(starType: GemsGuideStarType, nirBand: Option[Magnitude.Band]): MagnitudeLimits
    def getGemsMagnitudeLimitsForJava(starType: GemsGuideStarType, nirBand: edu.gemini.shared.util.immutable.Option[Magnitude.Band]): MagnitudeLimits =
      getGemsMagnitudeLimits(starType, nirBand.asScalaOpt)
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
    private val MagnitudeLimitsMap = Map[Pair[GemsGuideStarType, Magnitude.Band], MagnitudeLimits](
      (GemsGuideStarType.flexure, J) -> magLimits(J, 17.2, 8.0),
      (GemsGuideStarType.flexure, H) -> magLimits(H, 17.0, 8.0),
      (GemsGuideStarType.flexure, K) -> magLimits(K, 18.2, 8.0),
      (GemsGuideStarType.tiptilt, J) -> magLimits(J, 14.2, 7.1),
      (GemsGuideStarType.tiptilt, H) -> magLimits(H, 14.5, 7.3),
      (GemsGuideStarType.tiptilt, K) -> magLimits(K, 13.5, 6.5)
    )

    override def getGemsMagnitudeLimits(starType: GemsGuideStarType, nirBand: Option[Magnitude.Band]): MagnitudeLimits = {
      val filter = nirBand.fold(Gsaoi.Filter.H)(band => Gsaoi.Filter.getFilter(band, Gsaoi.Filter.H))
      MagnitudeLimitsMap((starType, filter.getCatalogBand.getValue))
    }
  }

  /**
   * Since Canopus is not explicitly listed in GemsInstrument, it must be visible outside of the table in order to
   * be used directly by Mascot, since it cannot be looked up through the GemsInstrumentToMagnitudeLimitsCalculator map.
   */
  trait CanopusWfsCalculator extends LimitsCalculator {
    def getNominalMagnitudeLimits(cwfs: Canopus.Wfs): MagnitudeLimits
  }

  lazy val CanopusWfsMagnitudeLimitsCalculator = new CanopusWfsCalculator {
    override def getGemsMagnitudeLimits(starType: GemsGuideStarType, nirBand: Option[Magnitude.Band]) =
      magLimits(R, 15.5, 8.0)

    override def getNominalMagnitudeLimits(cwfs: Canopus.Wfs): MagnitudeLimits =
      magLimits(R, 15.5, 8.0)
  }

  private lazy val Flamingos2OiwfsMagnitudeLimitsCalculator = new LimitsCalculator {
    override def getGemsMagnitudeLimits(starType: GemsGuideStarType, nirBand: Option[Magnitude.Band]) =
      magLimits(R, 18.0, 9.5)
  }
}