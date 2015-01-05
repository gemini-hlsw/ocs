package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsMagnitude
import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.catalog.api.{RadiusConstraint, QueryConstraint, RadiusLimits}
import edu.gemini.skycalc.Angle
import edu.gemini.spModel.core.Site
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
import edu.gemini.shared.skyobject.Magnitude

sealed trait SingleProbeStrategyParams {
  def site: Site
  def band: Magnitude.Band = Magnitude.Band.R
  def guideProbe: ValidatableGuideProbe
  def stepSize: Angle            = new Angle(10, Angle.Unit.DEGREES)
  def minDistance: Option[Angle] = Some(new Angle(20, Angle.Unit.ARCSECS))

  final def queryConstraints(ctx: ObsContext, mt: MagnitudeTable): Option[QueryConstraint] =
    for {
      mc <- magnitudeCalc(ctx, mt)
      rc <- radiusLimits(ctx)
      rl =  rc.toRadiusLimit
    } yield new QueryConstraint(ctx.getBaseCoordinates, rl, AgsMagnitude.manualSearchLimits(mc))

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
    override def stepSize    = new Angle(90, Angle.Unit.DEGREES)
    override def minDistance = Some(new Angle(0.0, Angle.Unit.ARCSECS))
  }

  case object Flamingos2OiwfsParams extends SingleProbeStrategyParams {
    val guideProbe        = Flamingos2OiwfsGuideProbe.instance
    val site              = Site.GS
    override def stepSize = new Angle(90, Angle.Unit.DEGREES)

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
    override val band = Magnitude.Band.K
  }

  case object NifsOiwfsParams extends SingleProbeStrategyParams {
    val guideProbe    = NifsOiwfsGuideProbe.instance
    val site          = Site.GN
    override val band = Magnitude.Band.K

  }

  case object NiriOiwfsParams extends SingleProbeStrategyParams {
    val guideProbe    = NiriOiwfsGuideProbe.instance
    val site          = Site.GN
    override val band = Magnitude.Band.K
  }

case class PwfsParams(site: Site, guideProbe: PwfsGuideProbe) extends SingleProbeStrategyParams {
    override def stepSize = new Angle(360, Angle.Unit.DEGREES)

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