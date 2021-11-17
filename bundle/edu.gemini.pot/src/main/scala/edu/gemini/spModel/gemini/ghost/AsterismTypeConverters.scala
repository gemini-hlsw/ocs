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
  val asterismTypeConverters: ((ResolutionMode, AsterismType, ResolutionMode)) => Option[AsterismType] = Map(
      (GhostStandard, GhostSingleTarget,  GhostHigh) -> GhostHighResolutionTargetPlusSky,
      (GhostStandard, GhostSingleTarget,  GhostPRV)  -> GhostHighResolutionTargetPlusSky,
      (GhostStandard, GhostDualTarget,    GhostHigh) -> GhostHighResolutionTargetPlusSky,
      (GhostStandard, GhostDualTarget,    GhostPRV)  -> GhostHighResolutionTargetPlusSky,
      (GhostStandard, GhostTargetPlusSky, GhostHigh) -> GhostHighResolutionTargetPlusSky,
      (GhostStandard, GhostTargetPlusSky, GhostPRV)  -> GhostHighResolutionTargetPlusSky,
      (GhostStandard, GhostSkyPlusTarget, GhostHigh) -> GhostHighResolutionTargetPlusSky,
      (GhostStandard, GhostSkyPlusTarget, GhostPRV)  -> GhostHighResolutionTargetPlusSky,

      (GhostHigh, GhostHighResolutionTargetPlusSky, GhostStandard) -> GhostTargetPlusSky,
      (GhostHigh, GhostHighResolutionTargetPlusSky, GhostPRV)      -> GhostHighResolutionTargetPlusSky,

      (GhostPRV, GhostHighResolutionTargetPlusSky, GhostStandard) -> GhostSingleTarget,
      (GhostPRV, GhostHighResolutionTargetPlusSky, GhostHigh)     -> GhostHighResolutionTargetPlusSky
  ).lift
}
