package edu.gemini.itc.gmos

import edu.gemini.itc.base.{ImagingResult, SpectroscopyResult, Result, LimitRule}

/** GMOS specific warning rule. */
final case class AdLimitRule(limit: Double, warnAtFraction: Double) extends LimitRule {
// TODO: REL-2576: Reactivate peak pixel warnings for spectroscopy for March 2016 release
//  def value(r: Result): Double = r.peakPixelCount / r.instrument.gain()
  def value(r: Result): Double = r match {
    case _: SpectroscopyResult => 0.0               // do not issue warnings for spectroscopy for now
    case _: ImagingResult      => r.peakPixelCount / r.instrument.gain()
  }

  def message(value: Double) = f"Peak pixel count is ${percentOfLimit(value)}%.0f%% of the A-D limit of $limit%.0f ADU."
}
