package edu.gemini.ags.impl

import edu.gemini.ags.api._
import edu.gemini.ags.api.AgsMagnitude._
import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.{NoBands, SiderealTarget}
import edu.gemini.spModel.guide.{GuideProbe, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._
import Scalaz._

/**
 * A Strategy with dummy settings to be used when no guide star is needed.
 */
case object OffStrategy extends AgsStrategy {

  override val key = AgsStrategyKey.OffKey

  override def magnitudes(ctx: ObsContext, mt: MagnitudeTable): List[(GuideProbe, AgsMagnitude.MagnitudeCalc)] = Nil

  override def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] = Nil

  override def analyze(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] = None

  override def analyzeMagnitude(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] = None

  override def catalogQueries(ctx: ObsContext, mt: MagnitudeTable): List[CatalogQuery] = Nil

  override def candidates(ctx: ObsContext, mt: MagnitudeTable)(ec: ExecutionContext): Future[List[ProbeCandidates]] = Nil.pure[Future]

  override def estimate(ctx: ObsContext, mt: MagnitudeTable)(ec: ExecutionContext): Future[AgsStrategy.Estimate] = AgsStrategy.Estimate.CompleteFailure.pure[Future]

  override def select(ctx: ObsContext, mt: MagnitudeTable)(ec: ExecutionContext): Future[Option[AgsStrategy.Selection]] = None.pure[Future]

  override val guideProbes: List[GuideProbe] = Nil

  override val probeBands = NoBands

}
