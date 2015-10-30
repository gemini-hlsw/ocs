package edu.gemini.itc.base

import java.util

import edu.gemini.itc.shared._

import scala.collection.JavaConversions._

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

  // === Imaging

  def toCcdData(r: ImagingResult): ItcCcd =
    ItcCcd(r.is2nCalc.singleSNRatio(), r.is2nCalc.totalSNRatio(), r.peakPixelCount, r.instrument.wellDepth, r.instrument.gain, Warning.collectWarnings(r))

  def serviceResult(r: ImagingResult): ItcImagingResult =
    ItcImagingResult(List(toCcdData(r)))

  def serviceResult(r: Array[ImagingResult]): ItcImagingResult =
    ItcImagingResult(r.map(toCcdData).toList)

  // === Spectroscopy

  def toCcdData(r: SpectroscopyResult, charts: List[SpcChartData]): ItcCcd = {
    val s2nChart: SpcChartData = charts.find(_.chartType == S2NChart).get
    val singleSNRatioVals: List[Double] = s2nChart.allSeries(SingleS2NData).map(_.yValues.max)
    val singleSNRatio: Double           = if (singleSNRatioVals.isEmpty) 0 else singleSNRatioVals.max
    val totalSNRatioVals: List[Double]  = s2nChart.allSeries(FinalS2NData).map(_.yValues.max)
    val totalSNRatio: Double            = if (totalSNRatioVals.isEmpty) 0 else totalSNRatioVals.max
    ItcCcd(singleSNRatio, totalSNRatio, r.peakPixelCount, r.instrument.wellDepth, r.instrument.gain, Warning.collectWarnings(r))
  }

  def serviceResult(r: SpectroscopyResult, charts: java.util.List[SpcChartData]): ItcSpectroscopyResult =
    ItcSpectroscopyResult(List(toCcdData(r, charts.toList)), charts.toList)

  def serviceResult(rs: Array[SpectroscopyResult], charts: java.util.List[SpcChartData]): ItcSpectroscopyResult =
    ItcSpectroscopyResult(rs.map(r => toCcdData(r, charts.toList)).toList, charts.toList)

}

