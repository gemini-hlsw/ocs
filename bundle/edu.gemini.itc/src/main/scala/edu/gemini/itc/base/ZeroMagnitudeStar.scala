package edu.gemini.itc.base

import edu.gemini.spModel.core.MagnitudeBand

/**
 * This class encapsulates the photon flux density (in photons/s/m^2/nm)
 * for a zero-magnitude star.
 */
object ZeroMagnitudeStar {
  /**
   * Get average flux in photons/s/m^2/nm in specified waveband.
   * Overrides method in base class AverageFlux.
   */
  def getAverageFlux(band: MagnitudeBand): Int = band match {

    // flux densities for each waveband for zero-magnitude star (of some type)
    // units are photons/s/m^2/nm
    // TODO: add missing Y band
    case MagnitudeBand.U  => 75900000
    case MagnitudeBand.B  => 146100000
    case MagnitudeBand.V  => 97100000
    case MagnitudeBand.R  => 64600000
    case MagnitudeBand.I  => 39000000
    case MagnitudeBand.J  => 19700000
    case MagnitudeBand.H  => 9600000
    case MagnitudeBand.K  => 4500000
    case MagnitudeBand.L  => 990000
    case MagnitudeBand.M  => 510000
    case MagnitudeBand.N  => 51000
    case MagnitudeBand.Q  => 7700

    // Values for Sloan filters taken from Schneider, Gunn, & Hoessel (1983)
    // TODO: add missing u' band
    case MagnitudeBand._g => 117000000
    case MagnitudeBand._r => 108000000
    case MagnitudeBand._i => 93600000
    case MagnitudeBand._z => 79800000
    case _                => throw new IllegalArgumentException("ITC does not support waveband " + band.name)

  }
}
