package edu.gemini.itc.michelle;

import edu.gemini.itc.base.GratingOptics;
import edu.gemini.spModel.gemini.michelle.MichelleParams;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class MichelleGratingOptics extends GratingOptics {

    private static final double SPECTROSCOPY_FRAME_TIME = .1; //Seconds
    private static final double SPECTROSCOPY_LOWRES_N_FRAME_TIME = .25; //Seconds
    private static final double SPECTROSCOPY_MED_N1_FRAME_TIME = 1.25; //Seconds
    private static final double SPECTROSCOPY_MED_N2_FRAME_TIME = 3.0; //Seconds
    private static final double SPECTROSCOPY_ECHELLE_FRAME_TIME = 30; //Seconds

    private final MichelleParams.Disperser grating;

    public MichelleGratingOptics(final String directory,
                                 final MichelleParams.Disperser grating,
                                 final double centralWavelength,
                                 final int detectorPixels) {

        super(directory, getGratingName(grating, centralWavelength), "gratings", centralWavelength, detectorPixels, 1);
        this.grating = grating;
    }

    private static String getGratingName(final MichelleParams.Disperser grating, final double centralWavelength) {
        switch (grating) {
            case ECHELLE:
                // ECHELLE_N covers 7-14 microns, ECHELLE_Q covers 16-26 microns
                if (centralWavelength <= 15) return "ECHELLE_N"; else return "ECHELLE_Q";
            default:
                return grating.name();
        }
    }

    public double getFrameTime() {
        final double frameTime;
        switch (grating) {
            case LOW_RES_10:
                frameTime = SPECTROSCOPY_LOWRES_N_FRAME_TIME;
                break;
            case MED_RES:
                frameTime = SPECTROSCOPY_MED_N1_FRAME_TIME;
                break;
            case HIGH_RES:
                frameTime = SPECTROSCOPY_MED_N2_FRAME_TIME;
                break;
            case ECHELLE:
                frameTime = SPECTROSCOPY_ECHELLE_FRAME_TIME;
                break;
            default:
                frameTime = SPECTROSCOPY_FRAME_TIME;
                break;
        }

        return frameTime;
    }

    @Override public double getStart() {
        return centralWavelength - (data.apply(gratingName).dispersion() * detectorPixels / 2) * _spectralBinning;
    }

    @Override public double getEnd() {
        return centralWavelength + (data.apply(gratingName).dispersion() * detectorPixels / 2) * _spectralBinning;
    }

}
