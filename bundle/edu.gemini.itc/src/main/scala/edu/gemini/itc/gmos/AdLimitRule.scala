package edu.gemini.itc.gmos

import edu.gemini.itc.base.{Result, LimitRule}

/** GMOS specific warning rule. */
final case class AdLimitRule(limit: Double, warnAtFraction: Double) extends LimitRule {
  def value(r: Result): Double = r.peakPixelCount / r.instrument.gain()
  def message(value: Double) = f"Peak pixel count is ${percentOfLimit(value)}%.0f%% of the A-D limit of $limit%.0f ADU."
}
