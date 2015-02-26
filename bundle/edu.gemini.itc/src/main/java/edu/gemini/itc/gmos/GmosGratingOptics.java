package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.Detector;
import edu.gemini.itc.shared.GratingOptics;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class GmosGratingOptics extends GratingOptics {

    public GmosGratingOptics(String directory,
                             GmosCommonType.Disperser grating,
                             Detector detector,
                             double centralWavelength,
                             int detectorPixels,
                             int spectralBinning) throws Exception {

        super(directory, grating.name(), gratingsName(detector), centralWavelength, detectorPixels, spectralBinning);
    }

    private static String gratingsName(Detector detector) {
        return detector.toString().contains("EEV") ? "eev_gratings" : "gratings";
    }

}
