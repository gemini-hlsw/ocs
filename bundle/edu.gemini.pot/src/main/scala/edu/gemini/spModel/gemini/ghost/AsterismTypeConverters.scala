package edu.gemini.spModel.gemini.ghost

import edu.gemini.spModel.target.env.{AsterismType, ResolutionMode}

object AsterismTypeConverters {
  /**
   * Given:
   * 1. An initial ResolutionMode RM1 amd an AsterismType AT1
   * 2. A desired ResolutionMode RM2
   * Find the AsterismType that best matches.
   * This is in place so that when the resolution mode is changed, the target data is maintained.
   */
  val asterismTypeConverters: Map[(ResolutionMode, AsterismType],
}
