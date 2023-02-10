package edu.gemini.spModel.gemini.ghost

import edu.gemini.spModel.target.env.{AsterismType, ResolutionMode}
import edu.gemini.spModel.target.env.AsterismType._
import edu.gemini.spModel.target.env.ResolutionMode._

object AsterismTypeConverters {

  /**
   * Called when the desired resolution mode changes, yields the new asterism
   * type to use.
   */
  val asterismTypeConverters: ResolutionMode => Option[AsterismType] =
    Map(
      GhostStandard -> GhostTargetPlusSky,  // HR modes that we're changing from are all Target + Sky
      GhostHigh     -> GhostHighResolutionTargetPlusSky,
      GhostPRV      -> GhostHighResolutionTargetPlusSkyPrv
    ).lift
}
