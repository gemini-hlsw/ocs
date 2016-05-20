package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.ags.api.{AgsAnalysis, AgsStrategy}
import edu.gemini.ags.api.AgsStrategy.{Estimate, Selection}
import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.{BandsList, SiderealTarget}
import edu.gemini.spModel.guide.{GuideProbe, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext

import scala.concurrent.Future

/**
  * Placeholder for AGS strategies that are not yet implemented, but that we must allow to guide with.
  */
case class UnimplementedStrategy(key: AgsStrategyKey) extends AgsStrategy {
  override def magnitudes(ctx: ObsContext, mt: MagnitudeTable): List[(GuideProbe, MagnitudeCalc)] = ???

  /**
    * Indicates the bands that will be used for a given probe
    */
  override def probeBands: BandsList = ???

  override def estimate(ctx: ObsContext, mt: MagnitudeTable): Future[Estimate] = ???

  override def candidates(ctx: ObsContext, mt: MagnitudeTable): Future[List[(GuideProbe, List[SiderealTarget])]] = ???

  override def guideProbes: List[GuideProbe] = ???

  override def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] = ???

  override def analyze(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] = ???

  override def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[Selection]] = ???

  /**
    * Returns a list of catalog queries that would be used to search for guide stars with the given context
    */
  override def catalogQueries(ctx: ObsContext, mt: MagnitudeTable): List[CatalogQuery] = ???
}

object UnimplementedStrategy {
  val unimplementedKeys = List(AgsStrategyKey.GmosNorthAltairOiwfsKey, AgsStrategyKey.GmosNorthPwfs1OiwfsKey)
}