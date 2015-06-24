package edu.gemini.itc.base;

import edu.gemini.spModel.core.MagnitudeBand;

/**
 * This class encapsulates the photon flux density (in photons/s/m^2/nm)
 * for a zero-magnitude star.
 */
public final class ZeroMagnitudeStar {
    // Keep anyone from instantiating this object.
    private ZeroMagnitudeStar() {}

    /**
     * Get average flux in photons/s/m^2/nm in specified waveband.
     * Overrides method in base class AverageFlux.
     */
    public static int getAverageFlux(final MagnitudeBand band) {

        // flux densities for each waveband for zero-magnitude star (of some type)
        // units are photons/s/m^2/nm
        // TODO: Missing support for Y band
        if      (band.equals(MagnitudeBand.U$.MODULE$))  return 75900000;
        else if (band.equals(MagnitudeBand.B$.MODULE$))  return 146100000;
        else if (band.equals(MagnitudeBand.V$.MODULE$))  return 97100000;
        else if (band.equals(MagnitudeBand.R$.MODULE$))  return 64600000;
        else if (band.equals(MagnitudeBand.I$.MODULE$))  return 39000000;
        else if (band.equals(MagnitudeBand.J$.MODULE$))  return 19700000;
        else if (band.equals(MagnitudeBand.H$.MODULE$))  return 9600000;
        else if (band.equals(MagnitudeBand.K$.MODULE$))  return 4500000;
        else if (band.equals(MagnitudeBand.L$.MODULE$))  return 990000;
        else if (band.equals(MagnitudeBand.M$.MODULE$))  return 510000;
        else if (band.equals(MagnitudeBand.N$.MODULE$))  return 51000;
        else if (band.equals(MagnitudeBand.Q$.MODULE$))  return 7700;

        // Values for Sloan filters taken from Schneider, Gunn, & Hoessel (1983)
        // TODO: Missing support for u' band
        else if (band.equals(MagnitudeBand._g$.MODULE$)) return 117000000;
        else if (band.equals(MagnitudeBand._r$.MODULE$)) return 108000000;
        else if (band.equals(MagnitudeBand._i$.MODULE$)) return 93600000;
        else if (band.equals(MagnitudeBand._z$.MODULE$)) return 79800000;

        else throw new IllegalArgumentException("ITC does not support waveband " + band.name());

    }
}
