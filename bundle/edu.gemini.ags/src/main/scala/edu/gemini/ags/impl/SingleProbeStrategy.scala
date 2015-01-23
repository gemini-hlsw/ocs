package edu.gemini.ags.impl

import edu.gemini.ags.api._
import edu.gemini.ags.api.AgsMagnitude._
import edu.gemini.catalog.api.{QueryConstraint, CatalogServerInstances}
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.{Coordinates, Angle}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.CoordinateParam.Units
import edu.gemini.spModel.target.system.HmsDegTarget
import edu.gemini.spModel.telescope.PosAngleConstraint._

import scala.collection.JavaConverters._
import scala.concurrent._
import ExecutionContext.Implicits.global

import scalaz._
import Scalaz._

/**
 * Implements the logic for estimation and selection for a single guide probe.
 * The same logic is applied to various single-star guiding scenarios (i.e.,
 * everything except for GeMS).
 */
case class SingleProbeStrategy(key: AgsStrategyKey, params: SingleProbeStrategyParams) extends AgsStrategy {

  override def magnitudes(ctx: ObsContext, mt: MagnitudeTable): List[(GuideProbe, AgsMagnitude.MagnitudeCalc)] =
    params.magnitudeCalc(ctx, mt).toList.map(params.guideProbe -> _)

  override def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] =
    List(AgsAnalysis.analysis(ctx, mt, params.guideProbe))

  override def candidates(ctx: ObsContext, mt: MagnitudeTable): Future[List[(GuideProbe, List[SiderealTarget])]] = {
    val empty = List((params.guideProbe: GuideProbe, List.empty[SiderealTarget]))
    queryConstraints(ctx, mt).foldLeft(Future.successful(empty)) { (_, qc) =>
      future {
        CatalogServerInstances.STANDARD.query(qc).candidates.toList.asScala.toList
      }.map { so => List((params.guideProbe, so.map(_.toNewModel))) }
    }
  }

  private def catalogResult(ctx: ObsContext, mt: MagnitudeTable): Future[List[SiderealTarget]] =
    // call candidates and extract the one and only tuple for this strategy,
    // throw away the guide probe (which we know anyway), and obtain just the
    // list of guide stars
    candidates(ctx, mt).map { lst =>
      lst.headOption.foldMap(_._2)
    }

  override def estimate(ctx: ObsContext, mt: MagnitudeTable): Future[AgsStrategy.Estimate] =
    catalogResult(ctx, mt).map(estimate(ctx, mt, _))

  def estimate(ctx: ObsContext, mt: MagnitudeTable, candidates: List[SiderealTarget]): AgsStrategy.Estimate = {
    // If we are unbounded and there are any candidates, we are guaranteed success.
    val pac   = ctx.getPosAngleConstraint(UNBOUNDED)
    val cv    = new CandidateValidator(params, mt, candidates)
    val steps = pac.steps(ctx.getPositionAngle, params.stepSize.toOldModel).toList.asScala
    val anglesWithResults  = steps.filter { angle => cv.exists(ctx.withPositionAngle(angle)) }
    val successProbability = anglesWithResults.size.toDouble / steps.size.toDouble
    AgsStrategy.Estimate.toEstimate(successProbability)
  }

  override def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[AgsStrategy.Selection]] =
    catalogResult(ctx, mt).map(select(ctx, mt, _))

  def select(ctx: ObsContext, mt: MagnitudeTable, candidates: List[SiderealTarget]): Option[AgsStrategy.Selection] = {
    if (candidates.size == 0) None
    else {
      val results = ctx.getPosAngleConstraint match {
        case FIXED                         => selectBounded(List(ctx), mt, candidates)
        case FIXED_180 | PARALLACTIC_ANGLE => selectBounded(List(ctx, ctx180(ctx)), mt, candidates)
        case UNBOUNDED                     => selectUnbounded(ctx, mt, candidates)
      }
      brightest(results, params.band)(_._2).map {
        case (angle, st) => AgsStrategy.Selection(angle, List(AgsStrategy.Assignment(params.guideProbe, st)))
      }
    }
  }

  // List of candidates and their angles for the case where the pos angle constraint is not unbounded.
  private def selectBounded(alternatives: List[ObsContext], mt: MagnitudeTable, candidates: List[SiderealTarget]): List[(Angle, SiderealTarget)] = {
    val cv = new CandidateValidator(params, mt, candidates)
    alternatives.map(a => (a, cv.select(a))).collect {
      case (c, Some(st)) => (Angle.fromDegrees(c.getPositionAngle.toDegrees.getMagnitude), st)
    }
  }

  // List of candidates and their angles for the case where the pos angle constraint is bounded.
  private def selectUnbounded(ctx: ObsContext, mt: MagnitudeTable, candidates: List[SiderealTarget]): List[(Angle, SiderealTarget)] =
    candidates.map(so => (SingleProbeStrategy.calculatePositionAngle(ctx.getBaseCoordinates.toNewModel, so), so)).filter {
      case (angle, st) => new CandidateValidator(params, mt, List(st)).exists(ctx.withPositionAngle(angle.toOldModel))
    }

  override def queryConstraints(ctx: ObsContext, mt: MagnitudeTable): List[QueryConstraint] =
    params.queryConstraints(ctx, mt).toList

  override val guideProbes: List[GuideProbe] =
    List(params.guideProbe)
}

object SingleProbeStrategy {
  import scala.math._

  /**
   * Calculate the position angle to a target from a specified base position.
   */
  def calculatePositionAngle(base: Coordinates, st: SiderealTarget): Angle = {
    val ra1    = st.coordinates.ra.toAngle.toRadians
    val dec1   = st.coordinates.dec.toAngle.toRadians
    val target = HmsDegTarget.fromSkyObject(st.toOldModel)
    val ra2    = Angle.fromDegrees(target.getC1.getAs(Units.DEGREES)).toRadians
    val dec2   = Angle.fromDegrees(target.getC2.getAs(Units.DEGREES)).toRadians
    val raDiff = ra2 - ra1
    val angle  = atan2(sin(raDiff), cos(dec1) * tan(dec2) - sin(dec1) * cos(raDiff))
    Angle.fromRadians(angle)
  }
}