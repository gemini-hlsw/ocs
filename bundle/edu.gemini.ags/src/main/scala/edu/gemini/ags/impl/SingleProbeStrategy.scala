package edu.gemini.ags.impl

import edu.gemini.ags.api._
import edu.gemini.ags.api.AgsMagnitude._
import edu.gemini.catalog.api.{QueryConstraint, CatalogServerInstances}
import edu.gemini.shared.skyobject.SkyObject
import edu.gemini.skycalc.Angle
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.telescope.PosAngleConstraint
import edu.gemini.spModel.telescope.PosAngleConstraint._

import scala.collection.JavaConverters._
import scala.concurrent._
import ExecutionContext.Implicits.global


/**
 * Implements the logic for estimation and selection for a single guide probe.
 * The same logic is applied to various single-star guiding scenarios (i.e.,
 * everything except for GeMS).
 */
case class SingleProbeStrategy(key: AgsStrategyKey, params: SingleProbeStrategyParams) extends AgsStrategy {

  def magnitudes(ctx: ObsContext, mt: MagnitudeTable): List[(GuideProbe, AgsMagnitude.MagnitudeCalc)] =
    params.magnitudeCalc(ctx, mt).toList.map(params.guideProbe -> _)

  def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] =
    List(AgsAnalysis.analysis(ctx, mt, params.guideProbe))

  def candidates(ctx: ObsContext, mt: MagnitudeTable): Future[List[(GuideProbe, List[SkyObject])]] = {
    val empty = List((params.guideProbe: GuideProbe, List.empty[SkyObject]))
    queryConstraints(ctx, mt).foldLeft(Future.successful(empty)) { (_, qc) =>
      future {
        CatalogServerInstances.STANDARD.query(qc).candidates.toList.asScala.toList
      }.map { so => List((params.guideProbe, so)) }
    }
  }

  def catalogResult(ctx: ObsContext, mt: MagnitudeTable): Future[List[SkyObject]] =
    // call candidates and extract the one and only tuple for this strategy,
    // throw away the guide probe (which we know anyway), and obtain just the
    // list of guide stars
    candidates(ctx, mt).map { lst =>
      lst.headOption.fold(List.empty[SkyObject]) { case (_, so) => so }
    }

  def estimate(ctx: ObsContext, mt: MagnitudeTable): Future[AgsStrategy.Estimate] =
    catalogResult(ctx, mt).map(estimate(ctx, mt, _))

  def estimate(ctx: ObsContext, mt: MagnitudeTable, candidates: List[SkyObject]): AgsStrategy.Estimate = {
    // If we are unbounded and there are any candidates, we are guaranteed success.
    val pac   = ctx.getPosAngleConstraint(UNBOUNDED)
    if (pac == UNBOUNDED) {
      if (candidates.size > 0) AgsStrategy.Estimate.GuaranteedSuccess
      else AgsStrategy.Estimate.CompleteFailure
    }
    else {
      val cv = new CandidateValidator(params, mt, candidates)
      val steps = pac.steps(ctx.getPositionAngle, params.stepSize).toList.asScala
      val anglesWithResults = steps.filter { angle => cv.exists(ctx.withPositionAngle(angle))}

      // For FIXED_180 and PARALLACTIC_ANGLE, we return guaranteed success (1.0) if either position angle
      // has candidates.
      val successProbability = anglesWithResults.size.toDouble / steps.size.toDouble
      if (pac == FIXED) {
        if (successProbability > 0.0) AgsStrategy.Estimate.GuaranteedSuccess
        else AgsStrategy.Estimate.CompleteFailure
      }
      else AgsStrategy.Estimate.toEstimate(successProbability)
    }
  }

  def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[AgsStrategy.Selection]] =
    catalogResult(ctx, mt).map(select(ctx, mt, _))

  def select(ctx: ObsContext, mt: MagnitudeTable, candidates: List[SkyObject]): Option[AgsStrategy.Selection] = {
    // List of candidates and their angles for the case where the pos angle constraint is not unbounded.
    def selectBounded(alternatives: List[ObsContext]): List[(Angle, SkyObject)] = {
      val cv = new CandidateValidator(params, mt, candidates)
      alternatives.map(a => (a, cv.select(a))).collect {
        case (c, Some(so)) => (c.getPositionAngle, so)
      }
    }

    // List of candidates and their angles for the case where the pos angle constraint is bounded.
    def selectUnbounded(): List[(Angle, SkyObject)] =
      candidates.map(so => (new SPTarget(so).calculatePositionAngle(ctx.getBaseCoordinates), so)).filter {
        case (angle, so) => new CandidateValidator(params, mt, List(so)).exists(ctx.withPositionAngle(angle))
      }

    if (candidates.size == 0) None
    else {
      val results = ctx.getPosAngleConstraint match {
        case FIXED                         => selectBounded(List(ctx))
        case FIXED_180 | PARALLACTIC_ANGLE => selectBounded(List(ctx, ctx180(ctx)))
        case UNBOUNDED                     => selectUnbounded()
      }
      brightest(results, params.band)(_._2).map {
        case (angle, skyObject) => AgsStrategy.Selection(angle, List(AgsStrategy.Assignment(params.guideProbe, skyObject)))
      }
    }
  }

  override def queryConstraints(ctx: ObsContext, mt: MagnitudeTable): List[QueryConstraint] =
    params.queryConstraints(ctx, mt).toList

  override val guideProbes: List[GuideProbe] =
    List(params.guideProbe)
}
