package edu.gemini.ags.impl

import edu.gemini.ags.api.{AgsAnalysis, AgsMagnitude, AgsStrategy, ProbeCandidates}
import edu.gemini.catalog.api.{ CatalogName, CatalogQuery }
import edu.gemini.catalog.votable.VoTableBackend
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.ags.AgsStrategyKey.Ngs2Key
import edu.gemini.spModel.core.{BandsList, RBandsList, SiderealTarget}
import edu.gemini.spModel.guide.{GuideProbe, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import scalaz._
import Scalaz._

/**
 * The new NGS2 strategy, which requires Canopus guide stars and a PWFS1 guide
 * star.
 */
final case class Ngs2Strategy(
  catalogName: CatalogName,
  backend:     Option[VoTableBackend]
) extends AgsStrategy {

  override def key: AgsStrategyKey = Ngs2Key

  // Tip-tilt asterism selection
  private val gems = GemsStrategy(catalogName, backend)

  // SFS star selection
  private val pwfs = SingleProbeStrategy(AgsStrategyKey.Pwfs1SouthNgs2Key, Pwfs1NGS2Params(catalogName), backend)

  override def magnitudes(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable): List[(GuideProbe, AgsMagnitude.MagnitudeCalc)] =
    gems.magnitudes(ctx, mt) ++ pwfs.magnitudes(ctx, mt)

  override def analyze(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable): List[AgsAnalysis] =
    gems.analyze(ctx, mt) ++ pwfs.analyze(ctx, mt)

  override def analyze(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] =
    if (pwfs.guideProbes.contains(guideProbe))
      pwfs.analyze(ctx, mt, guideProbe, guideStar)
    else
      gems.analyze(ctx, mt, guideProbe, guideStar)

  override def analyzeMagnitude(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] =
    if (pwfs.guideProbes.contains(guideProbe))
      pwfs.analyzeMagnitude(ctx, mt, guideProbe, guideStar)
    else
      gems.analyzeMagnitude(ctx, mt, guideProbe, guideStar)

  override def candidates(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable)(ec: ExecutionContext): Future[List[ProbeCandidates]] =
    for {
      g <- gems.candidates(ctx, mt)(ec)
      p <- pwfs.candidates(ctx, mt)(ec)
    } yield g ++ p

  /**
    * Returns a list of catalog queries that would be used to search for guide stars with the given context
    */
  override def catalogQueries(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable): List[CatalogQuery] =
    gems.catalogQueries(ctx, mt) ++ pwfs.catalogQueries(ctx, mt)

  override def estimate(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable)(ec: ExecutionContext): Future[AgsStrategy.Estimate] =
    for {
      g <- gems.estimate(ctx, mt)(ec)
      p <- pwfs.estimate(ctx, mt)(ec)
    } yield AgsStrategy.Estimate(g.probability * p.probability)

  override def select(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable)(ec: ExecutionContext): Future[Option[AgsStrategy.Selection]] =
    for {
      gOpt <- gems.select(ctx, mt)(ec)
      ctx2 = gOpt.map(r => ctx.withPositionAngle(r.posAngle)).getOrElse(ctx)
      pOpt <- pwfs.select(ctx2, mt)(ec)
    } yield {
      for {
        g <- gOpt
        p <- pOpt
      } yield AgsStrategy.Selection(g.posAngle, g.assignments ++ p.assignments)
    }

  override def guideProbes: List[GuideProbe] =
    gems.guideProbes ++ pwfs.guideProbes

  /**
    * Indicates the bands that will be used for a given probe
    */
  override def probeBands: BandsList = RBandsList

  /**
    * NGS2 does not have a guide strategy.
    */
  override val hasGuideSpeed: Boolean = false
}
