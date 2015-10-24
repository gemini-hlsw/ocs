package edu.gemini.itc.base

import java.util

import edu.gemini.itc.operation.{ImagingPointS2NMethodBCalculation, ImagingS2NMethodACalculation}
import edu.gemini.itc.shared._

import scala.collection.JavaConversions._

import scalaz._
import Scalaz._

sealed trait Recipe

trait ImagingRecipe extends Recipe {
  def calculateImaging(): ImagingResult
  def serviceResult(r: ImagingResult): ItcImagingResult
}

trait ImagingArrayRecipe extends Recipe {
  def calculateImaging(): Array[ImagingResult]
  def serviceResult(r: Array[ImagingResult]): ItcImagingResult
}


trait SpectroscopyRecipe extends Recipe {
  def calculateSpectroscopy(): SpectroscopyResult
  def serviceResult(r: SpectroscopyResult): ItcSpectroscopyResult
}


trait SpectroscopyArrayRecipe extends Recipe {
  def calculateSpectroscopy(): Array[SpectroscopyResult]
  def serviceResult(r: Array[SpectroscopyResult]): ItcSpectroscopyResult

}

object Recipe {

  // =============
  // GENERIC CHART CREATION
  // Utility functions that create generic signal and signal to noise charts for several instruments.

  def createSignalChart(result: SpectroscopyResult): SpcChartData = {
    createSignalChart(result, 0)
  }

  def createSignalChart(result: SpectroscopyResult, index: Int): SpcChartData = {
    createSignalChart(result, "Signal and Background ", index)
  }

  def createSigSwAppChart(result: SpectroscopyResult, index: Int): SpcChartData = {
    createSignalChart(result, "Signal and SQRT(Background) in software aperture of " + result.specS2N(index).getSpecNpix + " pixels", index)
  }

  def createSignalChart(result: SpectroscopyResult, title: String, index: Int): SpcChartData = {
    val data: java.util.List[SpcSeriesData] = new java.util.ArrayList[SpcSeriesData]()
    data.add(new SpcSeriesData(SignalData,     "Signal ",            result.specS2N(index).getSignalSpectrum.getData))
    data.add(new SpcSeriesData(BackgroundData, "SQRT(Background)  ", result.specS2N(index).getBackgroundSpectrum.getData))
    new SpcChartData(SignalChart, title, "Wavelength (nm)", "e- per exposure per spectral pixel", data.toList)
  }

  def createS2NChart(result: SpectroscopyResult): SpcChartData = {
    createS2NChart(result, 0)
  }

  def createS2NChart(result: SpectroscopyResult, index: Int): SpcChartData = {
    createS2NChart(result, "Intermediate Single Exp and Final S/N", index)
  }

  def createS2NChart(result: SpectroscopyResult, title: String, index: Int): SpcChartData = {
    val data: java.util.List[SpcSeriesData] = new util.ArrayList[SpcSeriesData]
    data.add(new SpcSeriesData(SingleS2NData, "Single Exp S/N", result.specS2N(index).getExpS2NSpectrum.getData))
    data.add(new SpcSeriesData(FinalS2NData,  "Final S/N  ",    result.specS2N(index).getFinalS2NSpectrum.getData))
    new SpcChartData(S2NChart, title, "Wavelength (nm)", "Signal / Noise per spectral pixel", data.toList)
  }


  // warnings

  def collectGenericWarnings(r: Result): List[ItcWarning] = {
      r.instrument.warnings().flatMap(_.warning(r)).toList
  }

  def collectWarnings(r: ImagingResult): List[ItcWarning] = {
    collectGenericWarnings(r) ++ r.instrument.imagingWarnings(r).toList
  }

  def collectWarnings(r: SpectroscopyResult): List[ItcWarning] = {
    collectGenericWarnings(r) ++ r.instrument.spectroscopyWarnings(r).toList
  }

  // Helper

  def toImgData(result: ImagingResult): ImgData = result.is2nCalc match {
    case i: ImagingS2NMethodACalculation       => ImgData(i.singleSNRatio(), i.totalSNRatio(), result.peakPixelCount, collectWarnings(result))
    case i: ImagingPointS2NMethodBCalculation  => ImgData(0.0 /* TODO value not known for this mode */, i.effectiveS2N(), result.peakPixelCount, collectWarnings(result))
    case _                                     => throw new NotImplementedError("unknown s2n calc method")
  }

  def serviceResult(r: ImagingResult): ItcImagingResult = ItcImagingResult(List(toImgData(r)))

  def serviceResult(r: Array[ImagingResult]): ItcImagingResult = ItcImagingResult(r.map(toImgData).toList)
}

