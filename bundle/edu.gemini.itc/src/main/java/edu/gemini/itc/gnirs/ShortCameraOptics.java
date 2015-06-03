package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.TransmissionElement;

/**
 * This represents the transmission of the optics native to the camera.
 */
public abstract class ShortCameraOptics extends TransmissionElement implements CameraOptics {
    protected static final String FILENAME = Gnirs.getPrefix() + GnirsParameters.getShortCameraName();

    public ShortCameraOptics(String cameraTransmissionFile) {
        super(cameraTransmissionFile);
    }

    public double getPixelScale() {
        return Gnirs.SHORT_CAMERA_PIXEL_SCALE;
    }

    public String toString() {
        return "Camera: " + getPixelScale() + "arcsec/pix (Short ";
    }
}
