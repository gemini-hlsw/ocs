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

  /**
   * Determines the default or best strategy for the given observation.
   */
  def defaultStrategy(ctx: ObsContext): Option[AgsStrategy] = {
    // Figure out which guide probes we're using, preferring those we mark as
    // selected (i.e., with a primary target)
    val guideEnv   = ctx.getTargets.getGuideEnvironment
    val ref0       = guideEnv.getPrimaryReferencedGuiders
    val referenced = (if (ref0.isEmpty) guideEnv.getReferencedGuiders else ref0).asScala

    // Determine how many referenced guiders in the context are assigned by the
    // given strategy.
    def incidence(s: AgsStrategy): Int = (referenced & s.guideProbes.toSet).size

    // Get the first strategy with the highest overlap between referenced
    // guiders and guiders assigned by the strategy.  It's important to preserve
    // order in this search because the valid strategies are returned in ranked
    // order of preference.
    AgsRegistrar.validStrategies(ctx) match {
      case Nil    =>
        None
      case h :: t =>
        val (highestIncidence, _) = ((h, incidence(h))/:t) { case ((s0, i0), s1) =>
          val i1 = incidence(s1)
          if (i0 >= i1) (s0, i0) else (s1, i1)
        }
        Some(highestIncidence)
    }
  }

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
