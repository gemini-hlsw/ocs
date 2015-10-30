package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.api.AgsStrategy.Estimate
import edu.gemini.ags.api.{AgsAnalysis, AgsMagnitude, AgsStrategy}
import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.{SingleBand, RBandsList, SiderealTarget}
import edu.gemini.spModel.guide.{ValidatableGuideProbe, GuideProbe}
import edu.gemini.spModel.obs.context.ObsContext

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Represents an AGS strategy consisting of multiple substrategies.
 *
 * There are several inherent shortcomings with this:
 * 1. select requires a position angle, and querying each substrategy may return a number of position angles.
 *    In the current implementation, the position angle for the first strategy is returned.
 * 2. probeBands must return one of a fixed number of BandsLists. We currently try to find a common one, or one
 *    that is a subset of all of those of the substrategies. If that is not possible, we try to find a single band
 *    that is a subset of those of the substrategies. If that fails, we just return R.
 */
case class MultiProbeStrategy(key: AgsStrategyKey, strategies: List[AgsStrategy]) extends AgsStrategy {
  override def magnitudes(ctx: ObsContext, mt: MagnitudeTable): List[(GuideProbe, AgsMagnitude.MagnitudeCalc)] =
    strategies.flatMap(_.magnitudes(ctx, mt))

  override def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] =
    strategies.flatMap(_.analyze(ctx, mt))

  override def analyze(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] =
    AgsAnalysis.analysis(ctx, mt, guideProbe, guideStar, probeBands)

  override def catalogQueries(ctx: ObsContext, mt: MagnitudeTable): List[CatalogQuery] =
    strategies.flatMap(_.catalogQueries(ctx, mt))

  override def candidates(ctx: ObsContext, mt: MagnitudeTable): Future[List[(GuideProbe, List[SiderealTarget])]] =
    Future.sequence(strategies.map(_.candidates(ctx, mt))).map(_.flatten)

  override def estimate(ctx: ObsContext, mt: MagnitudeTable): Future[AgsStrategy.Estimate] =
    Future.fold(strategies.map(_.estimate(ctx, mt)))(1.0)((p,est) => p * est.probability).map(Estimate(_))

  // We pick the position angle of the first assignment. May not want to do this.
  override def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[AgsStrategy.Selection]] = {
    val selections = Future.sequence(strategies.map(_.select(ctx, mt)))
    Future.fold(strategies.map(_.select(ctx, mt)))(None: Option[AgsStrategy.Selection])((resOpt,curOpt) =>
      curOpt.map(cur => AgsStrategy.Selection(cur.posAngle, resOpt.fold(cur.assignments)(cur.assignments ++ _.assignments))).orElse(resOpt)
    )
  }

  override val guideProbes: List[GuideProbe] =
    strategies.flatMap(_.guideProbes)

  override val probeBands = {
    val bandsLL = strategies.map(_.probeBands)

    // Find a BandsList that is a subset of the others.
    bandsLL.find(bL => {
      val bLSet = Set(bL.bands.list)
      bandsLL.forall(bL2 => bLSet.subsetOf(Set(bL2.bands.list)))
    }).getOrElse {
      // We must have a band, at least, that is a subset of all the bands.
      // Otherwise bork and return R bands list. Not sure what else to do here.
      val bandInt = bandsLL.map(bL => bL.bands.list.toSet).reduce((s1,s2) => s1.intersect(s2))
      bandInt.headOption.map(SingleBand).getOrElse(RBandsList)
    }
  }
}
