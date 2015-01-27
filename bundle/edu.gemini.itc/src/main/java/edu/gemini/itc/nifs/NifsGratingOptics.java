package edu.gemini.itc.nifs;

import edu.gemini.itc.shared.GratingOptics;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public final class NifsGratingOptics extends GratingOptics {

    public NifsGratingOptics(final String directory,
                             final String gratingName,
                             final double centralWavelength,
                             final int detectorPixels,
                             final int spectralBinning)
            throws Exception {

        super(directory, gratingName, "gratings", centralWavelength, detectorPixels, spectralBinning);
    }

    @Override protected int getGratingNumber() {
        int grating_num = 0;

        if (_gratingName.equals(NifsParameters.Z_G5602)) {
            grating_num = NifsParameters.Z_G5602_N;
        } else if (_gratingName.equals(NifsParameters.J_G5603)) {
            grating_num = NifsParameters.J_G5603_N;
        } else if (_gratingName.equals(NifsParameters.H_G5604)) {
            grating_num = NifsParameters.H_G5604_N;
        } else if (_gratingName.equals(NifsParameters.K_G5605)) {
            grating_num = NifsParameters.K_G5605_N;
        } else if (_gratingName.equals(NifsParameters.KS_G5606)) {
            grating_num = NifsParameters.KS_G5606_N;
        } else if (_gratingName.equals(NifsParameters.KL_G5607)) {
            grating_num = NifsParameters.KL_G5607_N;
        }
        return grating_num;
    }

}
