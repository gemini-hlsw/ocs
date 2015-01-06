package edu.gemini.ags.conf

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{SkyBackground, ImageQuality}
import edu.gemini.spModel.guide.GuideSpeed

sealed case class MagLimitsId(name: String)

case class FaintnessKey(iq: ImageQuality, sb: SkyBackground, gs: GuideSpeed) {
  override def toString: String = s"($iq, $sb, $gs)"
}
