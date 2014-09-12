package edu.gemini.ags.api

import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.catalog.api.MagnitudeLimits
import edu.gemini.catalog.api.MagnitudeLimits.{FaintnessLimit, SaturationLimit}
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.Magnitude.Band.{R, J, H, K}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.altair.{AltairAowfsGuider, InstAltair}
import edu.gemini.spModel.gemini.altair.AltairParams.Mode.{NGS, NGS_FL, LGS}
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe
import edu.gemini.spModel.gemini.gems.{Canopus, GemsInstrument}
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe
import edu.gemini.spModel.gemini.gnirs.GnirsOiwfsGuideProbe
import edu.gemini.spModel.gemini.gsaoi.{Gsaoi, GsaoiOdgw}
import edu.gemini.spModel.gemini.michelle.InstMichelle
import edu.gemini.spModel.gemini.nici.NiciOiwfsGuideProbe
import edu.gemini.spModel.gemini.nifs.NifsOiwfsGuideProbe
import edu.gemini.spModel.gemini.niri.NiriOiwfsGuideProbe
import edu.gemini.spModel.gemini.trecs.InstTReCS
import edu.gemini.spModel.gems.GemsGuideStarType
import edu.gemini.spModel.guide.{GuideSpeed, GuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions


/**
 * A magnitude table defined in the same way as we have done since before 2015A
 * (that is, with predefined magnitude limits).
 */
case class DefaultMagnitudeTable(ctx: ObsContext) extends MagnitudeTable {

  private def faint(band: Magnitude.Band, fl: Double): MagnitudeLimits =
    new MagnitudeLimits(band, new FaintnessLimit(fl), Option.empty[MagnitudeLimits.SaturationLimit].asGeminiOpt)

  private def magLimits(band: Magnitude.Band, fl: Double, sl: Double): MagnitudeLimits =
    new MagnitudeLimits(band, new FaintnessLimit(fl), new SaturationLimit(sl))

  def apply(site: Site, probe: GuideProbe): Option[MagnitudeCalc] = {
    def mc(nominalLimits: MagnitudeLimits): MagnitudeCalc = new MagnitudeCalc() {
      def apply(conds: Conditions, speed: GuideSpeed): MagnitudeLimits =
        nominalLimits.mapMagnitudes(conds.magAdjustOp()).mapMagnitudes(speed.magAdjustOp())
    }

    def ft(band: Magnitude.Band, fl: Double): Option[MagnitudeLimits] =
      Some(faint(band, fl))

    def ml(band: Magnitude.Band, fl: Double, sl: Double): Option[MagnitudeLimits] =
      Some(magLimits(band, fl, sl))

    ((site, probe) match {
      case (Site.GN, AltairAowfsGuider.instance)           =>
        ctx.getAOComponent.asScalaOpt.filter(_.isInstanceOf[InstAltair]).fold(Option(MagnitudeLimits.empty(R))) { ado =>
          ado.asInstanceOf[InstAltair].getMode match {
            case NGS | NGS_FL => ml(R, 15, -2)
            case LGS          => ml(R, 18, -2)
            case _            => None
          }
        }

      case (Site.GS, Flamingos2OiwfsGuideProbe.instance)   => ml(R, 15.0,  9.5)
      case (Site.GN, GmosOiwfsGuideProbe.instance)         => ml(R, 15.5,  9.5)
      case (Site.GS, GmosOiwfsGuideProbe.instance)         => ml(R, 14.5,  8.5)
      case (Site.GN, GnirsOiwfsGuideProbe.instance)        => ml(K, 14.0,  0.0)
      case (Site.GS, NiciOiwfsGuideProbe.instance)         => ml(R, 15.5, -2.0)
      case (Site.GN, NifsOiwfsGuideProbe.instance)         => ft(K, 14.5)
      case (Site.GN, NiriOiwfsGuideProbe.instance)         => ft(K, 14.0)

      case (s,       PwfsGuideProbe.pwfs1)                 =>
        Some(ctx.getInstrument).filter(_.isChopping).fold { site match {
          case Site.GN              => ml(R, 14.5, 9.0)
          case Site.GS              => ml(R, 13.0, 8.0)
        }} {_.getType match {
          case InstMichelle.SP_TYPE => ml(R, 13.0, 7.5)
          case InstTReCS.SP_TYPE    => ml(R, 12.0, 6.5)
          case _                    => None
        }}

      case (_,       PwfsGuideProbe.pwfs2)                 =>
        if (ctx.getInstrument.isChopping) ml(R, 13.0, 7.5)
        else                              ml(R, 14.5, 9.0)

      case (Site.GS, odgw) if odgw.isInstanceOf[GsaoiOdgw] =>
        Some(GsaoiOdgwMagnitudeLimitsCalculator.getGemsMagnitudeLimits(GemsGuideStarType.flexure, Some(H)))

      case (Site.GS, can) if can.isInstanceOf[Canopus.Wfs] =>
        Some(CanopusWfsMagnitudeLimitsCalculator.getGemsMagnitudeLimits(GemsGuideStarType.tiptilt, Some(R)))

      case _                                               => None
    }).map(mc)
  }

  /**
   * GSAOI, Canopus, and F2 require special handling for magnitude limits for GeMS.
   */
  trait GemsMagnitudeLimitsCalculator {
    def getGemsMagnitudeLimits(starType: GemsGuideStarType, nirBand: Option[Magnitude.Band]): MagnitudeLimits
    def getGemsMagnitudeLimitsForJava(starType: GemsGuideStarType, nirBand: edu.gemini.shared.util.immutable.Option[Magnitude.Band]): MagnitudeLimits =
      getGemsMagnitudeLimits(starType, nirBand.asScalaOpt)
  }

  /**
   * Unfortunately, we need a lookup table for the Mascot algorithm to map GemsInstruments to GemsMagnitudeLimitsCalculators.
   * We cannot include this in the GemsInstrument as this would cause dependency issues and we want to decouple these.
   */
  lazy val GemsInstrumentToMagnitudeLimitsCalculator = Map[GemsInstrument, GemsMagnitudeLimitsCalculator](
    GemsInstrument.gsaoi      -> GsaoiOdgwMagnitudeLimitsCalculator,
    GemsInstrument.flamingos2 -> Flamingos2OiwfsMagnitudeLimitsCalculator
  )

  private lazy val GsaoiOdgwMagnitudeLimitsCalculator = new GemsMagnitudeLimitsCalculator {
    /**
     * The map formerly in Gsaoi.Filter.
     */
    private val MagnitudeLimitsMap = Map[Pair[GemsGuideStarType, Magnitude.Band], MagnitudeLimits](
      (GemsGuideStarType.flexure, J) -> magLimits(J, 21.2, 12.0),
      (GemsGuideStarType.flexure, H) -> magLimits(H, 21.0, 12.0),
      (GemsGuideStarType.flexure, K) -> magLimits(K, 20.2, 11.0),
      (GemsGuideStarType.tiptilt, J) -> magLimits(J, 14.2,  7.1),
      (GemsGuideStarType.tiptilt, H) -> magLimits(H, 14.5,  7.3),
      (GemsGuideStarType.tiptilt, K) -> magLimits(K, 13.5,  6.5)
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
  trait CanopusWfsCalculator {
    def getNominalMagnitudeLimits(cwfs: Canopus.Wfs): MagnitudeLimits
  }
  lazy val CanopusWfsMagnitudeLimitsCalculator = new GemsMagnitudeLimitsCalculator with CanopusWfsCalculator {
    /**
     * The specific magnitude limits for the different Wfs probes.
     */
    private lazy val CwfsMagnitudeLimits = Map[Canopus.Wfs, MagnitudeLimits](
      Canopus.Wfs.cwfs1 -> magLimits(R, 18.0, 8.0),
      Canopus.Wfs.cwfs2 -> magLimits(R, 18.0, 8.5),
      Canopus.Wfs.cwfs3 -> magLimits(R, 17.5, 8.5)
    )
    override def getGemsMagnitudeLimits(starType: GemsGuideStarType, nirBand: Option[Magnitude.Band]) =
      magLimits(R, 18.0, 8.5)

    override def getNominalMagnitudeLimits(cwfs: Canopus.Wfs): MagnitudeLimits =
      CwfsMagnitudeLimits(cwfs)
  }

  private lazy val Flamingos2OiwfsMagnitudeLimitsCalculator = new GemsMagnitudeLimitsCalculator {
    override def getGemsMagnitudeLimits(starType: GemsGuideStarType, nirBand: Option[Magnitude.Band]) =
      magLimits(R, 18.0, 9.5)
  }
}