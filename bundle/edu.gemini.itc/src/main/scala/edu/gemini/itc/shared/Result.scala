package edu.gemini.itc.shared

import edu.gemini.itc.operation._

/*
 * Helper objects that are used to pass around results of imaging and spectroscopy calculations.
 * These objects also contain the parameter objects that were used for the calculations so that input values
 * can become a part of the output after the calculations have been performed, e.g. when the input parameters
 * and results need to be printed to a web page. Note that the ITC service will not return these objects but
 * simplified result objects which helps to contain ITC internals.
 */

/* Internal object for imaging results. */
final case class ImagingResult(
                      parameters: Parameters,
                      instrument: Instrument,
                      iqCalc: ImageQualityCalculatable,
                      sfCalc: SourceFraction,
                      peakPixelCount: Double,
                      is2nCalc: ImagingS2NCalculatable,
                      aoSystem: Option[AOSystem])

object ImagingResult {

  def apply(parameters: Parameters, instrument: Instrument, iqCalc: ImageQualityCalculatable, sfCalc: SourceFraction, peakPixelCount: Double, IS2Ncalc: ImagingS2NCalculatable) =
    new ImagingResult(parameters, instrument, iqCalc, sfCalc, peakPixelCount, IS2Ncalc, None)

  def apply(parameters: Parameters, instrument: Instrument, IQcalc: ImageQualityCalculatable, SFcalc: SourceFraction, peakPixelCount: Double, IS2Ncalc: ImagingS2NCalculatable, aoSystem: AOSystem) =
    new ImagingResult(parameters, instrument, IQcalc, SFcalc, peakPixelCount, IS2Ncalc, Some(aoSystem))

}

/* Internal object for spectroscopy results. */
final case class SpectroscopyResult(
                       sfCalc: SourceFraction,
                       iqCalc: ImageQualityCalculatable,
                       specS2N: Array[SpecS2N],
                       st: SlitThroughput,
                       aoSystem: Option[AOSystem])

object SpectroscopyResult {

  def apply(sfCalc: SourceFraction, iqCalc: ImageQualityCalculatable, specS2N: Array[SpecS2N], st: SlitThroughput) =
    new SpectroscopyResult(sfCalc, iqCalc, specS2N, st, None)

}
