package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.Instrument;

/**
 * This represents the transmission of the optics native to the camera.
 */
public class LongCameraBlueOptics extends LongCameraOptics {
    private static final String CAMERA_FILENAME = FILENAME +
            GnirsParameters.getBlueCameraName() + Instrument.getSuffix();

    public LongCameraBlueOptics(String directory) {
        super(directory + "/" + CAMERA_FILENAME);
    }

    public String toString() {
        return super.toString() + "Blue)";
    }
}

