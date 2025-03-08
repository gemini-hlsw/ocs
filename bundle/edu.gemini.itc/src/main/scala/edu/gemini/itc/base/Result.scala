package edu.gemini.itc.base

import edu.gemini.itc.operation._
import edu.gemini.itc.shared.{AllIntegrationTimes, ItcParameters, SignalToNoiseAt}

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
  val source = parameters.source
  val observation = parameters.observation
  val telescope = parameters.telescope
  val conditions = parameters.conditions
}

/* Internal object for imaging results. */
final case class ImagingResult(
                                parameters: ItcParameters,
                                instrument: Instrument,
                                iqCalc: ImageQualityCalculatable,
                                sfCalc: SourceFraction,
                                peakPixelCount: Double,
                                is2nCalc: ImagingS2NCalculatable,
                                aoSystem: Option[AOSystem],
                                times: Option[AllIntegrationTimes] // TODO make this not an Option once we do all instruments
) extends Result {
  def withTimes(times: AllIntegrationTimes): ImagingResult =
    copy(times = Some(times))
}

object ImagingResult {

  def apply(
      parameters: ItcParameters,
      instrument: Instrument,
      iqCalc: ImageQualityCalculatable,
      sfCalc: SourceFraction,
      peakPixelCount: Double,
      IS2Ncalc: ImagingS2NCalculatable
  ) =
    new ImagingResult(
      parameters,
      instrument,
      iqCalc,
      sfCalc,
      peakPixelCount,
      IS2Ncalc,
      None,
      None
    )

  def apply(
      parameters: ItcParameters,
      instrument: Instrument,
      IQcalc: ImageQualityCalculatable,
      SFcalc: SourceFraction,
      peakPixelCount: Double,
      IS2Ncalc: ImagingS2NCalculatable,
      aoSystem: AOSystem
  ) =
    new ImagingResult(
      parameters,
      instrument,
      IQcalc,
      SFcalc,
      peakPixelCount,
      IS2Ncalc,
      Some(aoSystem),
      None
    )

}

/* Internal object for generic spectroscopy results. */
final case class SpectroscopyResult(
                                     parameters: ItcParameters,
                                     instrument: Instrument,
                                     iqCalc: ImageQualityCalculatable,
                                     specS2N: Array[SpecS2N], // Array is used for IFU cases (GMOS and NIFS)
                                     slit: Slit,
                                     slitThrougput: Double,
                                     aoSystem: Option[AOSystem],
                                     signalToNoiseAt: Option[SignalToNoiseAt],
                                     times: AllIntegrationTimes
) extends Result {
  def withTimes(times: AllIntegrationTimes): SpectroscopyResult =
    copy(times = times)
  def peakPixelCount: Double = specS2N.map(_.getPeakPixelCount).max
}
