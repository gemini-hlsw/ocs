package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsMagnitude
import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.catalog.api.MagnitudeLimits.{SaturationLimit, FaintnessLimit}
import edu.gemini.catalog.api.{MagnitudeLimits, RadiusConstraint, QueryConstraint}
import edu.gemini.spModel.core.{MagnitudeBand, Angle, Site}
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe
import edu.gemini.spModel.gemini.gnirs.GnirsOiwfsGuideProbe
import edu.gemini.spModel.gemini.nifs.NifsOiwfsGuideProbe
import edu.gemini.spModel.gemini.niri.NiriOiwfsGuideProbe
import edu.gemini.spModel.guide.{GuideStarValidator, PatrolField, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe

sealed trait SingleProbeStrategyParams {
  def site: Site
  def band: MagnitudeBand = MagnitudeBand.R
  def guideProbe: ValidatableGuideProbe
  def stepSize: Angle            = Angle.fromDegrees(10)
  def minDistance: Option[Angle] = Some(Angle.fromArcsecs(20))

  final def queryConstraints(ctx: ObsContext, mt: MagnitudeTable): Option[QueryConstraint] =
    for {
      mc <- magnitudeCalc(ctx, mt)
      rc <- radiusLimits(ctx)
      rl =  rc.toRadiusLimit
      ml =  AgsMagnitude.manualSearchLimits(mc)
    } yield new QueryConstraint(ctx.getBaseCoordinates, rl, new MagnitudeLimits(ml.band, new FaintnessLimit(ml.faintnessConstraint.brightness), ml.saturationConstraint.map(s => new SaturationLimit(s.brightness)).asGeminiOpt))

  def radiusLimits(ctx: ObsContext): Option[RadiusConstraint] =
    RadiusLimitCalc.getAgsQueryRadiusLimits(guideProbe, ctx)

  def magnitudeCalc(ctx: ObsContext, mt: MagnitudeTable): Option[MagnitudeCalc] =
    mt(ctx, guideProbe)

  def validator(ctx: ObsContext): GuideStarValidator = guideProbe
}

object SingleProbeStrategyParams {
  case object AltairAowfsParams extends SingleProbeStrategyParams {
    val guideProbe           = AltairAowfsGuider.instance
    val site                 = Site.GN
    override def stepSize    = Angle.fromDegrees(90)
    override def minDistance = Some(Angle.zero)
  }

  case object Flamingos2OiwfsParams extends SingleProbeStrategyParams {
    val guideProbe        = Flamingos2OiwfsGuideProbe.instance
    val site              = Site.GS
    override def stepSize = Angle.fromDegrees(90)

    // Note, for single probe strategy it is always OIWFS....
//    override def radiusLimits(ctx: ObsContext): Option[RadiusLimits] = {
//      def flexureLimits: Option[RadiusLimits] =
//        RadiusLimitCalc.getAgsQueryRadiusLimits(PatrolField.fromRadiusLimits(new Angle(0.33, Angle.Unit.ARCMINS), new Angle(Canopus.RADIUS_ARCSEC, Angle.Unit.ARCSECS)), ctx).asScalaOpt
//      if (!ctx.getAOComponent.isEmpty) flexureLimits
//      else super.radiusLimits(ctx)
//    }
  }

  case class GmosOiwfsParams(site: Site) extends SingleProbeStrategyParams {
    val guideProbe = GmosOiwfsGuideProbe.instance
  }

  case object GnirsOiwfsParams extends SingleProbeStrategyParams {
    val guideProbe    = GnirsOiwfsGuideProbe.instance
    val site          = Site.GN
    override val band = MagnitudeBand.K
  }

  case object NifsOiwfsParams extends SingleProbeStrategyParams {
    val guideProbe    = NifsOiwfsGuideProbe.instance
    val site          = Site.GN
    override val band = MagnitudeBand.K

  }

  case object NiriOiwfsParams extends SingleProbeStrategyParams {
    val guideProbe    = NiriOiwfsGuideProbe.instance
    val site          = Site.GN
    override val band = MagnitudeBand.K
  }

case class PwfsParams(site: Site, guideProbe: PwfsGuideProbe) extends SingleProbeStrategyParams {
    override def stepSize = Angle.fromDegrees(360)

    private def vignettingProofPatrolField(ctx: ObsContext): PatrolField = {
      val min = guideProbe.getVignettingClearance(ctx)
      guideProbe.getCorrectedPatrolField(PatrolField.fromRadiusLimits(min, PwfsGuideProbe.PWFS_RADIUS), ctx)
    }

    override def radiusLimits(ctx: ObsContext): Option[RadiusConstraint] =
      RadiusLimitCalc.getAgsQueryRadiusLimits(Some(vignettingProofPatrolField(ctx)), ctx)

    // We have a special validator for Pwfs.
    override def validator(ctx: ObsContext): GuideStarValidator =
      vignettingProofPatrolField(ctx).validator(ctx)
  }
}