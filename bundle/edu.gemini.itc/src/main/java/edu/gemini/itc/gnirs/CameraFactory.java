package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.PixelScale;

public final class CameraFactory {

    private static final String BLUE    = "_BLUE";
    private static final String RED     = "_RED";

    private static final String LONG    = "LONG";
    private static final String SHORT   = "SHORT";


    public static TransmissionElement camera(final PixelScale pixelScale, final double wavelength, final String directory) {

        final String cameraLength = getCameraLength(pixelScale);
        final String cameraColor  = getCameraColor(wavelength);

        return new TransmissionElement(directory + "/" + Gnirs.getPrefix() + cameraLength + cameraColor + Instrument.getSuffix()) {
            public String toString() {
                // prepare a pretty string which is used as the name of this transmission element
                final String length = cameraLength.equals("LONG") ? "Long" : "Short";
                final String color  = cameraColor.equals("_BLUE") ? "Blue" : "Red";
                return String.format("Camera: %.2farcsec/pix (%s %s)", pixelScale.getValue(), length, color);
            }
        };
    }

    private static String getCameraLength(final PixelScale pixelScale) {
        switch (pixelScale) {
            case PS_005: return LONG;
            case PS_015: return SHORT;
            default:     throw new Error();
        }
    }

    private static String getCameraColor(final double wavelength) {
        if (wavelength < 2600) {
            return BLUE;
        } else {
            return RED;
        }
    }
}
