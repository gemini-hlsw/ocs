package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.GratingOptics;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.*;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class GnirsGratingOptics extends GratingOptics {

    public GnirsGratingOptics(final String directory,
                              final Disperser grating,
                              final double centralWavelength,
                              final int detectorPixels,
                              final int spectralBinning) {

        super(directory, grating.name(), "gratings", centralWavelength, detectorPixels, spectralBinning);
    }

}
