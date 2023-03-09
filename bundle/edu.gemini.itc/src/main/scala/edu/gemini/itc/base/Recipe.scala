package edu.gemini.itc.base

import java.util.{ArrayList, List => JList}
import edu.gemini.itc.shared._
import edu.gemini.itc.shared.ITCChart

import scala.collection.JavaConversions._

import scalaz._
import Scalaz._

import java.awt.Color

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
  def serviceResult(r: SpectroscopyResult, headless: Boolean): ItcSpectroscopyResult
}


trait SpectroscopyArrayRecipe extends Recipe {
  def calculateSpectroscopy(): Array[SpectroscopyResult]
  def serviceResult(r: Array[SpectroscopyResult], headless: Boolean): ItcSpectroscopyResult

}

object Recipe {

  // =============
  // GENERIC CHART CREATION
  // Utility functions that create generic signal and signal to noise charts for several instruments.

  // create signal/background chart
  def createSignalChart(result: SpectroscopyResult, index: Int): SpcChartData = {
    createSignalChart(result, "Signal & SQRT(Background) in one pixel", index)
  }

  def createSignalChart(result: SpectroscopyResult, title: String, index: Int): SpcChartData = {
    val data: JList[SpcSeriesData] = new ArrayList[SpcSeriesData]()
    data.add(SpcSeriesData(SignalData,     "Signal",           result.specS2N(index).getSignalSpectrum.getData, Some(ITCChart.DarkBlue)))
    data.add(SpcSeriesData(BackgroundData, "SQRT(Background)", result.specS2N(index).getBackgroundSpectrum.getData, Some(ITCChart.LightBlue)))
    new SpcChartData(SignalChart, title, ChartAxis("Wavelength (nm)"), ChartAxis("e- per exposure per spectral pixel"), data.toList)
  }

  def createS2NChart(result: SpectroscopyResult): SpcChartData = {
    createS2NChart(result, 0)
  }

  def createS2NChart(result: SpectroscopyResult, index: Int): SpcChartData = {
    createS2NChart(result, "Intermediate Single Exp and Final S/N in aperture", index)
  }

  def createS2NChart(result: SpectroscopyResult, title: String, index: Int): SpcChartData = {
    val data: JList[SpcSeriesData] = new ArrayList[SpcSeriesData]
    data.add(SpcSeriesData(SingleS2NData, "Single Exp S/N", result.specS2N(index).getExpS2NSpectrum.getData))
    data.add(SpcSeriesData(FinalS2NData,  "Final S/N  ",    result.specS2N(index).getFinalS2NSpectrum.getData))
    new SpcChartData(S2NChart, title, ChartAxis("Wavelength (nm)"), ChartAxis("Signal / Noise per spectral pixel"), data.toList)
  }

  def createS2NChart(singleS2N: VisitableSampledSpectrum, finalS2N: VisitableSampledSpectrum, title: String, color1: Color, color2: Color): SpcChartData = {
    val data: JList[SpcSeriesData] = new ArrayList[SpcSeriesData]
    data.add(SpcSeriesData(SingleS2NPerResEle, "Single Exp S/N", singleS2N.getData, Option(color1)))
    data.add(SpcSeriesData(FinalS2NPerResEle,  "Final S/N  ",    finalS2N.getData, Option(color2)))
    new SpcChartData(S2NChartPerRes, title, ChartAxis("Wavelength (nm)"), ChartAxis("Signal / Noise per spectral pixel"), data.toList)
  }

  // === Imaging

  def toCcdData(r: ImagingResult): ItcCcd =
    ItcCcd(r.is2nCalc.singleSNRatio(), r.is2nCalc.totalSNRatio(), r.peakPixelCount, r.instrument.wellDepth, r.instrument.gain, Warning.collectWarnings(r))

  def serviceResult(r: ImagingResult): ItcImagingResult =
    ItcImagingResult(List(toCcdData(r)))

  def serviceResult(r: Array[ImagingResult]): ItcImagingResult = {
    val ccds = r.map(toCcdData).toList
    ItcImagingResult(ccds)
  }

  // === Spectroscopy

  /** Collects the relevant information from the internal result in a simplified data object and collects some additional
    * information from the data series like e.g. the max signal to noise. For each individual CCD ITC basically
    * does a full analysis and gives us a separate result. */
  def toCcdData(r: SpectroscopyResult, charts: List[SpcChartData]): ItcCcd = {
    val s2nChart: SpcChartData = charts.find(_.chartType == S2NChart).get
    val singleSNRatioVals: List[Double] = s2nChart.allSeries(SingleS2NData).map(_.yValues.max)
    val singleSNRatio: Double           = if (singleSNRatioVals.isEmpty) 0 else singleSNRatioVals.max
    val totalSNRatioVals: List[Double]  = s2nChart.allSeries(FinalS2NData).map(_.yValues.max)
    val totalSNRatio: Double            = if (totalSNRatioVals.isEmpty) 0 else totalSNRatioVals.max
    ItcCcd(singleSNRatio, totalSNRatio, r.peakPixelCount, r.instrument.wellDepth, r.instrument.gain, Warning.collectWarnings(r))
  }

  // === Java helpers

  // One result (CCD) and a simple set of charts, this covers most cases.
  def serviceResult(r: SpectroscopyResult, charts: JList[SpcChartData], headless: Boolean): ItcSpectroscopyResult =
    ItcSpectroscopyResult(
      List(toCcdData(r, charts.toList)),
      if (headless) Nil else List(SpcChartGroup(charts.toList))
    )

  // One result (CCD) and a set of groups of charts, this covers NIFS (1 CCD and separate groups for IFU cases).
  def serviceGroupedResult(r: SpectroscopyResult, charts: JList[JList[SpcChartData]], headless: Boolean): ItcSpectroscopyResult =
    ItcSpectroscopyResult(
      List(toCcdData(r, charts.toList.flatten)),
      if (headless) Nil else charts.toList.map(l => SpcChartGroup(l.toList))
    )

  // A set of results and a set of groups of charts, this covers GMOS (3 CCDs and potentially separate groups
  // for IFU cases, if IFU is activated).
  def serviceGroupedResult(rs: Array[SpectroscopyResult], charts: JList[JList[SpcChartData]], headless: Boolean): ItcSpectroscopyResult = {
    ItcSpectroscopyResult(
      rs.map(r => toCcdData(r, charts.toList.flatten)).toList,
      if (headless) Nil else charts.toList.map(l => SpcChartGroup(l.toList))
    )

  }

}

