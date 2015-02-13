package edu.gemini.itc.shared

import edu.gemini.itc.parameters.SourceDefinitionParameters.{BrightnessUnit, Profile}

sealed trait SpatialProfile {
  val norm: Double
  val units: BrightnessUnit
}
final case class PointSource(norm: Double, units: BrightnessUnit) extends SpatialProfile
final case class GaussianSource(norm: Double, units: BrightnessUnit, fwhm: Double) extends SpatialProfile {
  require (fwhm >= 0.1, "Please use a Gaussian FWHM greater than 0.1")
}
final case class UniformSource(norm: Double, units: BrightnessUnit) extends SpatialProfile

sealed trait SpectralDistribution
final case class BlackBody(temperature: Double) extends SpectralDistribution
final case class PowerLaw(index: Double) extends SpectralDistribution
final case class EmissionLine(wavelength: Double, width: Double, flux: Double, fluxUnits: String, continuum: Double, continuumUnits: String) extends SpectralDistribution
final case class UserDefined(name: String, spectrum: String) extends SpectralDistribution
sealed trait Library extends SpectralDistribution {
  val specType: String
  val sedSpectrum: String
}
final case class LibraryStar(specType: String, sedSpectrum: String) extends Library
final case class LibraryNonStar(specType: String, sedSpectrum: String) extends Library

object SpectralDistribution {

}
