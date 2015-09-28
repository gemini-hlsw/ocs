package edu.gemini.ags.impl

import edu.gemini.ags.api._
import edu.gemini.ags.api.AgsMagnitude._
import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.catalog.votable.{RemoteBackend, VoTableBackend, CatalogException, VoTableClient}
import edu.gemini.pot.ModelConverters._
import edu.gemini.skycalc
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.{Coordinates, Angle}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.guide.{ValidatableGuideProbe, VignettingGuideProbe, GuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.system.CoordinateParam.Units
import edu.gemini.spModel.target.system.HmsDegTarget
import edu.gemini.spModel.telescope.PosAngleConstraint._
import edu.gemini.shared.util.immutable.ScalaConverters._

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

  override def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] =
    AgsAnalysis.analysis(ctx, mt, params.guideProbe, probeBands).toList

  override def analyze(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] =
    AgsAnalysis.analysis(ctx, mt, guideProbe, guideStar, probeBands)

  override def catalogQueries(ctx: ObsContext, mt: MagnitudeTable): List[CatalogQuery] =
    params.catalogQueries(ctx, mt).toList

  override def candidates(ctx: ObsContext, mt: MagnitudeTable): Future[List[(GuideProbe, List[SiderealTarget])]] = {
    val empty = Future.successful(List((params.guideProbe: GuideProbe, List.empty[SiderealTarget])))

    // We cannot let VoTableClient to filter targets as usual, instead we provide an empty magnitude constraint and filter locally
    catalogQueries(ctx, mt).strengthR(backend).headOption.map(Function.tupled(VoTableClient.catalog)).map(_.flatMap {
        case r if r.result.containsError => Future.failed(CatalogException(r.result.problems))
        case r                           => Future.successful(List((params.guideProbe, r.result.targets.rows)))
    }).getOrElse(empty)
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
    val cv    = CandidateValidator(params, mt, candidates)
    val steps = pac.steps(ctx.getPositionAngle, params.stepSize.toOldModel).toList.asScala
    val anglesWithResults  = steps.filter { angle => cv.exists(ctx.withPositionAngle(angle)) }
    val successProbability = anglesWithResults.size.toDouble / steps.size.toDouble
    AgsStrategy.Estimate.toEstimate(successProbability)
  }

  override def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[AgsStrategy.Selection]] =
    catalogResult(ctx, mt).map(select(ctx, mt, _))

  def select(ctx: ObsContext, mt: MagnitudeTable, candidates: List[SiderealTarget]): Option[AgsStrategy.Selection] = {

    // Temporary for Andy's tests.
    def feedback(feedback: => String): Unit =
      println(s"AGS $feedback")

    def selectMinVigetting(vprobe: VProbe, allValid: List[(ObsContext, List[SiderealTarget])]): Option[AgsStrategy.Selection] = {

      // Analyze the candidates to get magnitude and quality.
      val analyzed = allValid.map { case (ctx0, targets) =>
        val analyzedTargets = for {
          target    <- targets
          analysis  <- AgsAnalysis.analysis(ctx0, mt, vprobe, target, params.probeBands)
          magnitude <- params.referenceMagnitude(target)
        } yield (target, magnitude, analysis.quality)
        (ctx0, analyzedTargets)
      }

      // Now we don't care about anything but the best quality, even if it
      // vignettes more than a lower quality option.  Figure out what the
      // best quality is.  (Better quality compares LT worse quality.)
      val bestQuality = analyzed.flatMap { case (_, targets) => targets.map(_._3).minimum }.minimum
      feedback { s"Quality of best candidates is '${bestQuality.map(_.shows) | "None"}'" }

      bestQuality.flatMap { quality =>
        // Get vignetting results per context.  Calculates a
        // List[Angle, SiderealTarget, Double] where the Angle is the
        // position angle and the Double is the percent of vignetting.
        val ctxResults = analyzed.flatMap { case (ctx0, targets) =>

          feedback { f"Analyzing pos angle: ${ctx0.getPositionAngle.toDegrees.getMagnitude}%.2f degrees" }

          // Filter out anything but the best quality targets.
          val qualityTargets = targets.filter { case (_,_,q) => q === quality }

          feedback {
            // could have partitioned above but want to be able to just delete
            // the feedback when the time comes
            val msgs = targets.filter { case (_,_,q) => q =/= quality }.map { case (t,_,q) =>
              s"${t.name}, Quality '${q.shows}'"
            }
            if (msgs.isEmpty) "No lower quality targets to delete."
            else "Rejecting lower quality targets: " + msgs.mkString("\n\t", "\n\t", "")
          }

          // Sort the remaining targets (note, compare by "reference magnitude"
          // value since the band itself may be r, R, or UC).
          val sortedTargets  = qualityTargets.sortBy { case (_, mag, _) => mag.value }

          // Calculate min vignetting
          val minVig         = vprobe.calculator(ctx0).minCalc(sortedTargets)(_._1.coordinates) // TODO: when?

          feedback {
            minVig.fold("-> no targets for this position angle") { case ((target, _,_ ), vig) =>
              f"-> winner ${target.name}.  Vignettes ${vig*100}%.2f%%."
            }
          }

          // Finally extract the pos angle, sidereal target, vignetting, and mag
          minVig.map { case ((target,mag,_), vignetting) =>
            (ctx0.getPositionAngle.toNewModel, target, vignetting, mag)
          }
        }

        // Pick the lowest vignetting for all the context options.  If they
        // are tied for lowest vignetting, go with the brightest. (Note default
        // Magnitude sorting considers the band, so compare by .value)
        ctxResults.minimumBy(tup => (tup._3, tup._4.value)).map { case (angle, target, vig, mag) =>
          feedback { f"Selected ${target.name}.  Vignetting ${vig*100}%.2f%%, ${mag.shows}" }
          AgsStrategy.Selection(angle, List(AgsStrategy.Assignment(params.guideProbe, target)))
        }
      }
    }


    if (candidates.isEmpty) None
    else {
      params.guideProbe match {
        // If vignetting, filter according to the pos angle constraint, and then for each obs context, pick the best quality with
        // the least vignetting. Then pick the best quality with the least vignetting of the final result.
        case vprobe: VProbe =>
          // Filter for brightness constraints and reachability.
          val results = ctx.getPosAngleConstraint match {
            case FIXED                         => filterBounded(List(ctx), mt, candidates)
            case FIXED_180 | PARALLACTIC_ANGLE => filterBounded(List(ctx, ctx180(ctx)), mt, candidates)
            case UNBOUNDED                     => filterUnbounded(ctx, mt, candidates)
          }

          // Select the highest quality target that vignettes the least.
          selectMinVigetting(vprobe, results)

        // Otherwise proceed as normal.
        case _ =>
          val results = ctx.getPosAngleConstraint match {
            case FIXED                         => selectBounded(List(ctx), mt, candidates)
            case FIXED_180 | PARALLACTIC_ANGLE => selectBounded(List(ctx, ctx180(ctx)), mt, candidates)
            case UNBOUNDED                     => selectUnbounded(ctx, mt, candidates)
          }
          params.brightest(results)(_._2).map {
            case (angle, st) => AgsStrategy.Selection(angle, List(AgsStrategy.Assignment(params.guideProbe, st)))
          }
      }
    }
  }

  private def filterBounded(alternatives: List[ObsContext], mt: MagnitudeTable, candidates: List[SiderealTarget]): List[(ObsContext, List[SiderealTarget])] = {
    val cv = CandidateValidator(params, mt, candidates)
    alternatives.map(c => (c, cv.filter(c))).filter {
      case (c, cand) => cand.nonEmpty
    }
  }

  private def filterUnbounded(ctx: ObsContext, mt: MagnitudeTable, candidates: List[SiderealTarget]): List[(ObsContext, List[SiderealTarget])] = {
    for {
      base <- ctx.getBaseCoordinates.asScalaOpt.toList
      so <- candidates
      pa = SingleProbeStrategy.calculatePositionAngle(base.toNewModel, so)
      ctxSo = ctx.withPositionAngle(pa.toOldModel)
      if CandidateValidator(params, mt, List(so)).exists(ctxSo)
    } yield (ctxSo, List(so))
  }

  // List of candidates and their angles for the case where the pos angle constraint is not unbounded.
  private def selectBounded(alternatives: List[ObsContext], mt: MagnitudeTable, candidates: List[SiderealTarget]): List[(Angle, SiderealTarget)] = {
    val cv = CandidateValidator(params, mt, candidates)
    alternatives.map(a => (a, cv.select(a))).collect {
      case (c, Some(st)) => (Angle.fromDegrees(c.getPositionAngle.toDegrees.getMagnitude), st)
    }
  }

  // List of candidates and their angles for the case where the pos angle constraint is unbounded.
  private def selectUnbounded(ctx: ObsContext, mt: MagnitudeTable, candidates: List[SiderealTarget]): List[(Angle, SiderealTarget)] = {

    val pairs: List[(Angle, SiderealTarget)] =
      for {
        base <- ctx.getBaseCoordinates.asScalaOpt.toList
        so   <- candidates
      } yield (SingleProbeStrategy.calculatePositionAngle(base.toNewModel, so), so)

    pairs.filter {
      case (angle, st) => CandidateValidator(params, mt, List(st)).exists(ctx.withPositionAngle(angle.toOldModel))
    }

  }

  private def ctx180(c: ObsContext): ObsContext =
    c.withPositionAngle(c.getPositionAngle.add(180.0, skycalc.Angle.Unit.DEGREES))

  override val guideProbes: List[GuideProbe] = List(params.guideProbe)

  override val probeBands = params.probeBands
}

object SingleProbeStrategy {
  import scala.math._

  type VProbe = VignettingGuideProbe with ValidatableGuideProbe

  // TODO: Delete me when we upgrade scalaz
  implicit class MinimumByOp[A](l: List[A]) {
    def minimumBy[B](f: A => B)(implicit cmp: Ordering[B]): Option[A] =
      l match {
        case Nil => None
        case as  => Some(as.minBy(f))
      }
  }

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
}
