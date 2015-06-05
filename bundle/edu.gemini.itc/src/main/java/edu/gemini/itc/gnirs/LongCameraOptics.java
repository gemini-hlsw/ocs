package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.TransmissionElement;

/**
 * This represents the transmission of the optics native to the camera.
 */
public abstract class LongCameraOptics extends TransmissionElement {
    protected static final String FILENAME = Gnirs.getPrefix() + GnirsParameters.getLongCameraName();

    public LongCameraOptics(String cameraTransmissionFile) {
        super(cameraTransmissionFile);
    }

    public String toString() {
        return "Camera: 0.05arcsec/pix (Long ";
    }
}

