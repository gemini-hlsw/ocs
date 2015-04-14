package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.InstrumentDetails;

/**
 * This class holds the information from the Gnirs section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GnirsParameters implements InstrumentDetails {

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.

    public static final String NO_DISPERSER = "none";
    public static final String G10 = "G10";
    public static final String G32 = "G32";
    public static final String G110 = "G110";

    public static final String LOW_READ_NOISE = "lowNoise";
    public static final String VERY_LOW_READ_NOISE = "verylowNoise";
    public static final String HIGH_READ_NOISE = "highNoise";
    public static final String MED_READ_NOISE = "medNoise";
    public static final String LONG_CAMERA = "0.05";
    public static final String SHORT_CAMERA = "0.15";
    public static final String SLIT0_1 = "slit0.10";
    public static final String SLIT0_15 = "slit0.15";
    public static final String SLIT0_2 = "slit0.20";
    public static final String SLIT0_3 = "slit0.30";
    public static final String SLIT0_45 = "slit0.45";
    public static final String SLIT0_675 = "slit0.675";
    public static final String SLIT1_0 = "slit1.0";
    public static final String SLIT3_0 = "slit3.0";
    public static final String NO_SLIT = "none";
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
    private final String grating; // Grating or null
    private final String camera;
    private final String readNoise;
    private final double instrumentCentralWavelength;
    private final String mask;
    private final String xDisp;

    /**
     * Constructs a GnirsParameters.
     */
    public GnirsParameters(final String camera,
                           final String grating,
                           final String readNoise,
                           final String xDisp,
                           final double instrumentCentralWavelength,
                           final String mask) {
        this.grating = grating;
        this.camera = camera;
        this.xDisp = xDisp;
        this.readNoise = readNoise;
        this.instrumentCentralWavelength = instrumentCentralWavelength * 1000;
        this.mask = mask;
    }

    public String getGrating() {
        return grating;
    }

    public String getReadNoise() {
        return readNoise;
    }

    public String getFocalPlaneMask() {
        return mask;
    }

    public double getInstrumentCentralWavelength() {
        if (!isXDispUsed()) {
            return instrumentCentralWavelength;
        } else {
            return XDISP_CENTRAL_WAVELENGTH;
        }
    }

    public double getUnXDispCentralWavelength() {
        return instrumentCentralWavelength;
    }

    public String getStringSlitWidth() {
        switch (mask) {
            case SLIT0_1:   return "010";
            case SLIT0_15:  return "015";
            case SLIT0_2:   return "020";
            case SLIT0_3:   return "030";
            case SLIT0_45:  return "045";
            case SLIT0_675: return "0675";
            case SLIT1_0:   return "100";
            case SLIT3_0:   return "300";
            default:        return "none";
        }
    }

    public String getCameraLength() {
        switch (camera) {
            case LONG_CAMERA:   return LONG;
            case SHORT_CAMERA:  return SHORT;
            default:            throw new Error();
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
