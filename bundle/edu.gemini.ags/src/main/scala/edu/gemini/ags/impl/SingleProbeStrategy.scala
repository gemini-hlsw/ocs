package edu.gemini.ags.impl

import edu.gemini.ags.api._
import edu.gemini.ags.api.AgsMagnitude._
import edu.gemini.catalog.api.{QueryConstraint, CatalogServerInstances}
import edu.gemini.shared.skyobject.SkyObject
import edu.gemini.skycalc.{Coordinates, Angle}
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.CoordinateParam.Units
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
    AgsAnalysis.analysis(ctx, mt, params.guideProbe).toList

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
    val cv    = new CandidateValidator(params, mt, candidates)
    val steps = pac.steps(ctx.getPositionAngle, params.stepSize).toList.asScala
    val anglesWithResults  = steps.filter { angle => cv.exists(ctx.withPositionAngle(angle)) }
    val successProbability = anglesWithResults.size.toDouble / steps.size.toDouble
    AgsStrategy.Estimate.toEstimate(successProbability)
  }

  def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[AgsStrategy.Selection]] =
    catalogResult(ctx, mt).map(select(ctx, mt, _))

  def select(ctx: ObsContext, mt: MagnitudeTable, candidates: List[SkyObject]): Option[AgsStrategy.Selection] = {
    if (candidates.size == 0) None
    else {
      val results = ctx.getPosAngleConstraint match {
        case FIXED                         => selectBounded(List(ctx), mt, candidates)
        case FIXED_180 | PARALLACTIC_ANGLE => selectBounded(List(ctx, ctx180(ctx)), mt, candidates)
        case UNBOUNDED                     => selectUnbounded(ctx, mt, candidates)
      }
      brightest(results, params.band)(_._2).map {
        case (angle, skyObject) => AgsStrategy.Selection(angle, List(AgsStrategy.Assignment(params.guideProbe, skyObject)))
      }
    }
  }

  // List of candidates and their angles for the case where the pos angle constraint is not unbounded.
  private def selectBounded(alternatives: List[ObsContext], mt: MagnitudeTable, candidates: List[SkyObject]): List[(Angle, SkyObject)] = {
    val cv = new CandidateValidator(params, mt, candidates)
    alternatives.map(a => (a, cv.select(a))).collect {
      case (c, Some(so)) => (c.getPositionAngle, so)
    }
  }

  // List of candidates and their angles for the case where the pos angle constraint is bounded.
  private def selectUnbounded(ctx: ObsContext, mt: MagnitudeTable, candidates: List[SkyObject]): List[(Angle, SkyObject)] =
    candidates.map(so => (SingleProbeStrategy.calculatePositionAngle(ctx.getBaseCoordinates, so), so)).filter {
      case (angle, so) => new CandidateValidator(params, mt, List(so)).exists(ctx.withPositionAngle(angle))
    }

  override def queryConstraints(ctx: ObsContext, mt: MagnitudeTable): List[QueryConstraint] =
    params.queryConstraints(ctx, mt).toList

  override val guideProbes: List[GuideProbe] =
    List(params.guideProbe)
}


object SingleProbeStrategy {
  /**
   * Calculate the position angle to a target from a specified base position.
   */
  def calculatePositionAngle(base: Coordinates, so: SkyObject): Angle = {
    val ra1    = base.getRa.toRadians.getMagnitude
    val dec1   = base.getDec.toRadians.getMagnitude
    val target = new SPTarget(so).getTarget
    val ra2    = new Angle(target.getC1.getAs(Units.DEGREES), Angle.Unit.DEGREES).toRadians.getMagnitude
    val dec2   = new Angle(target.getC2.getAs(Units.DEGREES), Angle.Unit.DEGREES).toRadians.getMagnitude
    val raDiff = ra2 - ra1
    val angle  = Math.atan2(Math.sin(raDiff), Math.cos(dec1) * Math.tan(dec2) - Math.sin(dec1) * Math.cos(raDiff))
    new Angle(angle, Angle.Unit.RADIANS)
  }
}