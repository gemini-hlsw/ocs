package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.GratingOptics;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class GnirsGratingOptics extends GratingOptics {

    public GnirsGratingOptics(final String directory,
                              final String gratingName,
                              final double centralWavelength,
                              final int detectorPixels,
                              final int spectralBinning) throws Exception {

        super(directory, gratingName, "gratings", centralWavelength, detectorPixels, spectralBinning);
    }

    @Override protected int getGratingNumber() {
        int grating_num = 0;

        if (gratingName.equals(GnirsParameters.G10)) {
            grating_num = GnirsParameters.G10_N;
        } else if (gratingName.equals(GnirsParameters.G32)) {
            grating_num = GnirsParameters.G32_N;
        } else if (gratingName.equals(GnirsParameters.G110)) {
            grating_num = GnirsParameters.G110_N;
        }
        return grating_num;
    }

}
