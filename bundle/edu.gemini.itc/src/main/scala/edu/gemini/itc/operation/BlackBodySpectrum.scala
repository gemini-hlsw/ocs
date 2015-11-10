package edu.gemini.itc.operation

import edu.gemini.itc.base.{SampledSpectrumVisitor, ZeroMagnitudeStar, VisitableSampledSpectrum, DefaultSampledSpectrum}
import edu.gemini.spModel.core._

/**
 * This class creates a black body spectrum over the interval defined by the
 * blocking filter.  The code comes from Inger Jorgensen and Tom Geballe's
 * paper on IR spectrophotometric calibrations.  This Class implements
 * Visitable sampled specturm to create the sed.
 * (This class has been moved from Java without much further ado.)
 */
final class BlackBodySpectrum(spectrum: DefaultSampledSpectrum) extends VisitableSampledSpectrum {

  override def clone: AnyRef = {
    val clonedSpectrum: DefaultSampledSpectrum = spectrum.clone.asInstanceOf[DefaultSampledSpectrum]
    new BlackBodySpectrum(clonedSpectrum)
  }

  def trim(startWavelength: Double, endWavelength: Double) {
    spectrum.trim(startWavelength, endWavelength)
  }

  def reset(s: Array[Double], v: Double, r: Double) {
    spectrum.reset(s, v, r)
  }

  def applyWavelengthCorrection() {
    spectrum.applyWavelengthCorrection()
  }

  def accept(v: SampledSpectrumVisitor) {
    spectrum.accept(v)
  }

  //**********************
  // Accessors
  //
  /**
   * @return array of flux values.  For efficiency, it may return a
   *         referenct to actual member data.  The client must not alter this
   *         return value.
   */
  def getValues: Array[Double] = spectrum.getValues

  /**
   * @return starting x
   */
  def getStart: Double = spectrum.getStart

  /**
   * @return ending x
   */
  def getEnd: Double = spectrum.getEnd

  /**
   * @return x sample size (bin size)
   */
  def getSampling: Double = spectrum.getSampling

  /**
   * @return flux value in specified bin
   */
  def getY(index: Int): Double = spectrum.getY(index)

  /**
   * @return x of specified bin
   */
  def getX(index: Int): Double = spectrum.getX(index)

  /**
   * @return y value at specified x using linear interpolation.
   *         Silently returns zero if x is out of spectrum range.
   */
  def getY(x: Double): Double = spectrum.getY(x)

  /**
   * Returns the index of the data point with largest x value less than x
   */
  def getLowerIndex(x: Double): Int = spectrum.getLowerIndex(x)

  /**
   * @return number of bins in the histogram (number of data points)
   */
  def getLength: Int = spectrum.getLength

  //**********************
  // Mutators
  //
  /**
   * Sets y value in specified x bin.
   * If specified bin is out of range, this is a no-op.
   */
  def setY(bin: Int, y: Double) {
    spectrum.setY(bin, y)
  }

  /**
   * Rescales X axis by specified factor. Doesn't change sampling size.
   */
  def rescaleX(factor: Double) {
    spectrum.rescaleX(factor)
  }

  /**
   * Rescales Y axis by specified factor.
   */
  def rescaleY(factor: Double) {
    spectrum.rescaleY(factor)
  }

  def smoothY(factor: Int) {
    spectrum.smoothY(factor)
  }

  /**
   * Returns the integral of all the y values in the SampledSpectrum
   */
  def getIntegral: Double =spectrum.getIntegral

  /**
   * Returns the average of values in the SampledSpectrum in
   * the specified range.
   */
  def getAverage(x_start: Double, x_end: Double): Double = spectrum.getAverage(x_start, x_end)

  /**
   * This returns a 2d array of the data used to chart the SampledSpectrum
   * using JClass Chart.  The array has the following dimensions
   * double data[][] = new double[2][getLength()];
   * data[0][i] = x values
   * data[1][i] = y values
   */
  def getData: Array[Array[Double]] = spectrum.getData

