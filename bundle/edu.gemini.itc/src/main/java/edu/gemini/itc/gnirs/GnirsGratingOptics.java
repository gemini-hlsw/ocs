package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.GratingOptics;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class GnirsGratingOptics extends GratingOptics {

    public GnirsGratingOptics(final String directory,
                              final String gratingName,
                              final double centralWavelength,
                              final int detectorPixels,
                              final int spectralBinning) {

        super(directory, gratingName, "gratings", centralWavelength, detectorPixels, spectralBinning);
    }

}
