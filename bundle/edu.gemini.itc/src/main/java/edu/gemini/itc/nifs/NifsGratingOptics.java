package edu.gemini.itc.nifs;

import edu.gemini.itc.base.GratingOptics;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class NifsGratingOptics extends GratingOptics {

    public NifsGratingOptics(final String directory,
                             final String gratingName,
                             final double centralWavelength,
                             final int detectorPixels,
                             final int spectralBinning) {

        super(directory, gratingName, "gratings", centralWavelength, detectorPixels, spectralBinning);
    }

}
