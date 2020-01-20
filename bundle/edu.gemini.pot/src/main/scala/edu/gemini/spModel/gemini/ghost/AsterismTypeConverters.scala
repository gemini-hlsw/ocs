package edu.gemini.spModel.gemini.ghost

import edu.gemini.spModel.target.env.{AsterismType, ResolutionMode}
import edu.gemini.spModel.target.env.AsterismType._
import edu.gemini.spModel.target.env.ResolutionMode._

object AsterismTypeConverters {

  /**
   * Given:
   * 1. An initial ResolutionMode RM1 amd an AsterismType AT1
   * 2. A desired ResolutionMode RM2
   * Find the AsterismType that best matches.
   * This is in place so that when the resolution mode is changed, a conversion can be performed so that the target
   * data is maintained.
   */
  val asterismTypeConverters: Map[(ResolutionMode, AsterismType, ResolutionMode), AsterismType] = Map(
      (GhostStandard, GhostSingleTarget, GhostHigh)  -> GhostHighResolutionTarget,
      (GhostStandard, GhostSingleTarget, GhostPRV)   -> GhostHighResolutionTarget,
      (GhostStandard, GhostDualTarget, GhostHigh)    -> GhostHighResolutionTargetPlusSky,
      (GhostStandard, GhostDualTarget, GhostPRV)     -> GhostHighResolutionTarget,
      (GhostStandard, GhostTargetPlusSky, GhostHigh) -> GhostHighResolutionTargetPlusSky,
      (GhostStandard, GhostTargetPlusSky, GhostPRV)  -> GhostHighResolutionTarget,
      (GhostStandard, GhostSkyPlusTarget, GhostHigh) -> GhostHighResolutionTargetPlusSky,
      (GhostStandard, GhostSkyPlusTarget, GhostPRV)  -> GhostHighResolutionTarget,

      (GhostHigh, GhostHighResolutionTarget, GhostStandard) -> GhostSingleTarget,
      (GhostHigh, GhostHighResolutionTarget, GhostPRV)      -> GhostSingleTarget,
      (GhostHigh, GhostHighResolutionTargetPlusSky, GhostStandard) -> GhostTargetPlusSky,
      (GhostHigh, GhostHighResolutionTargetPlusSky, GhostPRV) -> GhostHighResolutionTarget,

      (GhostPRV, GhostHighResolutionTarget, GhostStandard) -> GhostSingleTarget)
}
