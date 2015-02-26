package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.Detector;
import edu.gemini.itc.shared.GratingOptics;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class GmosGratingOptics extends GratingOptics {

    public GmosGratingOptics(String directory,
                             String gratingName,
                             Detector detector,
                             double centralWavelength,
                             int detectorPixels,
                             int spectralBinning) throws Exception {

        super(directory, gratingName, gratingsName(detector), centralWavelength, detectorPixels, spectralBinning);
    }

    private static String gratingsName(Detector detector) {
        return detector.toString().contains("EEV") ? "eev_gratings" : "gratings";
    }

}
