package edu.gemini.itc.operation;

import edu.gemini.itc.shared.*;

/**
 * The AtmosphereVisitor class is designed to adjust the SED for the
 * Atmospheric Transmission at a given airmass.
 */
public class AtmosphereVisitor implements SampledSpectrumVisitor {
    private String FILENAME = null;

    private static double _airmass;
    private ArraySpectrum _transmission = null;

    /**
     * Constructs AtmosphereVisitor with specified air mass.
     * Airmass is a ratio of the length of line of sight through
     * atmosphere to height of atmosphere.  e.g looking straight up
     * airmass = 1.0.  Looking at an angle airmass > 1.
     * We will use a different convolution file for different
     * airmass ranges.
     */
    public AtmosphereVisitor(double airmass) {
        _airmass = airmass;
        if (_airmass < 1.26) {
            FILENAME = "atmosphere_extinction_airmass10";
        } else if (_airmass > 1.75) {
            FILENAME = "atmosphere_extinction_airmass20";
        } else {
            FILENAME = "atmosphere_extinction_airmass15";
        }

        _transmission = new
                DefaultArraySpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                FILENAME + ITCConstants.DATA_SUFFIX);
    }

    /**
     * @return the airmass used by this calculation
     */
    public double getAirMass() {
        return _airmass;
    }

    /**
     * Implements the SampledSpectrumVisitor interface
     */
    public void visit(SampledSpectrum sed) {
        for (int i = 0; i < sed.getLength(); i++) {
            sed.setY(i, _transmission.getY(sed.getX(i)) * sed.getY(i));
        }
    }

    public String toString() {
        return "AtmosphereVisitor using airmass " + _airmass;
    }
}
