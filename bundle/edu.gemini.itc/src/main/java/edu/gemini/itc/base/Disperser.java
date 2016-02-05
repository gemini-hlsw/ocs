package edu.gemini.itc.base;

import edu.gemini.itc.operation.Slit;

/**
 * Representation of a disperser element
 */
public interface Disperser {

    double dispersion();

    /** Spectral resolution in nm for a 0.5-arcsec slit */
    double resolution();

    /** Calculates the size of a spectral resolution element in [nm] for the background */
    default double resolution(final Slit slit) {
        return resolution() * slit.width() / 0.5;
    }

    /** Calculates the size of a spectral resolution element in [nm] for the source */
    default double resolution(final Slit slit, final double imgQuality) {
        //if image size is less than the slit width it will determine resolution
        final double width = imgQuality < slit.width() ? imgQuality : slit.width();
        return resolution() * width / 0.5;
    }

}
