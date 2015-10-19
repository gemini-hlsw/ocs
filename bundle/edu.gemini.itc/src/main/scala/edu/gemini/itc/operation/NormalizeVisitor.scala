package edu.gemini.itc.operation

import edu.gemini.itc.base.{ZeroMagnitudeStar, SampledSpectrum, SampledSpectrumVisitor}
import edu.gemini.spModel.core.{SurfaceBrightness, MagnitudeSystem, BrightnessUnit, MagnitudeBand}

/**
 * The NormalizeVisitor class is used to perform Normalization to the SED.
 * Normalization rescales the SED so that the average flux in a
 * specified waveband is equal to a specified value.
 * This is where unit conversion happens.
 */
final class NormalizeVisitor(band: MagnitudeBand, userNorm: Double, units: BrightnessUnit) extends SampledSpectrumVisitor {

  /**
   * Implements the visitor interface.
   * Performs the normalization.
   */
  def visit(sed: SampledSpectrum): Unit = {

    val norm = units match {

      case MagnitudeSystem.Vega | SurfaceBrightness.Vega =>
        val zeropoint = ZeroMagnitudeStar.getAverageFlux(band)
        zeropoint * Math.pow(10.0, -0.4 * userNorm)

      case MagnitudeSystem.AB | SurfaceBrightness.AB =>
        5.632e10 * Math.pow(10, -0.4 * userNorm) / band.center.toNanometers

      case MagnitudeSystem.Jy | SurfaceBrightness.Jy =>
        userNorm * 1.509e7 / band.center.toNanometers

      case MagnitudeSystem.Watts | SurfaceBrightness.Watts =>
        userNorm * band.center.toNanometers / 1.988e-13

      case MagnitudeSystem.ErgsWavelength | SurfaceBrightness.ErgsWavelength =>
        userNorm * band.center.toNanometers / 1.988e-14

      case MagnitudeSystem.ErgsFrequency | SurfaceBrightness.ErgsFrequency =>
        userNorm * 1.509e30 / band.center.toNanometers

    }

    // Calculate avg flux density in chosen normalization band.
    val average = sed.getAverage(band.start.toNanometers, band.end.toNanometers)

    // Calculate multiplier.
    val multiplier = norm / average

    // Apply normalization, multiply every value in the SED by
    // multiplier and then its average in specified band will be
    // the required amount.
    sed.rescaleY(multiplier)
  }

}