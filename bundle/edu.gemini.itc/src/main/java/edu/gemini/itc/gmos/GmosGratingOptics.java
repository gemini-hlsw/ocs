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

    @Override protected int getGratingNumber() {
        int grating_num = 0;

        if (_gratingName.equals(GmosParameters.B1200_G5301)) {
            grating_num = GmosParameters.B1200;
        } else if (_gratingName.equals(GmosParameters.R831_G5302)) {
            grating_num = GmosParameters.R831;
        } else if (_gratingName.equals(GmosParameters.B600_G5303)) {
            grating_num = GmosParameters.B600;
        } else if (_gratingName.equals(GmosParameters.R600_G5304)) {
            grating_num = GmosParameters.R600;
        } else if (_gratingName.equals(GmosParameters.R400_G5305)) {
            grating_num = GmosParameters.R400;
        } else if (_gratingName.equals(GmosParameters.R150_G5306)) {
            grating_num = GmosParameters.R150;
        }
        return grating_num;
    }

}