  /**
   * This returns a 2d array of the data used to chart the SampledSpectrum
   * using JClass Chart.  The array has the following dimensions
   * double data[][] = new double[2][getLength()];
   * data[0][i] = x values
   * data[1][i] = y values
   *
   * @param maxXIndex data is returned up to maximum specified x bin
   */
  def getData(maxXIndex: Int): Array[Array[Double]] = spectrum.getData(maxXIndex)

  /**
   * This returns a 2d array of the data used to chart the SampledSpectrum
   * using JClass Chart.  The array has the following dimensions
   * double data[][] = new double[2][getLength()];
   * data[0][i] = x values
   * data[1][i] = y values
   *
   * @param maxXIndex data is returned from minimum specified x bin
   * @param maxXIndex data is returned up to maximum specified x bin
   */
  def getData(minXIndex: Int, maxXIndex: Int): Array[Array[Double]] = spectrum.getData(minXIndex, maxXIndex)

}

object BlackBodySpectrum {

  def apply(temp: Double, interval: Double, flux: Double, units: BrightnessUnit, band: MagnitudeBand, redshift: Redshift) = {

    //rescale the start and end depending on the redshift
    val z         = redshift.z
    val start     =   300 / (1 + z)
    val end       = 30000 / (1 + z)
    val n         = ((end - start) / interval + 1).toInt
    val fluxArray = new Array[Double](n + 40)

    //if units need to be converted do it.
    val magFlux = convertToMag(flux, units, band)

    var i = 0
    var wavelength = start
    while (wavelength <= end) {
      fluxArray(i) = blackbodyFlux(wavelength, temp)
      i = i + 1
      wavelength += interval
    }

    val spectrum = new DefaultSampledSpectrum(fluxArray, start, interval)

    //with blackbody convert W m^2 um^-1 to phot....
    val zeropoint = ZeroMagnitudeStar.getAverageFlux(band)
    val phot_norm = zeropoint * Math.pow(10.0, -0.4 * magFlux)
    val average   = spectrum.getAverage(band.start.toNanometers / (1 + z), band.end.toNanometers / (1 + z))

    // Calculate multiplier.
    val multiplier = phot_norm / average
    spectrum.rescaleY(multiplier)
    
    new BlackBodySpectrum(spectrum)

  }

  private def blackbodyFlux(lambda: Double, temp: Double): Double =
    (1 / Math.pow(lambda / 1000, 4)) * (1 / (Math.exp(14387 / (lambda / 1000 * temp)) - 1))

  private def convertToMag(flux: Double, units: BrightnessUnit, band: MagnitudeBand): Double = {
    //THis method should convert the flux into units of magnitude.
    //same code as in NormalizeVisitor.java.  Eventually should come out
    // into a genral purpose conversion class if needs to be used again.

    def convert(norm: Double) = {
      val zeropoint: Double = ZeroMagnitudeStar.getAverageFlux(band)
      -(Math.log(norm / zeropoint) / Math.log(10)) / .4
    }

    units match {
      case MagnitudeSystem.Vega | SurfaceBrightness.Vega =>
        flux // this is already mag

      case MagnitudeSystem.AB | SurfaceBrightness.AB =>
        convert(5.632e10 * Math.pow(10, -0.4 * flux) / band.center.toNanometers)

      case MagnitudeSystem.Jy | SurfaceBrightness.Jy =>
        convert(flux * 1.509e7 / band.center.toNanometers)

      case MagnitudeSystem.Watts | SurfaceBrightness.Watts =>
        convert(flux * band.center.toNanometers / 1.988e-13)

      case MagnitudeSystem.ErgsWavelength | SurfaceBrightness.ErgsWavelength =>
        convert(flux * band.center.toNanometers / 1.988e-14)

      case MagnitudeSystem.ErgsFrequency | SurfaceBrightness.ErgsFrequency =>
        convert(flux * 1.509e30 / band.center.toNanometers)

    }


  }

}
