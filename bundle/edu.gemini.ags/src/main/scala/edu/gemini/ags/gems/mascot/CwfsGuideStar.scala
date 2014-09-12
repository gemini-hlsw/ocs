package edu.gemini.ags.gems.mascot

import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.gemini.gems.Canopus
import edu.gemini.skycalc.Coordinates

/**
 * Defines the default bandpass and filter for CWFS
 */
class CwfsGuideStar extends GuideStarType {
  def defaultBandpass: String = "R"

  def filter(ctx: ObsContext, magLimits: MagLimits, star: Star): Boolean = {
    val coords = new Coordinates(star.ra, star.dec)
    !(Canopus.instance.getProbesInRange(coords, ctx).isEmpty || (magLimits != null && !magLimits.filter(star)))
  }
}
