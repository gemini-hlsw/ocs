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

  def noExposureTime: Option[TotalExposure] = None
  def noAOSystem: Option[AOSystem] = None

  // =============
  // GENERIC CHART CREATION
  // Utility functions that create generic signal and signal to noise charts for several instruments.

  // create signal/background chart
  def createSignalChart(result: SpectroscopyResult, index: Int): SpcChartData = {
    createSignalChart(result, "Signal & SQRT(Background) in one pixel", index)
  }

  def createSignalChart(result: SpectroscopyResult, title: String, index: Int): SpcChartData = {
     createSignalChart (result.specS2N(index).getSignalSpectrum, "Signal", result.specS2N(index).getBackgroundSpectrum, "SQRT(Background)",title);
  }


  def createSignalChart(signal1: VisitableSampledSpectrum, dataLegend1: String,
                  signal2: VisitableSampledSpectrum, dataLegend2: String,
                  titleGraph : String): SpcChartData = {
    val data: JList[SpcSeriesData] = new ArrayList[SpcSeriesData]()
    data.add(SpcSeriesData(SignalData,     dataLegend1, signal1.getData, Some(ITCChart.DarkBlue)))
    data.add(SpcSeriesData(BackgroundData, dataLegend2, signal2.getData, Some(ITCChart.LightBlue)))
    new SpcChartData(SignalChart, titleGraph, ChartAxis("Wavelength (nm)"), ChartAxis("e- per exposure per spectral pixel"), data.toList)
  }

  def createS2NChart(result: SpectroscopyResult): SpcChartData = {
    createS2NChart(result, 0)
  }

  def createS2NChart(result: SpectroscopyResult, index: Int): SpcChartData = {
    createS2NChart(result, "Intermediate Single Exp and Final S/N in aperture", index)
  }

  def createS2NChart(result: SpectroscopyResult, title: String, index: Int): SpcChartData = {
     return createS2NChart(result.specS2N(index).getExpS2NSpectrum,result.specS2N(index).getFinalS2NSpectrum, "Single Exp S/N", "Final S/N  ", title)
  }

  def createS2NChart(expS2NSpectrum: VisitableSampledSpectrum, finalS2NSpectrum: VisitableSampledSpectrum,
                     legendExpS2N: String, legendS2N : String,
                     title: String): SpcChartData = {
    val data: JList[SpcSeriesData] = new ArrayList[SpcSeriesData]
    data.add(SpcSeriesData(SingleS2NData, legendExpS2N, expS2NSpectrum.getData))
    data.add(SpcSeriesData(FinalS2NData,  legendS2N,    finalS2NSpectrum.getData))
    new SpcChartData(S2NChart, title, ChartAxis("Wavelength (nm)"), ChartAxis("Signal / Noise per spectral pixel"), data.toList)
  }


  def createS2NChartPerRes(singleS2N: VisitableSampledSpectrum, finalS2N: VisitableSampledSpectrum, title: String, color1: Color, color2: Color): SpcChartData = {
    val data: JList[SpcSeriesData] = new ArrayList[SpcSeriesData]
    data.add(SpcSeriesData(SingleS2NPerResEle, "Single Exp S/N", singleS2N.getData, Option(color1)))
    data.add(SpcSeriesData(FinalS2NPerResEle,  "Final S/N  ",    finalS2N.getData, Option(color2)))
    new SpcChartData(S2NChartPerRes, title, ChartAxis("Wavelength (nm)"), ChartAxis("Signal / Noise per resolution element"), data.toList)
  }

  // === Imaging

  def toCcdData(r: ImagingResult): ItcCcd =
    ItcCcd(r.is2nCalc.singleSNRatio(), r.is2nCalc.totalSNRatio(), r.peakPixelCount, r.instrument.wellDepth, r.instrument.gain, Warning.collectWarnings(r))

  def serviceResult(r: ImagingResult): ItcImagingResult =
    ItcImagingResult(List(toCcdData(r)), r.exposureCalculations)

  def serviceResult(r: Array[ImagingResult]): ItcImagingResult = {
    val ccds = r.map(toCcdData).toList
    ItcImagingResult(ccds, r.map(_.exposureCalculations).toList.headOption.flatten) // FIXME: Review how many result are we gettingg
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
  def serviceResult(r: SpectroscopyResult, charts: JList[SpcChartData], headless: Boolean): ItcSpectroscopyResult = {
    ItcSpectroscopyResult(
      List(toCcdData(r, charts.toList)),
      if (headless) Nil else List(SpcChartGroup(charts.toList)),
      r.exposureCalculations,
      r.signalToNoiseAt
    )
  }

  // One result (CCD) and a set of groups of charts, this covers NIFS (1 CCD and separate groups for IFU cases).
  def serviceGroupedResult(r: SpectroscopyResult, charts: JList[JList[SpcChartData]], headless: Boolean): ItcSpectroscopyResult = {
    ItcSpectroscopyResult(
      List(toCcdData(r, charts.toList.flatten)),
      if (headless) Nil else charts.toList.map(l => SpcChartGroup(l.toList)),
      r.exposureCalculations,
      r.signalToNoiseAt
    )
  }

  // A set of results and a set of groups of charts, this covers GMOS (3 CCDs and potentially separate groups
  // for IFU cases, if IFU is activated).
  def serviceGroupedResult(rs: Array[SpectroscopyResult], charts: JList[JList[SpcChartData]], headless: Boolean): ItcSpectroscopyResult = {
    val snAtArray = rs.flatMap(_.signalToNoiseAt)
    // if all the snAt ar empty it means we are out of range, so return none
    // else filter out the ones containing 0 and return the first. If none is over 0 just return the first.
    val snAt = snAtArray.find(_.finalSignalToNoise > 0).orElse(snAtArray.headOption)
    ItcSpectroscopyResult(
      rs.map(r => toCcdData(r, charts.toList.flatten)).toList,
      if (headless) Nil else charts.toList.map(l => SpcChartGroup(l.toList)),
      rs.flatMap(_.exposureCalculations).headOption, // At least for gmos we return the same exposure calculation for each ccd.
      snAt
    )

  }

}
object RecipeUtil {
  val instance = this

  // We want to find sn at a specific wavelength.
  // if the wavelength is out of range return None, other wise return the value even if sn is 0
  def signalToNoiseAt(wavelengthAt: Double, signal: Spectrum, total: Spectrum): Option[SignalToNoiseAt] = {
    // Both spectrums need the same range
    require(signal.getStart == total.getStart)
    require(signal.getEnd == total.getEnd)
    val result = if (wavelengthAt >= signal.getStart && wavelengthAt <= signal.getEnd) {
      // 0 is a valid sn
      Option(SignalToNoiseAt(wavelengthAt, signal.getY(wavelengthAt), total.getY(wavelengthAt)))
    } else None
    result
  }
}
