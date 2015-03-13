package edu.gemini.itc.trecs;

import edu.gemini.itc.shared.GratingOptics;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class TrecsGratingOptics extends GratingOptics {

    public TrecsGratingOptics(final String directory,
                              final String gratingName,
                              final double centralWavelength,
                              final int detectorPixels,
                              final int spectralBinning) {

        super(directory, gratingName, "gratings", centralWavelength, detectorPixels, spectralBinning);
    }

    @Override  public double getStart() {
        return centralWavelength - (data.apply(gratingName).dispersion() * detectorPixels / 2) * _spectralBinning;
    }

    @Override  public double getEnd() {
        return centralWavelength + (data.apply(gratingName).dispersion() * detectorPixels / 2) * _spectralBinning;
    }

}
