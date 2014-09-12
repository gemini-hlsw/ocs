package edu.gemini.ags.gems.mascot

import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray
import edu.gemini.skycalc.Coordinates


class OdgwGuideStar extends GuideStarType {
  def defaultBandpass: String = "H"

  def filter(ctx: ObsContext, magLimits: MagLimits, star: Star): Boolean = {
    val coords = new Coordinates(star.ra, star.dec)
    val idOpt = GsaoiDetectorArray.instance.getId(coords, ctx)
    !(idOpt.isEmpty || (magLimits != null && !magLimits.filter(star)))
  }
}

