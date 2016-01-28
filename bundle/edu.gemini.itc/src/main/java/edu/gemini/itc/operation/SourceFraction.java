package edu.gemini.itc.operation;

/**
 * Helper to calculate the fraction of the incoming source flux that goes through the aperture.
 * Also calculates the diameter of the aperture in [arcsec] and the pixels covered by the aperture.
 */
public interface SourceFraction {

    /**
     * Gets the fraction of the source flux that goes through the aperture.
     * For the simplest case (uniform sources) this corresponds to the area of the aperture in arcsec² (flux per area
     * multiplied by area equals the total flux), for other sources the corresponding flux distribution (e.g. gaussian)
     * over the area of the aperture is taken into account to calculate this factor. The unit of the factor is arcsec².
     */
    double getSourceFraction();

    /** Gets the number of enclosed pixels in this aperture. */
    double getNPix();

    /**
     * Gets the diameter of this aperture in arcsec.
     * This value is calculated for a circular aperture and makes only sense for imaging.
     * (It would make sense to split the imaging and spectroscopy cases more clearly in the implementation
     * because some things need to be handled differently and/or make only sense in one or the other case.)
     */
    double getSoftwareAperture();

}
