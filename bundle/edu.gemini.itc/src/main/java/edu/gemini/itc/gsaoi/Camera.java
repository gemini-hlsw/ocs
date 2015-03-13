package edu.gemini.itc.gsaoi;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.TransmissionElement;

/**
 * This represents the transmission of the optics native to the camera.
 */
public class Camera extends TransmissionElement {
    private static final String FILENAME = Gsaoi.getPrefix() +
            "camera" + Instrument.getSuffix();

    public Camera(String directory) {
        super(directory + FILENAME);
    }

    public String toString() {
        return "Camera: 0.02\"/pix";
    }
}
