// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.ags.impl

import edu.gemini.ags.api.{AgsAnalysis, AgsMagnitude, AgsStrategy, ProbeCandidates}
import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.ags.AgsStrategyKey.{GhostPwfs1Key, GhostPwfs2Key}
import edu.gemini.spModel.core.{BandsList, SiderealTarget}
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.guide.{GuideProbe, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext

import scala.concurrent.{ExecutionContext, Future}


/**
 * The GHOST AGS strategy is essentially a PWFS strategy which treats the
 * unlinked base position (if any) of the single target mode as the base
 * position.  In other words, if Single Target with overridden base position
 * we ignore the override and use the single target as the base.  If any other
 * version of GHOST asterism (like Dual Target), we continue to use the
 * actual base position.
 */
final case class GhostStrategy(key: AgsStrategyKey, delegate: AgsStrategy) extends AgsStrategy {

  private def adjContext(ctx: ObsContext): ObsContext = {
    val env = ctx.getTargets()
    env.getAsterism match {
      case GhostAsterism.SingleTarget(t, Some(_)) =>
        // Single target w/o overridden base so that the guide star search works
        // as if the base position is the single target.
        ctx.withTargets(env.setAsterism(GhostAsterism.SingleTarget(t, None)))
      case _                                      =>
        ctx
    }
  }

  override def magnitudes(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable): List[(GuideProbe, AgsMagnitude.MagnitudeCalc)] =
    delegate.magnitudes(adjContext(ctx), mt)

  override def analyze(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable): List[AgsAnalysis] =
    delegate.analyze(adjContext(ctx), mt)

  override def analyze(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] =
    delegate.analyze(adjContext(ctx), mt, guideProbe, guideStar)

  override def analyzeMagnitude(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] =
    delegate.analyzeMagnitude(adjContext(ctx), mt, guideProbe, guideStar)

  override def candidates(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable)(ec: ExecutionContext): Future[List[ProbeCandidates]] =
    delegate.candidates(adjContext(ctx), mt)(ec)

  override def catalogQueries(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable): List[CatalogQuery] =
    delegate.catalogQueries(adjContext(ctx), mt)

  override def estimate(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable)(ec: ExecutionContext): Future[AgsStrategy.Estimate] =
    delegate.estimate(adjContext(ctx), mt)(ec)

  override def select(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable)(ec: ExecutionContext): Future[Option[AgsStrategy.Selection]] =
    delegate.select(adjContext(ctx), mt)(ec)

  override def guideProbes: List[GuideProbe] =
    delegate.guideProbes

  override def probeBands: BandsList =
    delegate.probeBands
}
