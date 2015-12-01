package edu.gemini.itc.base

import edu.gemini.itc.shared.ItcWarning
import scala.collection.JavaConversions._

/**
 * A generic rule that can create a warning message based on the given ITC calculation result.
 */
trait WarningRule {
  def warning(r: Result): Option[ItcWarning]
}

/**
 * Generic rule for creating a warning if a given percentage of a numerical limit is reached.
 */
trait LimitRule extends WarningRule {
  def message(value: Double): String
  def limit: Double
  def warnAtFraction: Double        // e.g. 0.8 = 80%
  def value(r: Result): Double      // extraction method to get value from result

  def warnLevel                     = limit * warnAtFraction
  def warn(value: Double): Boolean  = value >= warnLevel
  def percentOfLimit(value: Double) = (value / limit ) * 100.0
  def warning(r: Result) = {
    val v = value(r)
    if (warn(v)) Some(ItcWarning(message(v))) else None
  }

}

/**
 * Limits for the peak pixel count of the ITC calculation result (imaging & spectroscopy).
 */
trait PeakPixelLimitRule extends LimitRule {
// TODO: REL-2576: Reactivate peak pixel warnings for spectroscopy for March 2016 release
// def value(r: Result): Double = r.peakPixelCount
  def value(r: Result): Double = r match {
    case _: SpectroscopyResult => 0.0               // do not issue warnings for spectroscopy for now
    case _: ImagingResult      => r.peakPixelCount
  }
}

/** Generic rule for warnings if the saturation limit is reached. */
final case class SaturationLimitRule(limit: Double, warnAtFraction: Double) extends PeakPixelLimitRule {
  def message(value: Double) = f"Peak pixel count is ${percentOfLimit(value)}%.0f%% of the well depth limit of $limit%.0f e-."
}

/** Generic rule for warnings if the linearity limit is reached. */
final case class LinearityLimitRule(limit: Double, warnAtFraction: Double) extends PeakPixelLimitRule {
  def message(value: Double) = f"Peak pixel count is ${percentOfLimit(value)}%.0f%% of the linearity limit of $limit%.0f e-."
}


/**
 * Helper object to create warnings for ITC results.
 */
object Warning {

  def collectGenericWarnings(r: Result): List[ItcWarning] = {
    r.instrument.warnings().flatMap(_.warning(r)).toList
  }

  def collectWarnings(r: ImagingResult): List[ItcWarning] = {
    collectGenericWarnings(r) ++ r.instrument.imagingWarnings(r).toList
  }

  def collectWarnings(r: SpectroscopyResult): List[ItcWarning] = {
    collectGenericWarnings(r) ++ r.instrument.spectroscopyWarnings(r).toList
  }

}

