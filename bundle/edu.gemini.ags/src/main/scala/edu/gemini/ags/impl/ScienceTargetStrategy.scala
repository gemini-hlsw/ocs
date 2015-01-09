package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsMagnitude._
import edu.gemini.ags.impl._
import edu.gemini.ags.api.{AgsMagnitude, AgsAnalysis, AgsStrategy}
import edu.gemini.catalog.api.{RadiusConstraint, QueryConstraint}
import edu.gemini.shared.skyobject.SkyObject
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.guide.{ValidatableGuideProbe, GuideProbe}
import edu.gemini.spModel.obs.context.ObsContext

import scala.concurrent.Future

case class ScienceTargetStrategy(key: AgsStrategyKey, guideProbe: ValidatableGuideProbe) extends AgsStrategy {

  // Since the science target is the used as the guide star, success is always guaranteed.
  override def estimate(ctx: ObsContext, mt: MagnitudeTable): Future[AgsStrategy.Estimate] =
    Future.successful(AgsStrategy.Estimate.GuaranteedSuccess)

  override def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] =
    List(AgsAnalysis.analysis(ctx, mt, guideProbe))

  override def candidates(ctx: ObsContext, mt: MagnitudeTable): Future[List[(GuideProbe, List[SiderealTarget])]] = {
    val so = ctx.getTargets.getBase.toNewModel
    Future.successful(List((guideProbe, List(so))))
  }

  override def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[AgsStrategy.Selection]] = {
    // The science target is the guide star, but must be converted from SPTarget to SkyObject.
    val siderealTarget = ctx.getTargets.getBase.toNewModel
    val posAngle       = ctx.getPositionAngle.toNewModel
    val assignment     = AgsStrategy.Assignment(guideProbe, siderealTarget)
    val selection      = AgsStrategy.Selection(posAngle, List(assignment))
    Future.successful(Some(selection))
  }

  override def magnitudes(ctx: ObsContext, mt: MagnitudeTable): List[(GuideProbe, MagnitudeCalc)] =
    mt(ctx, guideProbe).toList.map((guideProbe, _))

  override def queryConstraints(ctx: ObsContext, mt: MagnitudeTable): List[QueryConstraint] =
    (for {
      mc <- magnitudeCalc(ctx, mt)
      rc <- radiusLimits(ctx)
      rl =  rc.toRadiusLimit
      ml <- AgsMagnitude.manualSearchLimits(mc)
    } yield new QueryConstraint(ctx.getBaseCoordinates, rl, ml.toMagnitudeLimits)).toList

  private def radiusLimits(ctx: ObsContext): Option[RadiusConstraint] =
    RadiusLimitCalc.getAgsQueryRadiusLimits(guideProbe, ctx)

  private def magnitudeCalc(ctx: ObsContext, mt: MagnitudeTable): Option[MagnitudeCalc] =
    mt(ctx, guideProbe)

  override val guideProbes: List[GuideProbe] = List(guideProbe)
}
