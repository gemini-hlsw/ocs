package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.spModel.core.Wavelength;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.*;

/**
 * This class holds the information from the Gnirs section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GnirsParameters implements InstrumentDetails {

    public static final String GNIRS = "gnirs";

    public static final String X_DISP_ON = "yes";
    public static final String X_DISP_OFF = "no";

    public static final String BLUE = "BC";
    public static final String RED = "RC";

    public static final String LONG = "L";
    public static final String SHORT = "S";

    public static final double LONG_CAMERA_SCALE_FACTOR = 3.0;

    private static final double XDISP_CENTRAL_WAVELENGTH = 1616.85;

    // Data members
    private final Disperser grating;
    private final PixelScale camera;
    private final ReadMode readMode;
    private final Wavelength instrumentCentralWavelength;
    private final SlitWidth mask;
    private final String xDisp;

    /**
     * Constructs a GnirsParameters.
     */
    public GnirsParameters(final PixelScale camera,
                           final Disperser grating,
                           final ReadMode readMode,
                           final String xDisp,
                           final Wavelength instrumentCentralWavelength,
                           final SlitWidth mask) {
        this.grating = grating;
        this.camera = camera;
        this.xDisp = xDisp;
        this.readMode = readMode;
        this.instrumentCentralWavelength = instrumentCentralWavelength;
        this.mask = mask;
    }

    public Disperser getGrating() {
        return grating;
    }

    public ReadMode getReadMode() {
        return readMode;
    }

    public SlitWidth getFocalPlaneMask() {
        return mask;
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

    public String getStringSlitWidth() {
        switch (mask) {
            case SW_1: return "010";
            case SW_2: return "015";
            case SW_3: return "020";
            case SW_4: return "030";
            case SW_5: return "045";
            case SW_6: return "0675";
            case SW_7: return "100";
            case SW_8: return "300";
            default:   throw new Error();
        }
    }

    public String getCameraLength() {
        switch (camera) {
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
        return xDisp.equals(X_DISP_ON);
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
