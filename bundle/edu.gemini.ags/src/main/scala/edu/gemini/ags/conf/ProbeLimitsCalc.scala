package edu.gemini.ags.conf

import edu.gemini.ags.api.AgsMagnitude.MagnitudeCalc
import edu.gemini.catalog.api.{SaturationConstraint, FaintnessConstraint, MagnitudeConstraints}
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.guide.GuideSpeed

/**
 * Calculates magnitude limits given a faintness table and an adjustment to
 * apply for saturation, adjusting for CC worse than 50.
 */
case class ProbeLimitsCalc(band: MagnitudeBand, saturationAdjustment: Double, faintnessTable: FaintnessMap) extends MagnitudeCalc {
  def apply(c: Conditions, gs: GuideSpeed): MagnitudeConstraints = {
    val faint  = faintnessTable(FaintnessKey(c.iq, c.sb, gs))
    val bright = faint - saturationAdjustment

    def ccAdj(v: Double): Double = v + c.cc.magAdjustment
    MagnitudeConstraints(band, FaintnessConstraint(ccAdj(faint)), Some(SaturationConstraint(ccAdj(bright))))
  }
}