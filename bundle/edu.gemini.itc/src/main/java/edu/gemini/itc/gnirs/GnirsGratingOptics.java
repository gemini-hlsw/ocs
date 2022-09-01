package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.GratingOptics;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.*;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class GnirsGratingOptics extends GratingOptics {

    private final double order;
    private final double scale;

    public GnirsGratingOptics(final String directory,
                              final Disperser grating,
                              final double centralWavelength,
                              final int detectorPixels,
                              final double order,
                              final double scale,
                              final int spectralBinning) {

        super(directory, grating.name(), "gratings", centralWavelength, detectorPixels, spectralBinning);
        this.order = order;
        this.scale = scale;
    }

    // Take camera scale factor and order into account.
    public double resolutionHalfArcsecSlit() {
        System.out.println("resolutionHalfArcsecSlit(): "+  super.resolutionHalfArcsecSlit() + "  scale: "+ scale + "  order: " + order);
        return super.resolutionHalfArcsecSlit() / scale / order;
    }

    // Take camera scale factor and order into account.
    public double dispersion() {
        return super.dispersion() / scale / order;
    }

}
