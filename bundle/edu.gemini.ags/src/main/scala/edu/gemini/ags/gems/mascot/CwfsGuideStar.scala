package edu.gemini.ags.gems.mascot

import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.gemini.gems.Canopus
import edu.gemini.skycalc.Coordinates
import edu.gemini.ags.impl._

/**
 * Defines the default bandpass and filter for CWFS
 */
class CwfsGuideStar extends GuideStarType {
  override def defaultBandpass = MagnitudeBand.R

  def filter(ctx: ObsContext, magLimits: MagLimits, star: Star): Boolean = {
    val coordinates = new Coordinates(star.target.coordinates.ra.toAngle.toOldModel, star.target.coordinates.dec.toAngle.toOldModel)
    !(Canopus.instance.getProbesInRange(coordinates, ctx).isEmpty || (magLimits != null && !magLimits.filter(star)))
  }
}
