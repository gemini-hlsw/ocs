package edu.gemini.ags.api

import edu.gemini.ags.impl.Strategy
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._

import scala.collection.JavaConverters._

/**
 * Methods for finding strategies.
 */
object AgsRegistrar {
  def lookup(key: AgsStrategyKey): Option[AgsStrategy] = Strategy.fromKey(key)

  def defaultStrategy(ctx: ObsContext): Option[AgsStrategy] =
    validStrategies(ctx).headOption

  def selectedStrategy(ctx: ObsContext): Option[AgsStrategy] =
    ctx.getSelectedAgsStrategy.asScalaOpt.flatMap(lookup) orElse defaultStrategy(ctx)

  def validStrategies(ctx: ObsContext): List[AgsStrategy] =
    Strategy.validStrategies(ctx)

  def validStrategiesAsJava(ctx: ObsContext): java.util.List[AgsStrategy] =
    validStrategies(ctx).asJava

  def strategiesFor(guideProbe: GuideProbe, ctx: ObsContext): List[AgsStrategy] =
    validStrategies(ctx).filter(_.guideProbes.contains(guideProbe))
}
