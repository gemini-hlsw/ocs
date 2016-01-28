package edu.gemini.itc.base;

/**
 * Definition of capabilities needed for instruments that support spectroscopy.
 */
public interface SpectroscopyInstrument {

    /** Gets the slit width of the mask in arcsec. */
    double getSlitWidth();

}
