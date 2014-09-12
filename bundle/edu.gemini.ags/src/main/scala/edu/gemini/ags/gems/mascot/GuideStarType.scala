package edu.gemini.ags.gems.mascot

import edu.gemini.spModel.obs.context.ObsContext

/**
 * Common guide star properties
 */
trait GuideStarType {
  /**The default bandpass to use for calculations */
  def defaultBandpass: String

  /**
   * Returns true if the star is a valid guide star.
   * @param ctx the science program observing context
   * @param magLimits optional limits for each magnitude
   * @param star describes the star's position and magnitudes
   */
  def filter(ctx: ObsContext, magLimits: MagLimits, star: Star): Boolean
}

