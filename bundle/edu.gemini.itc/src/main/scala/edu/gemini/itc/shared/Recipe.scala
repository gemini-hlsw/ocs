package edu.gemini.itc.shared

import java.awt.Color
import java.util

import scala.collection.JavaConversions

sealed trait Recipe

trait ImagingRecipe extends Recipe {
  def calculateImaging(): ImagingResult
}

trait ImagingArrayRecipe extends Recipe {
  def calculateImaging(): Array[ImagingResult]
}


trait SpectroscopyRecipe extends Recipe {
  def calculateSpectroscopy(): (ItcSpectroscopyResult, SpectroscopyResult)
}


trait SpectroscopyArrayRecipe extends Recipe {
  def calculateSpectroscopy(): (ItcSpectroscopyResult, Array[SpectroscopyResult])
}

object Recipe {

  // =============
  // GENERIC CHART CREATION
  // Utility functions that create generic signal and signal to noise charts for several instruments.

  def createSignalChart(result: SpectroscopyResult): SpcDataSet = {
    createSignalChart(result, 0)
  }

  def createSignalChart(result: SpectroscopyResult, index: Int): SpcDataSet = {
    createSignalChart(result, "Signal and Background ", index)
  }

  def createSigSwAppChart(result: SpectroscopyResult, index: Int): SpcDataSet = {
    createSignalChart(result, "Signal and SQRT(Background) in software aperture of " + result.specS2N(index).getSpecNpix + " pixels", index)
  }

  def createSignalChart(result: SpectroscopyResult, title: String, index: Int): SpcDataSet = {
    val data: java.util.List[SpcData] = new java.util.ArrayList[SpcData]()
    data.add(new SpcData("Signal ", Color.RED, result.specS2N(index).getSignalSpectrum.getData))
    data.add(new SpcData("SQRT(Background)  ", Color.BLUE, result.specS2N(index).getBackgroundSpectrum.getData))
    new SpcDataSet("Signal", title, "Wavelength (nm)", "e- per exposure per spectral pixel", JavaConversions.asScalaBuffer(data))
  }

  def createS2NChart(result: SpectroscopyResult): SpcDataSet = {
    createS2NChart(result, 0)
  }

  def createS2NChart(result: SpectroscopyResult, index: Int): SpcDataSet = {
    createS2NChart(result, "Intermediate Single Exp and Final S/N", index)
  }

  def createS2NChart(result: SpectroscopyResult, title: String, index: Int): SpcDataSet = {
    val data: java.util.List[SpcData] = new util.ArrayList[SpcData]
    data.add(new SpcData("Single Exp S/N", Color.RED, result.specS2N(index).getExpS2NSpectrum.getData))
    data.add(new SpcData("Final S/N  ", Color.BLUE, result.specS2N(index).getFinalS2NSpectrum.getData))
    new SpcDataSet("S2N", title, "Wavelength (nm)", "Signal / Noise per spectral pixel", JavaConversions.asScalaBuffer(data))
  }

}

