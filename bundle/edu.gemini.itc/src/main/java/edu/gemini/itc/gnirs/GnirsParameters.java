package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.itc.shared.InstrumentDetails;

import javax.servlet.http.HttpServletRequest;

/**
 * This class holds the information from the Gnirs section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GnirsParameters implements InstrumentDetails {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String INSTRUMENT_GRATING = "instrumentDisperser";
    public static final String INSTRUMENT_CAMERA = "instrumentCamera";
    public static final String INSTRUMENT_CENTRAL_WAVELENGTH = "instrumentCentralWavelength";
    public static final String X_DISP = "xdisp";
    public static final String READ_NOISE = "readNoise";
    public static final String DARK_CURRENT = "darkCurrent";
    public static final String FP_MASK = "instrumentFPMask";
    public static final String INSTRUMENT_LOCATION = "instrumentLocation";


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
    private String _grating; // Grating or null
    private String _camera;
    private String _readNoise;
    private String _darkCurrent;
    private String _instrumentCentralWavelength;
    private String _FP_Mask;
    private String _instrumentLocation;
    private String _cameraLength;
    private String _cameraColor;
    private String _xDisp;

    /**
     * Constructs a GnirsParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public GnirsParameters(HttpServletRequest r) {
        parseServletRequest(r);
    }

    /**
     * Constructs a GnirsParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public GnirsParameters(ITCMultiPartParser p) {
        parseMultipartParameters(p);
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) {
        // Parse the acquisition camera section of the form.

        // Get Grating
        _grating = r.getParameter(INSTRUMENT_GRATING);
        if (_grating == null) {
            ITCParameters.notFoundException(INSTRUMENT_GRATING);
        }

        //Get Camera
        _camera = r.getParameter(INSTRUMENT_CAMERA);
        if (_camera == null) {
            ITCParameters.notFoundException(INSTRUMENT_CAMERA);
        }

        _xDisp = r.getParameter(X_DISP);
        if (_xDisp == null) {
            ITCParameters.notFoundException(X_DISP);
        }

        _readNoise = r.getParameter(READ_NOISE);
        if (_readNoise == null) {
            ITCParameters.notFoundException(READ_NOISE);
        }

        // Get Instrument Central Wavelength
        _instrumentCentralWavelength =
                r.getParameter(INSTRUMENT_CENTRAL_WAVELENGTH);
        if (_instrumentCentralWavelength == null) {
            ITCParameters.notFoundException(
                    INSTRUMENT_CENTRAL_WAVELENGTH);
        }
        if (_instrumentCentralWavelength.equals(" ")) {
            _instrumentCentralWavelength = "0";
        }


        _FP_Mask = r.getParameter(FP_MASK);
        if (_FP_Mask == null) {
            ITCParameters.notFoundException(FP_MASK);
        }
    }

    public void parseMultipartParameters(ITCMultiPartParser p) {
        _grating = p.getParameter(INSTRUMENT_GRATING);
        _camera = p.getParameter(INSTRUMENT_CAMERA);
        _xDisp = p.getParameter(X_DISP);
        _readNoise = p.getParameter(READ_NOISE);
        _instrumentCentralWavelength = p.getParameter(INSTRUMENT_CENTRAL_WAVELENGTH);
        if (_instrumentCentralWavelength.equals(" ")) {
            _instrumentCentralWavelength = "0";
        }
        _FP_Mask = p.getParameter(FP_MASK);
    }

    /**
     * Constructs a GnirsParameters from a test file.
     */
    public GnirsParameters(String camera, String grating, String readNoise, String xDisp, String darkCurrent,
                           String instrumentCentralWavelength, String FP_Mask) {
        _grating = grating;
        _camera = camera;
        _xDisp = xDisp;
        _darkCurrent = darkCurrent;
        _readNoise = readNoise;
        _instrumentCentralWavelength =
                instrumentCentralWavelength;
        _FP_Mask = FP_Mask;
    }

    public String getGrating() {
        return _grating;
    }

    //  Uncommented following section on 2/27/2014 (see REL-480)
    public String getReadNoise() {
        return _readNoise;
    }

    public String getDarkCurrent() {
        return _darkCurrent;
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
            _cameraLength = LONG;
        } else if (_camera.equals(SHORT_CAMERA)) {
            _cameraLength = SHORT;
        } else {
            throw new RuntimeException("Error Camera Not Selected");
        }
        return _cameraLength;
    }

    public String getCameraColor() {
        if (getInstrumentCentralWavelength() < 2600) {
            _cameraColor = BLUE;
        } else {
            _cameraColor = RED;
        }
        return _cameraColor;
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
