package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.Instrument;

/**
 * This represents the transmission of the optics native to the camera.
 */
public class LongCameraRedOptics extends LongCameraOptics {
    private static final String CAMERA_FILENAME = FILENAME +
            GnirsParameters.getRedCameraName() + Instrument.getSuffix();

    public LongCameraRedOptics(String directory) {
        super(directory + "/" + CAMERA_FILENAME);
    }

    public String toString() {
        return super.toString() + "Red)";
    }
}

