package edu.gemini.itc.ghost

import edu.gemini.itc.base.PeakPixelLimitRule

/** GHOST saturation warning rule, applied separately to each arm.
  *
  * Each GHOST detector saturates at its A-D converter limit, given in ADU. The peak pixel count
  * is in electrons, so the limit is converted with the arm's gain.
  *
  * @param adcLimit       A-D saturation limit of the detector, in ADU
  * @param gain           gain of the detector, in e-/ADU
  * @param warnAtFraction fraction of the limit at which to warn (1.0 = only once saturated)
  */
final case class GhostSaturationLimitRule(adcLimit: Int, gain: Double, warnAtFraction: Double) extends PeakPixelLimitRule {

  def limit: Double = adcLimit * gain

  def message(value: Double): String =
    f"Peak pixel count is ${percentOfLimit(value)}%.0f%% of the A-D saturation limit of $limit%.0f e- ($adcLimit%d ADU)."

}
