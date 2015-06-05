package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.TransmissionElement;

/**
 * This represents the transmission of the optics native to the camera.
 */
public abstract class ShortCameraOptics extends TransmissionElement {
    protected static final String FILENAME = Gnirs.getPrefix() + GnirsParameters.getShortCameraName();

    public ShortCameraOptics(String cameraTransmissionFile) {
        super(cameraTransmissionFile);
    }

    public String toString() {
        return "Camera: 0.15arcsec/pix (Short ";
    }
}
