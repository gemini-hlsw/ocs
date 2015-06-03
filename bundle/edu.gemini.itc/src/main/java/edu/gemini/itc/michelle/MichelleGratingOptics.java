package edu.gemini.itc.michelle;

import edu.gemini.itc.base.GratingOptics;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class MichelleGratingOptics extends GratingOptics {

    private static final double SPECTROSCOPY_FRAME_TIME = .1; //Seconds
    private static final double SPECTROSCOPY_LOWRES_N_FRAME_TIME = .25; //Seconds
    private static final double SPECTROSCOPY_MED_N1_FRAME_TIME = 1.25; //Seconds
    private static final double SPECTROSCOPY_MED_N2_FRAME_TIME = 3.0; //Seconds
    private static final double SPECTROSCOPY_ECHELLE_FRAME_TIME = 30; //Seconds

    public MichelleGratingOptics(final String directory,
                                 final String gratingName,
                                 final double centralWavelength,
                                 final int detectorPixels) {

        super(directory, gratingName, "gratings", centralWavelength, detectorPixels, 1);
    }

    public double getFrameTime() {
        final double frameTime;
        switch (gratingName) {
            case MichelleParameters.LOW_N:
                frameTime = SPECTROSCOPY_LOWRES_N_FRAME_TIME;
                break;
            case MichelleParameters.MED_N1:
                frameTime = SPECTROSCOPY_MED_N1_FRAME_TIME;
                break;
            case MichelleParameters.MED_N2:
                frameTime = SPECTROSCOPY_MED_N2_FRAME_TIME;
                break;
            case MichelleParameters.ECHELLE_N:
            case MichelleParameters.ECHELLE_Q:
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
