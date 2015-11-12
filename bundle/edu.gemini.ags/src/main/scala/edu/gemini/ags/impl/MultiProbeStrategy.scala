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
 * Currently, this is only used to represent combinations of probes where at most one probe has a patrol field
 * that is not symmetric with regards to rotations about the base position.
 *
 * Since select returns a position angle, this allows us to use the position angle of the paStrategy and ignore
 * the position angles of the (fully or approximately) rotation-symmetric (around the base pos) strategies returned
 * by select.
 */
sealed abstract class MultiProbeStrategy(val key: AgsStrategyKey, paStrategy: AgsStrategy, paSymmetricStrategies: List[AgsStrategy]) extends AgsStrategy {
  val strategies = paStrategy :: paSymmetricStrategies

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

  // As described in the comment for the class, we pick the position angle for the FIRST strategy, since it is the
  // only one that can have a patrol field that is not (at least approximately) symmetric under rotation around the base.
  override def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[AgsStrategy.Selection]] = {
    Future.fold(strategies.map(_.select(ctx, mt)))(None: Option[AgsStrategy.Selection])((resOpt,curOpt) =>
      curOpt.map(cur => AgsStrategy.Selection(cur.posAngle, resOpt.fold(cur.assignments)(cur.assignments ++ _.assignments))).orElse(resOpt)
    )
  }

  override val guideProbes: List[GuideProbe] =
    strategies.flatMap(_.guideProbes)
}

case object GmosNorthOiwfsAltair extends MultiProbeStrategy(AgsStrategyKey.GmosNorthAltairOiwfsKey, Strategy.GmosNorthOiwfs, List(Strategy.AltairAowfs)) {
  override val probeBands = RBandsList
}

case object GmosNorthOiwfsPwfs1 extends MultiProbeStrategy(AgsStrategyKey.GmosNorthPwfs1OiwfsKey, Strategy.GmosNorthOiwfs, List(Strategy.Pwfs1North)) {
  override val probeBands = RBandsList
}