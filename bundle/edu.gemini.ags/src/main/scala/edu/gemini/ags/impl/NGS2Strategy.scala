package edu.gemini.ags.impl

import edu.gemini.ags.api.{AgsAnalysis, AgsMagnitude, AgsStrategy}
import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.ags.AgsStrategyKey.NGS2Key
import edu.gemini.spModel.core.{BandsList, RBandsList, SiderealTarget}
import edu.gemini.spModel.guide.{GuideProbe, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import Strategy.Pwfs1South

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import scalaz._
import Scalaz._

/**
  * The new NGS2 strategy, which requires Canopus guide stars and a PWFS1 guide star.
  */
object NGS2Strategy extends AgsStrategy {
  private val strategies: List[AgsStrategy] = List(GemsStrategy, Pwfs1South)

  override def key: AgsStrategyKey = NGS2Key

  override def magnitudes(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable): List[(GuideProbe, AgsMagnitude.MagnitudeCalc)] =
    GemsStrategy.magnitudes(ctx, mt) ++ Pwfs1South.magnitudes(ctx, mt)

  override def analyze(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable): List[AgsAnalysis] =
    GemsStrategy.analyze(ctx, mt) ++ Pwfs1South.analyze(ctx, mt)

  override def analyze(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] =
    if (Pwfs1South.guideProbes.contains(guideProbe))
      Pwfs1South.analyze(ctx, mt, guideProbe, guideStar)
    else
      GemsStrategy.analyze(ctx, mt, guideProbe, guideStar)

  override def analyzeMagnitude(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] =
    if (Pwfs1South.guideProbes.contains(guideProbe))
      Pwfs1South.analyzeMagnitude(ctx, mt, guideProbe, guideStar)
    else
      GemsStrategy.analyzeMagnitude(ctx, mt, guideProbe, guideStar)

  override def candidates(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable)(ec: ExecutionContext): Future[List[(GuideProbe, List[SiderealTarget])]] =
    for {
      g <- GemsStrategy.candidates(ctx, mt)(ec)
      p <- Pwfs1South.candidates(ctx, mt)(ec)
    } yield g ++ p

  /**
    * Returns a list of catalog queries that would be used to search for guide stars with the given context
    */
  override def catalogQueries(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable): List[CatalogQuery] =
    GemsStrategy.catalogQueries(ctx, mt) ++ Pwfs1South.catalogQueries(ctx, mt)

  override def estimate(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable)(ec: ExecutionContext): Future[AgsStrategy.Estimate] =
    for {
      g <- GemsStrategy.estimate(ctx, mt)(ec)
      p <- Pwfs1South.estimate(ctx, mt)(ec)
    } yield AgsStrategy.Estimate(g.probability * p.probability)

  override def select(ctx: ObsContext, mt: AgsMagnitude.MagnitudeTable)(ec: ExecutionContext): Future[Option[AgsStrategy.Selection]] =
    for {
      gOpt <- GemsStrategy.select(ctx, mt)(ec)
      ctx2 = gOpt.map(r => ctx.withPositionAngle(r.posAngle)).getOrElse(ctx)
      pOpt <- Pwfs1South.select(ctx2, mt)(ec)
    } yield {
      for {
        g <- gOpt
        p <- pOpt
      } yield AgsStrategy.Selection(g.posAngle, g.assignments ++ p.assignments)
    }

  override def guideProbes: List[GuideProbe] =
    GemsStrategy.guideProbes ++ Pwfs1South.guideProbes

  /**
    * Indicates the bands that will be used for a given probe
    */
  override def probeBands: BandsList = RBandsList
}
