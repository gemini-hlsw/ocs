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
  // For Java usage
  val instance = this

  def lookup(key: AgsStrategyKey): Option[AgsStrategy] = Strategy.fromKey(key)

  def lookupForJava(key: AgsStrategyKey): edu.gemini.shared.util.immutable.Option[AgsStrategy] =
    lookup(key).asGeminiOpt

  /**
   * Determines the default or best strategy for the given observation.
   */
  def defaultStrategy(ctx: ObsContext): Option[AgsStrategy] =
    validStrategies(ctx).headOption

  /**
   * Obtains the user's preference, if any, to override the default strategy
   * for the observation.  This may not be a valid strategy for the observation.
   */
  def strategyOverride(ctx: ObsContext): Option[AgsStrategy] =
    ctx.getAgsStrategyOverride.asScalaOpt.flatMap(lookup)

  /**
   * Obtains the valid preferred guiding strategy to use for this observation.
   * Uses the explicit "override" choice if set and valid, or else the default
   * strategy.
   */
  def currentStrategy(ctx: ObsContext): Option[AgsStrategy] =
    strategyOverride(ctx).filter(validStrategies(ctx).contains) orElse defaultStrategy(ctx)

  def currentStrategyForJava(ctx: ObsContext): edu.gemini.shared.util.immutable.Option[AgsStrategy] =
    currentStrategy(ctx).asGeminiOpt

  def validStrategies(ctx: ObsContext): List[AgsStrategy] =
    Strategy.validStrategies(ctx)

  def validStrategiesAsJava(ctx: ObsContext): java.util.List[AgsStrategy] =
    validStrategies(ctx).asJava

  /**
   * Determines which strategies assign stars to the given guide probe.
   */
  def strategiesFor(guideProbe: GuideProbe, ctx: ObsContext): List[AgsStrategy] =
    validStrategies(ctx).filter(_.guideProbes.contains(guideProbe))
}
