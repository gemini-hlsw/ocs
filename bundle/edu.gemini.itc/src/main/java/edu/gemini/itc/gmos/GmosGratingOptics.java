package edu.gemini.itc.gmos;

import edu.gemini.itc.base.Detector;
import edu.gemini.itc.base.GratingOptics;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class GmosGratingOptics extends GratingOptics {

    public GmosGratingOptics(final String directory,
                             final GmosCommonType.Disperser grating,
                             final Detector detector,
                             final double centralWavelength,
                             final int detectorPixels,
                             final int spectralBinning) {

        super(directory, grating.name(), gratingsName(detector), centralWavelength, detectorPixels, spectralBinning);
    }

    private static String gratingsName(final Detector detector) {
        return detector.toString().contains("EEV") ? "eev_gratings" : "gratings";
    }

}
