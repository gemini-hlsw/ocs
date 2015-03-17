package edu.gemini.itc.shared;

import edu.gemini.itc.service.WavebandDefinition;

/**
 * This class encapsulates the photon flux density (in photons/s/m^2/nm)
 * for a zero-magnitude star.
 */
public final class ZeroMagnitudeStar {
    // Keep anyone from instantiating this object.
    private ZeroMagnitudeStar() {
    }

    /**
     * Get average flux in photons/s/m^2/nm in specified waveband.
     * Overrides method in base class AverageFlux.
     */
    public static int getAverageFlux(WavebandDefinition waveband) {

        // flux densities for each waveband for zero-magnitude star (of some type)
        // units are photons/s/m^2/nm
        switch (waveband) {
            case U:     return 75900000;
            case B:     return 146100000;
            case V:     return 97100000;
            case R:     return 64600000;
            case I:     return 39000000;
            case J:     return 19700000;
            case H:     return 9600000;
            case K:     return 4500000;
            case L:     return 990000;
            case M:     return 510000;
            case N:     return 51000;
            case Q:     return 7700;

            // Values for Sloan filters taken from Schneider, Gunn, & Hoessel (1983)
            case g:     return 117000000;
            case r:     return 108000000;
            case i:     return 93600000;
            case z:     return 79800000;

            default: throw new IllegalArgumentException("unknown waveband " + waveband);
        }
    }
}
