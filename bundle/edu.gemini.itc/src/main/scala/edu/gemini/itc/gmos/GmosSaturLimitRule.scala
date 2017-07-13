package edu.gemini.itc.gmos

import edu.gemini.itc.base.{LimitRule, Result}

/** GMOS specific warning rule. */
final case class GmosSaturLimitRule(adLimit: Double, wellDepth: Double, xBin: Int, yBin: Int,
                                    gain: Double, warnAtFraction: Double) extends LimitRule {
  def value(r: Result): Double = r.peakPixelCount

  def wellSaturLimit: Double = wellDepth * xBin * yBin
  def adcSaturLimit: Double = adLimit * gain

  /** For GMOS saturation limit is determined either by the full well limit or by the ADC limit.
    * Therefor for the "saturation" limit we use the smallest of the two values.
    */
  def limit: Double = wellSaturLimit.min(adcSaturLimit)

  def saturElem: String = if (limit == wellSaturLimit) "full well depth" else "A-D"

  def message(value: Double) = f"Peak pixel count is ${percentOfLimit(value)}%.0f%% of the $saturElem limit of $limit%.0f e-."

}
