package edu.gemini.itc.shared

sealed trait Recipe

trait ImagingRecipe extends Recipe {
  def calculateImaging(): ImagingResult
}

trait ImagingArrayRecipe extends Recipe {
  def calculateImaging(): Array[ImagingResult]
}


trait SpectroscopyRecipe extends Recipe {
  def calculateSpectroscopy(): SpectroscopyResult
}

trait SpectroscopyArrayRecipe extends Recipe {
  def calculateSpectroscopy(): Array[SpectroscopyResult]
}