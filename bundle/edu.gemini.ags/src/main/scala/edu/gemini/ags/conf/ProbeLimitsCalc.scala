package edu.gemini.ags.conf

import edu.gemini.ags.api.AgsMagnitude.MagnitudeCalc
import edu.gemini.ags.impl._
import edu.gemini.catalog.api.MagnitudeLimits
import edu.gemini.catalog.api.MagnitudeLimits.{SaturationLimit, FaintnessLimit}
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.guide.GuideSpeed

/**
 * Calculates magnitude limits given a faintness table and an adjustment to
 * apply for saturation, adjusting for CC worse than 50.
 */
case class ProbeLimitsCalc(band: MagnitudeBand, saturationAdjustment: Double, faintnessTable: FaintnessMap) extends MagnitudeCalc {
  def apply(c: Conditions, gs: GuideSpeed): MagnitudeLimits = {
    val faint  = faintnessTable(FaintnessKey(c.iq, c.sb, gs))
    val bright = faint - saturationAdjustment

    def ccAdj(v: Double): Double = v + c.cc.magAdjustment
    new MagnitudeLimits(band.toOldModel, new FaintnessLimit(ccAdj(faint)), new SaturationLimit(ccAdj(bright)))
  }
}