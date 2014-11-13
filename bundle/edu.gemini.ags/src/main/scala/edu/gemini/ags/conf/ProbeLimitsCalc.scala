package edu.gemini.ags.conf

import edu.gemini.ags.api.AgsMagnitude.MagnitudeCalc
import edu.gemini.catalog.api.MagnitudeLimits
import edu.gemini.catalog.api.MagnitudeLimits.{SaturationLimit, FaintnessLimit}
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.guide.GuideSpeed

/**
 * Calculates magnitude limits given a faintness table and an adjustment to
 * apply for saturation, adjusting for CC worse than 50.
 */
case class ProbeLimitsCalc(band: Band, saturationAdjustment: Double, faintnessTable: FaintnessMap) extends MagnitudeCalc {
  def apply(c: Conditions, gs: GuideSpeed): MagnitudeLimits = {
    val faint  = faintnessTable(FaintnessKey(c.iq, c.sb, gs))
    val bright = faint - saturationAdjustment

    // TODO: cleanup to not have to go through a new Magnitude instance :-/
    def ccAdj(v: Double): Double = c.cc.adjust(new Magnitude(band, v)).getBrightness

    new MagnitudeLimits(band, new FaintnessLimit(ccAdj(faint)), new SaturationLimit(ccAdj(bright)))
  }
}