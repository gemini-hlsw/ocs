package edu.gemini.itc.igrins2;

import edu.gemini.itc.base.GratingOptics;

public class Igrins2GratingOptics extends GratingOptics {
    public Igrins2GratingOptics(String directory,
                                String gratingName,
                                String gratingList,
                                double centralWavelength,
                                int detectorPixels,
                                int spectralBinning) {
        super(directory, gratingName, gratingList, centralWavelength, detectorPixels, spectralBinning);
        data.keySet();
    }
    public double getStart() { return get_trans().getStart(); }
    public double getEnd() { return get_trans().getEnd(); }
    public double getSpectralPixelWidth() { return dispersion(-1); }
}
