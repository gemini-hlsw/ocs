package edu.gemini.itc.gmos

import edu.gemini.itc.base.PeakPixelLimitRule

/** GMOS specific warning rule. */
final case class AdLimitRule(limit: Double, warnAtFraction: Double) extends PeakPixelLimitRule {
  def message(value: Double) = f"Peak pixel count is ${percentOfLimit(value)}%.0f%% of the A-D limit of $limit%.0f e-."
}
