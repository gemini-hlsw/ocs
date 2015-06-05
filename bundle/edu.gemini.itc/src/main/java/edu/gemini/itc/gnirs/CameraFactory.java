package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.PixelScale;

public final class CameraFactory {

    private static final String BLUE    = "BC";
    private static final String RED     = "RC";

    private static final String LONG    = "L";
    private static final String SHORT   = "S";


    public static TransmissionElement camera(final GnirsParameters params, final String directory) {

        final String cameraLength = getCameraLength(params.getPixelScale());
        final String cameraColor  = getCameraColor(params.getInstrumentCentralWavelength());

        return new TransmissionElement(directory + "/" + Gnirs.getPrefix() + cameraLength + cameraColor + Instrument.getSuffix()) {
            public String toString() {
                // prepare a pretty string which is used as the name of this transmission element
                final String length = cameraLength.equals("L") ? "Long" : "Short";
                final String color  = cameraColor.equals("BC") ? "Blue" : "Red";
                return String.format("Camera: %.2farcsec/pix (%s %s)", params.getPixelScale().getValue(), length, color);
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
