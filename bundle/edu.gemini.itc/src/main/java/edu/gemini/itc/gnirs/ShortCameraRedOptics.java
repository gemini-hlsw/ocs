package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.Instrument;

/**
 * This represents the transmission of the optics native to the camera.
 */
public class ShortCameraRedOptics extends ShortCameraOptics {
    private static final String CAMERA_FILENAME = FILENAME +
            GnirsParameters.getRedCameraName() + Instrument.getSuffix();

    public ShortCameraRedOptics(String directory) {
        super(directory + "/" + CAMERA_FILENAME);
    }

    public String toString() {
        return super.toString() + "Red)";
    }
}
