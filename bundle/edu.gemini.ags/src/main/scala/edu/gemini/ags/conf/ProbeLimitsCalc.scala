package edu.gemini.ags.conf

import edu.gemini.ags.api.AgsMagnitude.MagnitudeCalc
import edu.gemini.ags.api.agsBandExtractor
import edu.gemini.catalog.api._
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.guide.GuideSpeed

import scalaz._
import Scalaz._

/**
 * Calculates magnitude limits given a faintness table and an adjustment to
 * apply for saturation, adjusting for CC worse than 50.
 */
case class ProbeLimitsCalc(band: MagnitudeBand, saturationAdjustment: Double, faintnessTable: FaintnessMap) extends MagnitudeCalc {
  def apply(c: Conditions, gs: GuideSpeed): MagnitudeConstraints = {
    val faint  = faintnessTable(FaintnessKey(c.iq, c.sb, gs))
    val bright = faint - saturationAdjustment

    // Single probe reads adjustments from the table and only adjust for CC
    c.cc.adjust(MagnitudeConstraints(band, agsBandExtractor(band), FaintnessConstraint(faint), Some(SaturationConstraint(bright))))
  }
}