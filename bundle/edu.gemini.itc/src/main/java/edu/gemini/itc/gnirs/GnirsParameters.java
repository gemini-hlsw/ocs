package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.spModel.core.Wavelength;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.*;

/**
 * This class holds the information from the Gnirs section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GnirsParameters implements InstrumentDetails {

    public static final String BLUE = "BC";
    public static final String RED = "RC";

    public static final String LONG = "L";
    public static final String SHORT = "S";

    public static final double LONG_CAMERA_SCALE_FACTOR = 3.0;

    private static final double XDISP_CENTRAL_WAVELENGTH = 1616.85;

    // Data members
    private final Disperser     grating;
    private final PixelScale    pixelScale;
    private final ReadMode      readMode;
    private final Wavelength    instrumentCentralWavelength;
    private final SlitWidth     slitWidth;
    private final CrossDispersed xDisp;

    /**
     * Constructs a GnirsParameters.
     */
    public GnirsParameters(final PixelScale     pixelScale,
                           final Disperser      grating,
                           final ReadMode       readMode,
                           final CrossDispersed xDisp,
                           final Wavelength     instrumentCentralWavelength,
                           final SlitWidth      slitWidth) {
        this.grating                        = grating;
        this.pixelScale                     = pixelScale;
        this.xDisp                          = xDisp;
        this.readMode                       = readMode;
        this.instrumentCentralWavelength    = instrumentCentralWavelength;
        this.slitWidth                      = slitWidth;
    }

    public PixelScale getPixelScale() {
        return pixelScale;
    }

    public SlitWidth getSlitWidth() {
        return slitWidth;
    }

    public Disperser getGrating() {
        return grating;
    }

    public ReadMode getReadMode() {
        return readMode;
    }

    public double getInstrumentCentralWavelength() {
        if (!isXDispUsed()) {
            return instrumentCentralWavelength.toNanometers();
        } else {
            return XDISP_CENTRAL_WAVELENGTH;
        }
    }

    public double getUnXDispCentralWavelength() {
        return instrumentCentralWavelength.toNanometers();
    }

    public String getCameraLength() {
        switch (pixelScale) {
            case PS_005: return LONG;
            case PS_015: return SHORT;
            default:     throw new Error();
        }
    }

    public String getCameraColor() {
        if (getInstrumentCentralWavelength() < 2600) {
            return BLUE;
        } else {
            return RED;
        }
    }

    public boolean isXDispUsed() {
        return !xDisp.equals(CrossDispersed.NO);
    }

    public static String getLongCameraName() {
        return LONG;
    }

    public static String getShortCameraName() {
        return SHORT;
    }

    public static String getBlueCameraName() {
        return BLUE;
    }

    public static String getRedCameraName() {
        return RED;
    }

}
