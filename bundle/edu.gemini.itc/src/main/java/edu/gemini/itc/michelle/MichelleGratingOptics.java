package edu.gemini.itc.michelle;

import edu.gemini.itc.shared.GratingOptics;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class MichelleGratingOptics extends GratingOptics {

    public MichelleGratingOptics(final String directory,
                                 final String gratingName,
                                 final double centralWavelength,
                                 final int detectorPixels,
                                 final int spectralBinning) throws Exception {

        super(directory, gratingName, "gratings", centralWavelength, detectorPixels, spectralBinning);
    }

    @Override protected int getGratingNumber() {
        int grating_num = 0;

        if (gratingName.equals(MichelleParameters.LOW_N)) {
            grating_num = MichelleParameters.LOWN;
        } else if (gratingName.equals(MichelleParameters.LOW_Q)) {
            grating_num = MichelleParameters.LOWQ;
        } else if (gratingName.equals(MichelleParameters.MED_N1)) {
            grating_num = MichelleParameters.MEDN1;
        } else if (gratingName.equals(MichelleParameters.MED_N2)) {
            grating_num = MichelleParameters.MEDN2;
        } else if (gratingName.equals(MichelleParameters.ECHELLE_N)) {
            grating_num = MichelleParameters.ECHELLEN;
        } else if (gratingName.equals(MichelleParameters.ECHELLE_Q)) {
            grating_num = MichelleParameters.ECHELLEQ;
        }
        return grating_num;
    }


    @Override public double getStart() {
        return centralWavelength - (data.get(getGratingNumber()).dispersion * detectorPixels / 2) * _spectralBinning;
    }

    @Override public double getEnd() {
        return centralWavelength + (data.get(getGratingNumber()).dispersion * detectorPixels / 2) * _spectralBinning;
    }

}
