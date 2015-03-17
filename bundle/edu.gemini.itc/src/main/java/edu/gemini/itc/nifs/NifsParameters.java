package edu.gemini.itc.nifs;

import edu.gemini.itc.service.InstrumentDetails;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;

import javax.servlet.http.HttpServletRequest;

/**
 * This class holds the information from the Nifs section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class NifsParameters implements InstrumentDetails {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String INSTRUMENT_FILTER = "instrumentFilter";
    public static final String INSTRUMENT_GRATING = "instrumentDisperser";
    public static final String INSTRUMENT_CAMERA = "instrumentCamera";
    public static final String INSTRUMENT_CENTRAL_WAVELENGTH = "instrumentCentralWavelength";
    //public static final String X_DISP = "xdisp";
    public static final String READ_NOISE = "readNoise";
    public static final String DARK_CURRENT = "darkCurrent";
    public static final String FP_MASK = "instrumentFPMask";
    public static final String IFU_METHOD = "ifuMethod";
    public static final String SINGLE_IFU = "singleIFU";
    public static final String RADIAL_IFU = "radialIFU";
    public static final String SUMMED_APERTURE_IFU = "summedApertureIFU";
    public static final String INSTRUMENT_LOCATION = "instrumentLocation";


    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.

    public static final String NO_DISPERSER = "none";
    public static final String Z_G5602 = "z_G5602";
    public static final String J_G5603 = "j_G5603";
    public static final String H_G5604 = "h_G5604";
    public static final String K_G5605 = "k_G5605";
    public static final String KS_G5606 = "ks_G5606";
    public static final String KL_G5607 = "kl_G5607";

    //filters
    public static final String ZJ_G0601 = "ZJ_G0601";
    public static final String HJ_G0602 = "HJ_G0602";
    public static final String HK_G0603 = "HK_G0603";

    public static final String LOW_READ_NOISE = "lowNoise";
    public static final String VERY_LOW_READ_NOISE = "verylowNoise";
    public static final String HIGH_READ_NOISE = "highNoise";
    public static final String MED_READ_NOISE = "medNoise";
    public static final String IFU = "ifu";
    public static final String IFU_OFFSET = "ifuOffset";
    public static final String IFU_MIN_OFFSET = "ifuMinOffset";
    public static final String IFU_MAX_OFFSET = "ifuMaxOffset";
    public static final String IFU_NUM_X = "ifuNumX";
    public static final String IFU_NUM_Y = "ifuNumY";
    public static final String IFU_CENTER_X = "ifuCenterX";
    public static final String IFU_CENTER_Y = "ifuCenterY";
    public static final String NO_SLIT = "none";
    public static final String NIFS = "nifs";

    // Data members
    private String _Filter;
    private String _grating; // Grating or null
    private String _readNoise;
    private String _darkCurrent;
    private String _instrumentCentralWavelength;
    private String _FP_Mask;
    private String _IFUMethod;
    private String _IFUOffset;
    private String _IFUMinOffset;
    private String _IFUMaxOffset;
    private String _IFUnumX;
    private String _IFUnumY;
    private String _IFUcenterX;
    private String _IFUcenterY;

    private String _instrumentLocation;

    /**
     * Constructs a NifsParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public NifsParameters(HttpServletRequest r) {
        parseServletRequest(r);
    }

    /**
     * Constructs a NifsParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public NifsParameters(ITCMultiPartParser p) {
        parseMultipartParameters(p);
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) {
        // Parse the acquisition camera section of the form.
        // Get filter
        _Filter = r.getParameter(INSTRUMENT_FILTER);
        if (_Filter == null) {
            ITCParameters.notFoundException(INSTRUMENT_FILTER);
        }
        // Get Grating
        _grating = r.getParameter(INSTRUMENT_GRATING);
        if (_grating == null) {
            ITCParameters.notFoundException(INSTRUMENT_GRATING);
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

        _IFUMethod = r.getParameter(IFU_METHOD);
        if (_IFUMethod == null) {
            if (_FP_Mask.equals(IFU))
                ITCParameters.notFoundException("a value for " + IFU_METHOD + ".  Please either deselect the\n" +
                        " IFU or select an IFU Method(Single or Radial). ");
        } else {

            if (_IFUMethod.equals(SINGLE_IFU)) {
                _IFUOffset = r.getParameter(IFU_OFFSET);
                if (_IFUOffset == null) {
                    ITCParameters.notFoundException(IFU_OFFSET);
                }
            } else if (_IFUMethod.equals(RADIAL_IFU)) {
                _IFUMinOffset = r.getParameter(IFU_MIN_OFFSET);
                if (_IFUMinOffset == null) {
                    ITCParameters.notFoundException(IFU_MIN_OFFSET);
                }

                _IFUMaxOffset = r.getParameter(IFU_MAX_OFFSET);
                if (_IFUMaxOffset == null) {
                    ITCParameters.notFoundException(IFU_MAX_OFFSET);
                }

            } else if (_IFUMethod.equals(SUMMED_APERTURE_IFU)) {
                _IFUnumX = r.getParameter(IFU_NUM_X);
                if (_IFUnumX == null) {
                    ITCParameters.notFoundException(IFU_NUM_X);
                }

                _IFUnumY = r.getParameter(IFU_NUM_Y);
                if (_IFUnumY == null) {
                    ITCParameters.notFoundException(IFU_NUM_Y);
                }
                _IFUcenterX = r.getParameter(IFU_CENTER_X);
                if (_IFUcenterX == null) {
                    ITCParameters.notFoundException(IFU_CENTER_X);
                }

                _IFUcenterY = r.getParameter(IFU_CENTER_Y);
                if (_IFUcenterY == null) {
                    ITCParameters.notFoundException(IFU_CENTER_Y);
                }

            } else
                ITCParameters.notFoundException(" a correct value for the IFU Parameters. ");
        }


    }

    public void parseMultipartParameters(ITCMultiPartParser p) {
        _Filter = p.getParameter(INSTRUMENT_FILTER);
        _grating = p.getParameter(INSTRUMENT_GRATING);
        _readNoise = p.getParameter(READ_NOISE);
        _instrumentCentralWavelength = p.getParameter(INSTRUMENT_CENTRAL_WAVELENGTH);
        if (_instrumentCentralWavelength.equals(" ")) {
            _instrumentCentralWavelength = "0";
        }
        _FP_Mask = p.getParameter(FP_MASK);
        if (_FP_Mask.equals(IFU)) {
            _IFUMethod = p.getParameter(IFU_METHOD);
            if (_IFUMethod.equals(SINGLE_IFU)) {
                _IFUOffset = p.getParameter(IFU_OFFSET);
            } else if (_IFUMethod.equals(RADIAL_IFU)) {
                _IFUMinOffset = p.getParameter(IFU_MIN_OFFSET);
                _IFUMaxOffset = p.getParameter(IFU_MAX_OFFSET);
            } else if (_IFUMethod.equals(SUMMED_APERTURE_IFU)) {
                _IFUnumX = p.getParameter(IFU_NUM_X);
                _IFUnumY = p.getParameter(IFU_NUM_Y);
                _IFUcenterX = p.getParameter(IFU_CENTER_X);
                _IFUcenterY = p.getParameter(IFU_CENTER_Y);
            } else
                ITCParameters.notFoundException(" a correct value for the IFU Parameters. ");
        }
    }

    /**
     * Constructs a NifsParameters from a test file.
     */
    public NifsParameters(String Filter, String grating, String readNoise, String darkCurrent, String instrumentCentralWavelength, String FP_Mask, String IFUMethod, String IFUOffset, String IFUMinOffset, String IFUMaxOffset, String IFUNumX, String IFUNumY, String IFUCenterX, String IFUCenterY) {

        _Filter = Filter;
        _grating = grating;
        _darkCurrent = darkCurrent;
        _readNoise = readNoise;
        _instrumentCentralWavelength =
                instrumentCentralWavelength;
        _FP_Mask = FP_Mask;
        _IFUMethod = IFUMethod;
        _IFUOffset = IFUOffset;
        _IFUMinOffset = IFUMinOffset;
        _IFUMaxOffset = IFUMaxOffset;
        _IFUnumX = IFUNumX;
        _IFUnumY = IFUNumY;
        _IFUcenterX = IFUCenterX;
        _IFUcenterY = IFUCenterY;
    }

    public String getFilter() {
        return _Filter;
    }

    public String getGrating() {
        return _grating;
    }

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
        return new Double(_instrumentCentralWavelength).doubleValue() * 1000;
    }

    public double getUnXDispCentralWavelength() {
        return new Double(_instrumentCentralWavelength).doubleValue() * 1000;
    }


    public String getIFUMethod() {
        return _IFUMethod;
    }

    public double getIFUOffset() {
        return new Double(_IFUOffset).doubleValue();
    }

    public double getIFUMinOffset() {
        return new Double(_IFUMinOffset).doubleValue();
    }

    public double getIFUMaxOffset() {
        return new Double(_IFUMaxOffset).doubleValue();
    }


    public int getIFUNumX() {
        return new Integer(_IFUnumX).intValue();
    }

    public int getIFUNumY() {
        return new Integer(_IFUnumY).intValue();
    }

    public double getIFUCenterX() {
        return new Double(_IFUcenterX).doubleValue();
    }

    public double getIFUCenterY() {
        return new Double(_IFUcenterY).doubleValue();
    }

    public double getFPMask() {
        if (_FP_Mask.equals(IFU))  //**** Might need to be changed.!!!!
            return 0.15;
        else
            return -1.0;
    }

    public String getStringSlitWidth() {
        if (_FP_Mask.equals(IFU))
            return "IFU";
        else
            return "none";

    }


    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Grating:\t" + getGrating() + "\n");
        sb.append("Instrument Central Wavelength:\t" +
                getInstrumentCentralWavelength() + "\n");
        sb.append("\n");
        return sb.toString();
    }
}
