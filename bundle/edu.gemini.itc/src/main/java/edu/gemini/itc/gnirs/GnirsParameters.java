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

    private static double XDISP_CENTRAL_WAVELENGTH = 1616.85;

    // Data members
    private final String _grating; // Grating or null
    private final String _camera;
    private final String _readNoise;
    private final String _instrumentCentralWavelength;
    private final String _FP_Mask;
    private final String _xDisp;

    /**
     * Constructs a GnirsParameters.
     */
    public GnirsParameters(final String camera,
                           final String grating,
                           final String readNoise,
                           final String xDisp,
                           final String instrumentCentralWavelength,
                           final String FP_Mask) {
        _grating = grating;
        _camera = camera;
        _xDisp = xDisp;
        _readNoise = readNoise;
        _instrumentCentralWavelength = instrumentCentralWavelength;
        _FP_Mask = FP_Mask;
    }

    public String getGrating() {
        return _grating;
    }

    //  Uncommented following section on 2/27/2014 (see REL-480)
    public String getReadNoise() {
        return _readNoise;
    }

    public String getFocalPlaneMask() {
        return _FP_Mask;
    }

    public double getInstrumentCentralWavelength() {
        if (!isXDispUsed()) {
            return new Double(_instrumentCentralWavelength) * 1000;
        } else {
            return XDISP_CENTRAL_WAVELENGTH;
        }
    }

    public double getUnXDispCentralWavelength() {
        return new Double(_instrumentCentralWavelength) * 1000;
    }

    public String getStringSlitWidth() {
        if (_FP_Mask.equals(SLIT0_1))
            return "010";
        else if (_FP_Mask.equals(SLIT0_15))
            return "015";
        else if (_FP_Mask.equals(SLIT0_2))
            return "020";
        else if (_FP_Mask.equals(SLIT0_3))
            return "030";
        else if (_FP_Mask.equals(SLIT0_45))
            return "045";
        else if (_FP_Mask.equals(SLIT0_675))
            return "0675";
        else if (_FP_Mask.equals(SLIT1_0))
            return "100";
        else if (_FP_Mask.equals(SLIT3_0))
            return "300";
        else
            return "none";

    }

    public String getCameraLength() {
        if (_camera.equals(LONG_CAMERA)) {
            return LONG;
        } else if (_camera.equals(SHORT_CAMERA)) {
            return SHORT;
        } else {
            throw new RuntimeException("Error Camera Not Selected");
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
        if (_xDisp.equals(X_DISP_ON)) {
            return true;
        } else {
            return false;
        }
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


    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Grating:\t" + getGrating() + "\n");
        sb.append("Instrument Central Wavelength:\t" + getInstrumentCentralWavelength() + "\n");
        sb.append("Focal Plane Mask: \t " + _FP_Mask + " arcsec slit \n");
        sb.append("\n");
        return sb.toString();
    }
}
