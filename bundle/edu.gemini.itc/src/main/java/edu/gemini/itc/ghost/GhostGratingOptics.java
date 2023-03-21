package edu.gemini.itc.ghost;

import edu.gemini.itc.base.GratingOptics;

import java.util.logging.Logger;

public class GhostGratingOptics extends GratingOptics {
    private static final Logger log = Logger.getLogger(GhostGratingOptics.class.getName());
    public GhostGratingOptics(String directory, String gratingName, String gratingsName, double centralWavelength, int detectorPixels, int spectralBinning) {
        super(directory, gratingName, gratingsName, centralWavelength, detectorPixels, spectralBinning);
        data.keySet();
    }

    public double getStart() {
        return get_trans().getStart();
    }

    public double getEnd() {
        return get_trans().getEnd();
    }

    public double getPixelWidth() {
        //return dispersion(-1) * _spectralBinning;
        return dispersion(-1);
    }
}
