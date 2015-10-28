package edu.gemini.itc.base

import edu.gemini.itc.operation._
import edu.gemini.itc.shared.{ItcParameters, ItcWarning}

/*
 * Helper objects that are used to pass around detailed results of imaging and spectroscopy calculations internally.
 * These objects also contain the parameter objects that were used for the calculations so that input values
 * can become a part of the output after the calculations have been performed, e.g. when the input parameters
 * and results need to be printed to a web page. The ITC service will not return these objects but simplified
 * result objects which helps to contain ITC internals however these detailed results are used by the web
 * application to produce detailed HTML output.
 */

sealed trait Result {
  def parameters: ItcParameters
  def instrument: Instrument
  def peakPixelCount: Double

  // Accessors for convenience.
  val source      = parameters.source
  val observation = parameters.observation
  val telescope   = parameters.telescope
  val conditions  = parameters.conditions
}

/* Warning limits */
sealed trait WarningLimit {
  def message(value: Double): String
  def maxValue: Double
  def lowLimit: Double    // as a fraction of the max value; e.g. 0.80 = 80%

  def warnLevel = maxValue * lowLimit
  def warn(value: Double): Boolean = value >= warnLevel
  def value(r: Result): Double = r.peakPixelCount // currently peak pixel count is the only value we're looking at
  def warning(r: Result) = if (warn(value(r))) Some(ItcWarning(message(value(r)))) else None
}

final case class SaturationLimit(maxValue: Double, lowLimit: Double) extends WarningLimit {
  def message(value: Double) = f"Peak pixel count of $value%.0f exceeds ${lowLimit*100.0}%.0f%% of the well depth limit of $maxValue%.0f and may be saturated."
}
final case class GainLimit(maxValue: Double, lowLimit: Double) extends WarningLimit {
  def message(value: Double) = f"Peak pixel count of $value%.0f exceeds ${lowLimit*100.0}%.0f%% of the gain limit of $maxValue%.0f and may be saturated."
}
final case class LinearityLimit(maxValue: Double, lowLimit: Double) extends WarningLimit {
  def message(value: Double) = f"Peak pixel count of $value%.0f exceeds ${lowLimit*100.0}%.0f%% of the linearity limit of $maxValue%.0f and will cause deviations."
}

/* Internal object for imaging results. */
final case class ImagingResult(
                      parameters:       ItcParameters,
                      instrument:       Instrument,
                      iqCalc:           ImageQualityCalculatable,
                      sfCalc:           SourceFraction,
                      peakPixelCount:   Double,
                      is2nCalc:         ImagingS2NCalculatable,
                      aoSystem:         Option[AOSystem]) extends Result

object ImagingResult {

  def apply(parameters: ItcParameters, instrument: Instrument, iqCalc: ImageQualityCalculatable, sfCalc: SourceFraction, peakPixelCount: Double, IS2Ncalc: ImagingS2NCalculatable) =
    new ImagingResult(parameters, instrument, iqCalc, sfCalc, peakPixelCount, IS2Ncalc, None)

  def apply(parameters: ItcParameters, instrument: Instrument, IQcalc: ImageQualityCalculatable, SFcalc: SourceFraction, peakPixelCount: Double, IS2Ncalc: ImagingS2NCalculatable, aoSystem: AOSystem) =
    new ImagingResult(parameters, instrument, IQcalc, SFcalc, peakPixelCount, IS2Ncalc, Some(aoSystem))

}

sealed trait SpectroscopyResult extends Result {
  val sfCalc: SourceFraction
  val iqCalc: ImageQualityCalculatable
  val specS2N: Array[SpecS2N]
  val st: SlitThroughput
  val aoSystem: Option[AOSystem]
  lazy val peakPixelCount: Double = specS2N.map(_.getPeakPixelCount).max
}

/* Internal object for generic spectroscopy results (all instruments except for GNIRS). */
final case class GenericSpectroscopyResult(
                      parameters:       ItcParameters,
                      instrument:       Instrument,
                      sfCalc:           SourceFraction,
                      iqCalc:           ImageQualityCalculatable,
                      specS2N:          Array[SpecS2N],
                      st:               SlitThroughput,
                      aoSystem:         Option[AOSystem]) extends SpectroscopyResult

/* Internal object for GNIRS spectroscopy results.
 * I somehow think it should be possible to unify this in a clever way with the "generic" spectroscopy result
 * used for the other instruments, but right now I don't understand the calculations well enough to figure out
 * how to do that in a meaningful way. */
final case class GnirsSpectroscopyResult(
                      parameters:       ItcParameters,
                      instrument:       Instrument,
                      sfCalc:           SourceFraction,
                      iqCalc:           ImageQualityCalculatable,
                      specS2N:          Array[SpecS2N],
                      st:               SlitThroughput,
                      aoSystem:         Option[AOSystem],
                      signalOrder:      Array[VisitableSampledSpectrum],
                      backGroundOrder:  Array[VisitableSampledSpectrum],
                      finalS2NOrder:    Array[VisitableSampledSpectrum]) extends SpectroscopyResult

object SpectroscopyResult {

  def apply(parameters: ItcParameters, instrument: Instrument, sfCalc: SourceFraction, iqCalc: ImageQualityCalculatable, specS2N: Array[SpecS2N], st: SlitThroughput) =
    new GenericSpectroscopyResult(parameters, instrument, sfCalc, iqCalc, specS2N, st, None)

}

