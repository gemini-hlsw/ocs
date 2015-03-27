package edu.gemini.ags.impl

import edu.gemini.ags.api._
import edu.gemini.ags.api.AgsMagnitude._
import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.catalog.votable.{RemoteBackend, VoTableBackend, CatalogException, VoTableClient}
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.{Magnitude, MagnitudeBand, Coordinates, Angle}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.guide.{ValidatableGuideProbe, VignettingGuideProbe, GuideProbe}
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
case class SingleProbeStrategy(key: AgsStrategyKey, params: SingleProbeStrategyParams, backend: VoTableBackend = RemoteBackend) extends AgsStrategy {
  import SingleProbeStrategy._

  override def magnitudes(ctx: ObsContext, mt: MagnitudeTable): List[(GuideProbe, AgsMagnitude.MagnitudeCalc)] =
    params.magnitudeCalc(ctx, mt).toList.map(params.guideProbe -> _)

  def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] =
    AgsAnalysis.analysis(ctx, mt, params.guideProbe, probeBands).toList

  private def catalogQueries(ctx: ObsContext, mt: MagnitudeTable): Option[CatalogQuery] =
    params.catalogQueries(ctx, mt)

  override def candidates(ctx: ObsContext, mt: MagnitudeTable): Future[List[(GuideProbe, List[SiderealTarget])]] = {
    val empty = Future.successful(List((params.guideProbe: GuideProbe, List.empty[SiderealTarget])))
    def filterOnMagnitude(q: CatalogQuery, t: SiderealTarget): Boolean = {
      params.referenceMagnitude(t).exists(q.filterOnMagnitude(t, _))
    }

    catalogQueries(ctx, mt).strengthR(backend).map(Function.tupled(VoTableClient.catalog)).map(_.flatMap {
        case r if r.result.containsError => Future.failed(CatalogException(r.result.problems))
        case r                           => Future.successful(List((params.guideProbe, r.result.targets.rows.filter(t => filterOnMagnitude(r.query, t)))))
    }).getOrElse(empty)
  }

  private def catalogResult(ctx: ObsContext, mt: MagnitudeTable): Future[List[SiderealTarget]] = {
    // call candidates and extract the one and only tuple for this strategy,
    // throw away the guide probe (which we know anyway), and obtain just the
    // list of guide stars
    candidates(ctx, mt).map { lst =>
      lst.headOption.foldMap(_._2)
    }
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
      params.guideProbe match {
        // If vignetting, filter according to the pos angle constraint, and then for each obs context, pick the best quality with
        // the least vignetting. Then pick the best quality with the least vignetting of the final result.
        case v: ValidatableGuideProbe with VignettingGuideProbe =>
          val results = ctx.getPosAngleConstraint match {
            case FIXED                         => filterBounded(List(ctx), mt, candidates)
            case FIXED_180 | PARALLACTIC_ANGLE => filterBounded(List(ctx, ctx180(ctx)), mt, candidates)
            case UNBOUNDED                     => filterUnbounded(ctx, mt, candidates)
          }
          val bestPerCtx = for {
            (c, soList) <- results
            rating      <- brightestByQualityAndVignetting(soList, mt, c, v, params)
          } yield (c, rating)
          bestPerCtx.reduceOption(vignettingCtxOrder.min).map {
            case (c, (_, _, _, st)) => AgsStrategy.Selection(c.getPositionAngle.toNewModel, List(AgsStrategy.Assignment(params.guideProbe, st)))
          }

        // Otherwise proceed as normal.
        case _ =>
          val results = ctx.getPosAngleConstraint match {
            case FIXED                         => selectBounded(List(ctx), mt, candidates)
            case FIXED_180 | PARALLACTIC_ANGLE => selectBounded(List(ctx, ctx180(ctx)), mt, candidates)
            case UNBOUNDED                     => selectUnbounded(ctx, mt, candidates)
          }
          brightest(results, params)(_._2).map {
            case (angle, st) => AgsStrategy.Selection(angle, List(AgsStrategy.Assignment(params.guideProbe, st)))
          }
      }
    }
  }

  private def filterBounded(alternatives: List[ObsContext], mt: MagnitudeTable, candidates: List[SiderealTarget]): List[(ObsContext, List[SiderealTarget])] = {
    val cv = new CandidateValidator(params, mt, candidates)
    alternatives.map(c => (c, cv.filter(c))).filter {
      case (c, cand) => cand.nonEmpty
    }
  }

  private def filterUnbounded(ctx: ObsContext, mt: MagnitudeTable, candidates: List[SiderealTarget]): List[(ObsContext, List[SiderealTarget])] = {
    for {
      so <- candidates
      pa = SingleProbeStrategy.calculatePositionAngle(ctx.getBaseCoordinates.toNewModel, so)
      ctxSo = ctx.withPositionAngle(pa.toOldModel)
      if new CandidateValidator(params, mt, List(so)).exists(ctxSo)
    } yield (ctxSo, List(so))
  }

  // List of candidates and their angles for the case where the pos angle constraint is not unbounded.
  private def selectBounded(alternatives: List[ObsContext], mt: MagnitudeTable, candidates: List[SiderealTarget]): List[(Angle, SiderealTarget)] = {
    val cv = new CandidateValidator(params, mt, candidates)
    alternatives.map(a => (a, cv.select(a))).collect {
      case (c, Some(st)) => (Angle.fromDegrees(c.getPositionAngle.toDegrees.getMagnitude), st)
    }
  }

  // List of candidates and their angles for the case where the pos angle constraint is unbounded.
  private def selectUnbounded(ctx: ObsContext, mt: MagnitudeTable, candidates: List[SiderealTarget]): List[(Angle, SiderealTarget)] =
    candidates.map(so => (SingleProbeStrategy.calculatePositionAngle(ctx.getBaseCoordinates.toNewModel, so), so)).filter {
      case (angle, st) => new CandidateValidator(params, mt, List(st)).exists(ctx.withPositionAngle(angle.toOldModel))
    }

  override val guideProbes: List[GuideProbe] =
    List(params.guideProbe)

  override val probeBands: List[MagnitudeBand] = params.probeBands
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
    val ra2    = Angle.fromDegrees(target.getRa.getAs(Units.DEGREES)).toRadians
    val dec2   = Angle.fromDegrees(target.getDec.getAs(Units.DEGREES)).toRadians
    val raDiff = ra2 - ra1
    val angle  = atan2(sin(raDiff), cos(dec1) * tan(dec2) - sin(dec1) * cos(raDiff))
    Angle.fromRadians(angle)
  }

  lazy val vignettingOrder = implicitly[scala.math.Ordering[(Int, Double, Option[Magnitude])]].on { x: (AgsGuideQuality, Double, Option[Magnitude], SiderealTarget) =>
    (AgsGuideQuality.All.indexOf(x._1), x._2, x._3)
  }

  lazy val vignettingCtxOrder = vignettingOrder.on { x0: (ObsContext, (AgsGuideQuality, Double, Option[Magnitude], SiderealTarget)) => x0._2 }

  /**
   * Given a list of candidates, bin them by quality and then pick the one that vignettes the
   * science area the least and is the brightest.
   * @param lst              the list of possible candidates
   * @param mt               magnitude lookup table
   * @param ctx              context information
   * @param probe            the guide probe
   * @return                 Some((quality, vignetting factor, guideStar) if a candidate exists that can be used, None otherwise
   */
  def brightestByQualityAndVignetting(lst: List[SiderealTarget], mt: MagnitudeTable, ctx: ObsContext,
                                      probe: ValidatableGuideProbe with VignettingGuideProbe,
                                      params: SingleProbeStrategyParams): Option[(AgsGuideQuality, Double, Option[Magnitude], SiderealTarget)] = {
    // Create tuples (AgsQuality, vignetting factor, target) and then find the min by
    // ordering on the first two, returning the corresponding target.
    val candidates = for {
      st <- lst
      spTarget = new SPTarget(HmsDegTarget.fromSkyObject(st.toOldModel))
      analysis <- AgsAnalysis.analysis(ctx, mt, probe, spTarget, params.probeBands)
    } yield {
      val vig = probe.calculateVignetting(ctx, st.coordinates)
      (analysis.quality, vig, params.referenceMagnitude(st), st)
    }
    candidates.reduceOption(vignettingOrder.min)
  }
}
