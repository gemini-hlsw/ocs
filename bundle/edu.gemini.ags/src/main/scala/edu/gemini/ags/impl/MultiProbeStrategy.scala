package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.api.AgsStrategy.Estimate
import edu.gemini.ags.api.{AgsAnalysis, AgsMagnitude, AgsStrategy}
import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.{RBandsList, SiderealTarget}
import edu.gemini.spModel.guide.{GuideProbe, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext

import scala.concurrent.Future

import scalaz._
import Scalaz._


/**
 * Represents an AGS strategy consisting of multiple substrategies.
 *
 * Currently, this is only used to represent combinations of SingleProbeStrategies.
 */
case class MultiProbeStrategy(key: AgsStrategyKey, strategies: NonEmptyList[AgsStrategy]) extends AgsStrategy {

  override def magnitudes(ctx: ObsContext, mt: MagnitudeTable): List[(GuideProbe, AgsMagnitude.MagnitudeCalc)] =
    strategies.toList.flatMap(_.magnitudes(ctx, mt))

  override def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] =
    strategies.toList.flatMap(_.analyze(ctx, mt))

  override def analyze(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] =
    AgsAnalysis.analysis(ctx, mt, guideProbe, guideStar, probeBands)

  override def catalogQueries(ctx: ObsContext, mt: MagnitudeTable): List[CatalogQuery] =
    strategies.toList.flatMap(_.catalogQueries(ctx, mt))

  override def candidates(ctx: ObsContext, mt: MagnitudeTable): Future[List[(GuideProbe, List[SiderealTarget])]] =
    Future.traverse(strategies.toList)(_.candidates(ctx, mt)).map(_.flatten)

  override def estimate(ctx: ObsContext, mt: MagnitudeTable): Future[AgsStrategy.Estimate] =
    Future.fold(strategies.toList.map(_.estimate(ctx, mt)))(1.0)((p,est) => p * est.probability).map(Estimate(_))

  // As described in the comment for the class, we pick the position angle for the FIRST strategy, since it is the
  // only one that can have a patrol field that is not (at least approximately) symmetric under rotation around the base.
  override def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[AgsStrategy.Selection]] = {
    Future.fold(strategies.toList.map(_.select(ctx, mt)))(None: Option[AgsStrategy.Selection])((resOpt,curOpt) =>
      curOpt.map(cur => AgsStrategy.Selection(cur.posAngle, resOpt.fold(cur.assignments)(cur.assignments ++ _.assignments))).orElse(resOpt)
    )
  }

  // From the left, take the band that is the superset of the most bands.
  // This should but may not contain all bands, as BandsList is a sealed trait.
  override lazy val probeBands = strategies.toList.map(_.probeBands).reduceLeft((sup,cur) => {
    if (sup.bands.toSet.subsetOf(cur.bands.toSet)) cur else sup
  })

  override lazy val guideProbes: List[GuideProbe] =
    strategies.toList.flatMap(_.guideProbes).distinct
}

case object GmosNorthOiwfsAltair extends MultiProbeStrategy(AgsStrategyKey.GmosNorthAltairOiwfsKey, NonEmptyList(Strategy.GmosNorthOiwfs, Strategy.AltairAowfs))
case object GmosNorthOiwfsPwfs1 extends MultiProbeStrategy(AgsStrategyKey.GmosNorthPwfs1OiwfsKey, NonEmptyList(Strategy.GmosNorthOiwfs, Strategy.Pwfs1North))