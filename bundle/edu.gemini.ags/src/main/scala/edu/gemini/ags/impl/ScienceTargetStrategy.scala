package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsMagnitude._
import edu.gemini.pot.ModelConverters._
import edu.gemini.ags.api.{AgsAnalysis, AgsStrategy}
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.BandsList
import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.guide.{ValidatableGuideProbe, GuideProbe}
import edu.gemini.spModel.obs.context.ObsContext

import scala.concurrent.{ExecutionContext, Future}

/** This strategy is for instruments that guide on the science target. */
case class ScienceTargetStrategy(key: AgsStrategyKey, guideProbe: ValidatableGuideProbe, override val probeBands: BandsList) extends AgsStrategy {

  // Since the science target is the used as the guide star, success is always guaranteed.
  override def estimate(ctx: ObsContext, mt: MagnitudeTable)(ec: ExecutionContext): Future[AgsStrategy.Estimate] =
  Future.successful(AgsStrategy.Estimate.GuaranteedSuccess)

  override def analyze(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] =
    AgsAnalysis.analysis(ctx, mt, guideProbe, guideStar, probeBands)

  override def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] =
    AgsAnalysis.analysis(ctx, mt, guideProbe, probeBands).toList

  private def toSiderealTargets(ctx: ObsContext): List[SiderealTarget] = {
    val when = ctx.getSchedulingBlockStart
    val ts   = ctx.getTargets.getAsterism.allSpTargets.map(_.toSiderealTarget(when))
    ts.list.toList
  }

  override def candidates(ctx: ObsContext, mt: MagnitudeTable)(ec: ExecutionContext): Future[List[(GuideProbe, List[SiderealTarget])]] =
    Future.successful(List((guideProbe, toSiderealTargets(ctx))))

  override def select(ctx: ObsContext, mt: MagnitudeTable)(ec: ExecutionContext): Future[Option[AgsStrategy.Selection]] = {
    // As of 17B there are no instruments that use this strategy with a multi-target asterism (which
    // could result in mutiple assignments below) so we use only the first sidereal target we find.
    // In practice this is fine. A multi-target asterism in this context is a configuration error
    // that will result in a stern warning.
    val siderealTargets = toSiderealTargets(ctx).take(1)
    val assignments     = siderealTargets.map(AgsStrategy.Assignment(guideProbe, _))
    val posAngle        = ctx.getPositionAngle
    val selection       = AgsStrategy.Selection(posAngle, assignments)
    Future.successful(Some(selection))
  }

  override def magnitudes(ctx: ObsContext, mt: MagnitudeTable): List[(GuideProbe, MagnitudeCalc)] =
    mt(ctx, guideProbe).toList.map((guideProbe, _))

  override val guideProbes: List[GuideProbe] = List(guideProbe)

  // No queries are required for this strategy
  override def catalogQueries(ctx: ObsContext, mt: MagnitudeTable) = Nil
}