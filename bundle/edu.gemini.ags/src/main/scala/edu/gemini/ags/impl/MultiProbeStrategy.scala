package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.api.AgsStrategy.Estimate
import edu.gemini.ags.api.{AgsAnalysis, AgsGuideQuality, AgsMagnitude, AgsStrategy}
import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.skycalc.Angle
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.guide.{GuideProbe, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.telescope.{PosAngleConstraint, PosAngleConstraintAware}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import Scalaz._


/**
 * Represents an AGS strategy consisting of multiple substrategies.
 *
 * Currently, this is only used to represent combinations of SingleProbeStrategies.
 */
class MultiProbeStrategy(val key: AgsStrategyKey, val strategies: List[AgsStrategy]) extends AgsStrategy {

  override def magnitudes(ctx: ObsContext, mt: MagnitudeTable): List[(GuideProbe, AgsMagnitude.MagnitudeCalc)] =
    strategies.flatMap(_.magnitudes(ctx, mt))

  override def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] =
    strategies.flatMap(_.analyze(ctx, mt))

  override def analyze(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] =
    AgsAnalysis.analysis(ctx, mt, guideProbe, guideStar, probeBands)

  override def catalogQueries(ctx: ObsContext, mt: MagnitudeTable): List[CatalogQuery] =
    strategies.flatMap(_.catalogQueries(ctx, mt))

  override def candidates(ctx: ObsContext, mt: MagnitudeTable): Future[List[(GuideProbe, List[SiderealTarget])]] =
    Future.traverse(strategies)(_.candidates(ctx, mt)).map(_.flatten)

  override def estimate(ctx: ObsContext, mt: MagnitudeTable): Future[AgsStrategy.Estimate] =
    Future.fold(strategies.map(_.estimate(ctx, mt)))(1.0)((p,est) => p * est.probability).map(Estimate(_))

  // This is a bit hacked, as it requires invoking select for each of the substrategies, which are structured to
  // pick the best assignment for the current position angle constraint. This, of course, might result in different
  // position angles for substrategies if we naively call select on each, so instead, we hack the context to
  // handle each position angle constraint and then pick the best one.
  override def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[AgsStrategy.Selection]] = {
    // We hack the obs context to be a list of obs contexts with all possible position angles, and with fixed
    // position angle constraint so that guide probes will not be allowed to take on different angles in select calls.
    def ctxToFixedList: List[ObsContext] = {
      val ctxFixed = {
        val instFixed = ctx.getInstrument match {
          case pac : PosAngleConstraintAware =>
            val newPac = pac.clone.asInstanceOf[SPInstObsComp with PosAngleConstraintAware]
            newPac.setPosAngleConstraint(PosAngleConstraint.FIXED)
            newPac
          case oth : SPInstObsComp => oth
        }
        ctx.withInstrument(instFixed)
      }
      (ctx.getPosAngleConstraint match {
        case PosAngleConstraint.FIXED                                            => List(0)
        case PosAngleConstraint.FIXED_180 | PosAngleConstraint.PARALLACTIC_ANGLE => List(0, 180)
        case PosAngleConstraint.UNBOUNDED                                        => Range(0, 360, 15).toList
      }).map(a => ctxFixed.withPositionAngle(ctx.getPositionAngle.add(a, Angle.Unit.DEGREES)))
    }

    // We have a List[ObsContext], and we want to get a Future[Option[AgsStrategy.Selection]]
    // We can take the List[ObsContext] to a Future[List[Option[AgsStrategy.Selection]]].
    // Then we simply filter out the empty Options, and of the remaining, fold to get the best selection.
    // We MAY want to make a comparator for AgsStrategy.Selection. I'm just not sure the best way to do this.
    // Assign values to GuideQualities, add together, pick the maximum, and then break ties by vignetting?
    val noStrategy: Option[AgsStrategy.Selection] = None
    def ctxSelect(cCtx: ObsContext): Future[(ObsContext, Option[AgsStrategy.Selection])] = {
      val lOfF = strategies.map(_.select(cCtx, mt).map(opt => (ctx, opt)))
      Future.fold(lOfF)(ctx -> noStrategy) { case ((_, resOpt), (_, curOpt)) =>
        cCtx -> curOpt.map(cur => AgsStrategy.Selection(cur.posAngle, resOpt.fold(cur.assignments)(cur.assignments ++ _.assignments))).orElse(resOpt)
      }
    }

    // Now we convert to a future of lists of (ObsContext, Option[AgsStrategy.Selection]), and pick the best one,
    // if one exists. This entails picking the best quality, followed by vignetting, followed by magnitude.
    // To calculate this for multiple selections:
    // 1. Quality index is the quality converted to an int, and then summed over all targets. Lowest wins.
    // 2. Vignetting index is vignetting for each vignetting guide probe, summed across all. Lowest wins.
    // 3. Best quality across all targets. Used because for magnitude, we only want best quality class in comparison.
    // 4. Magnitude is magnitude of the brightest star in the assignment in the best quality class.
    def selValue(cCtx: ObsContext, sel: AgsStrategy.Selection): (Int, Double, Int, Double) = {
      val qualitiesByTarget = sel.assignments.collect {
        case AgsStrategy.Assignment(gp: ValidatableGuideProbe, t) => t -> analyze(cCtx, mt, gp, t)
      }.collect {
        case (t, Some(v)) => t -> AgsGuideQuality.All.indexOf(v.quality)
      }.toMap
      val (bestQuality, qualityIdx) = {
        val qualities = qualitiesByTarget.values
        (qualities.min, qualities.sum)
      }

      val vignettingIdx = sel.assignments.collect {
        case AgsStrategy.Assignment(gp: SingleProbeStrategy.VProbe,t) => gp.calculator(cCtx).calc(t.coordinates)
      }.sum

      // We can use default value of 0.0 as the guide probes for which targets with magnitudes exist should be
      // consistent across all selections.
      val brightestMag = sel.assignments.collect {
        case AgsStrategy.Assignment(_, t) if qualitiesByTarget.get(t).exists(_ === bestQuality) =>
          probeBands.extract(t).map(_.value)
      }.collect {
        case Some(v) => v
      }.minimum.getOrElse(0.0)

      (qualityIdx, vignettingIdx, bestQuality, brightestMag)
    }

    // Turn into a Future of List[(ObsContext, Option[AgsStrategy.Selection]), collect the results that have
    // a defined selection while calculating their value, and then return the one (if one exists) of minimum value.
    Future.sequence(ctxToFixedList.map(ctxSelect))
      .map(_.collect { case (cCtx, Some(sel)) => (cCtx, sel, selValue(cCtx, sel)) }.minimumBy(_._3).map(_._2))
  }


  // From the left, take the band that is the superset of the most bands.
  // This should but may not contain all bands, as BandsList is a sealed trait.
  override lazy val probeBands = strategies.map(_.probeBands).reduceLeft((sup,cur) => {
    if (sup.bands.toSet.subsetOf(cur.bands.toSet)) cur else sup
  })

  override lazy val guideProbes: List[GuideProbe] =
    strategies.flatMap(_.guideProbes).distinct
}

case object GmosNorthOiwfsAltair extends MultiProbeStrategy(AgsStrategyKey.GmosNorthAltairOiwfsKey, List(Strategy.GmosNorthOiwfs, Strategy.AltairAowfs))
case object GmosNorthOiwfsPwfs1 extends MultiProbeStrategy(AgsStrategyKey.GmosNorthPwfs1OiwfsKey, List(Strategy.GmosNorthOiwfs, Strategy.Pwfs1North))