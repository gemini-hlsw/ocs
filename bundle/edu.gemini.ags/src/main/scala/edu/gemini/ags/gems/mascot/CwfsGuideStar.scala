package edu.gemini.ags.gems.mascot

import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.gemini.gems.CanopusWfs
import edu.gemini.skycalc.Coordinates

/**
 * Defines the default bandpass and filter for CWFS
 */
class CwfsGuideStar extends GuideStarType {
  def filter(ctx: ObsContext, magLimits: MagLimits, star: Star): Boolean = {
    val coordinates = new Coordinates(star.target.coordinates.ra.toAngle.toDegrees, star.target.coordinates.dec.toDegrees)
    CanopusWfs.isProbeInRange(coordinates, ctx) && magLimits.filter(star)
  }
}
