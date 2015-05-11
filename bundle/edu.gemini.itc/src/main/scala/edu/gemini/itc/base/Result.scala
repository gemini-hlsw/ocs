package edu.gemini.itc.base

import edu.gemini.itc.operation._
import edu.gemini.itc.shared.Parameters

/*
 * Helper objects that are used to pass around results of imaging and spectroscopy calculations.
 * These objects also contain the parameter objects that were used for the calculations so that input values
 * can become a part of the output after the calculations have been performed, e.g. when the input parameters
 * and results need to be printed to a web page. Note that the ITC service will not return these objects but
 * simplified result objects which helps to contain ITC internals.
 */

sealed trait Result {
  val parameters: Parameters
  val instrument: Instrument

  // Accessors for convenience.
  val source      = parameters.source
  val observation = parameters.observation
  val telescope   = parameters.telescope
  val conditions  = parameters.conditions

}

/* Internal object for imaging results. */
final case class ImagingResult(
                      parameters: Parameters,
                      instrument: Instrument,
                      iqCalc: ImageQualityCalculatable,
                      sfCalc: SourceFraction,
                      peakPixelCount: Double,
                      is2nCalc: ImagingS2NCalculatable,
                      aoSystem: Option[AOSystem]) extends Result

object ImagingResult {

  def apply(parameters: Parameters, instrument: Instrument, iqCalc: ImageQualityCalculatable, sfCalc: SourceFraction, peakPixelCount: Double, IS2Ncalc: ImagingS2NCalculatable) =
    new ImagingResult(parameters, instrument, iqCalc, sfCalc, peakPixelCount, IS2Ncalc, None)

  def apply(parameters: Parameters, instrument: Instrument, IQcalc: ImageQualityCalculatable, SFcalc: SourceFraction, peakPixelCount: Double, IS2Ncalc: ImagingS2NCalculatable, aoSystem: AOSystem) =
    new ImagingResult(parameters, instrument, IQcalc, SFcalc, peakPixelCount, IS2Ncalc, Some(aoSystem))

}

sealed trait SpectroscopyResult extends Result {
  val sfCalc: SourceFraction
  val iqCalc: ImageQualityCalculatable
  val specS2N: Array[SpecS2N]
  val st: SlitThroughput
  val aoSystem: Option[AOSystem]
}

/* Internal object for generic spectroscopy results (all instruments except for GNIRS). */
final case class GenericSpectroscopyResult(
                      parameters: Parameters,
                      instrument: Instrument,
                      sfCalc: SourceFraction,
                      iqCalc: ImageQualityCalculatable,
                      specS2N: Array[SpecS2N],
                      st: SlitThroughput,
                      aoSystem: Option[AOSystem]) extends SpectroscopyResult

/* Internal object for GNIRS spectroscopy results.
 * I somehow think it should be possible to unify this in a clever way with the "generic" spectroscopy result
 * used for the other instruments, but right now I don't understand the calculations well enough to figure out
 * how to do that in a meaningful way. */
final case class GnirsSpectroscopyResult(
                      parameters: Parameters,
                      instrument: Instrument,
                      sfCalc: SourceFraction,
                      iqCalc: ImageQualityCalculatable,
                      specS2N: Array[SpecS2N],
                      st: SlitThroughput,
                      aoSystem: Option[AOSystem],
                      signalOrder: Array[VisitableSampledSpectrum],
                      backGroundOrder: Array[VisitableSampledSpectrum],
                      finalS2NOrder: Array[VisitableSampledSpectrum]) extends SpectroscopyResult

object SpectroscopyResult {

  def instance = this

  def apply(parameters: Parameters, instrument: Instrument, sfCalc: SourceFraction, iqCalc: ImageQualityCalculatable, specS2N: Array[SpecS2N], st: SlitThroughput) =
    new GenericSpectroscopyResult(parameters, instrument, sfCalc, iqCalc, specS2N, st, None)

}

