package edu.gemini.ags.gems.mascot

import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.gemini.gsaoi.{GsaoiDetectorArray, GsaoiOdgw}
import edu.gemini.skycalc.Coordinates

object OdgwGuideStar extends GuideStarType {
  override def filter(ctx: ObsContext, magLimits: MagLimits, star: Star): Boolean = {
    val coords = new Coordinates(star.target.coordinates.ra.toAngle.toDegrees, star.target.coordinates.dec.toAngle.toDegrees)
    val idOpt = GsaoiDetectorArray.instance.getId(coords, ctx)
    idOpt.isDefined && magLimits.filter(star)
  }

  override val guideGroup = GsaoiOdgw.Group.instance
}

