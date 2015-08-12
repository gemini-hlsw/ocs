package edu.gemini.itc.base

import edu.gemini.spModel.core.MagnitudeBand

/**
 * This class encapsulates the photon flux density (in photons/s/m^2/nm)
 * for a zero-magnitude star.
 */
object ZeroMagnitudeStar {

  import MagnitudeBand._

  /**
   * Get average flux in photons/s/m^2/nm in specified waveband.
   * Overrides method in base class AverageFlux.
   */
  def getAverageFlux(band: MagnitudeBand): Int = band match {

    // flux densities for each waveband for zero-magnitude star (of some type)
    // units are photons/s/m^2/nm
    case U => 75900000
    case B => 146100000
    case V => 97100000
    case R => 64600000
    case I => 39000000
    case Y => 29680000
    case J => 19700000
    case H => 9600000
    case K => 4500000
    case L => 990000
    case M => 510000
    case N => 51000
    case Q => 7700

    // Values for Sloan filters taken from Schneider, Gunn, & Hoessel (1983)
    case `_u` => 65670000
    case `_g` => 117000000
    case `_r` => 108000000
    case `_i` => 93600000
    case `_z` => 79800000

    // Other bands are not supported, in particular AP and UC
    case _ => sys.error("unsupported waveband " + band.name)


  }
}
