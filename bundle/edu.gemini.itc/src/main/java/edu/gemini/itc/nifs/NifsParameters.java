package edu.gemini.itc.nifs;

import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;

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
    public static final String READ_NOISE = "readNoise";
    public static final String DARK_CURRENT = "darkCurrent";
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
     * Constructs a NifsParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public NifsParameters(ITCMultiPartParser p) {
        _Filter = p.getParameter(INSTRUMENT_FILTER);
        _grating = p.getParameter(INSTRUMENT_GRATING);
        _readNoise = p.getParameter(READ_NOISE);
        _instrumentCentralWavelength = p.getParameter(INSTRUMENT_CENTRAL_WAVELENGTH);
        if (_instrumentCentralWavelength.equals(" ")) {
            _instrumentCentralWavelength = "0";
        }

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

    /**
     * Constructs a NifsParameters from a test file.
     */
    public NifsParameters(String Filter, String grating, String readNoise, String darkCurrent, String instrumentCentralWavelength, String IFUMethod, String IFUOffset, String IFUMinOffset, String IFUMaxOffset, String IFUNumX, String IFUNumY, String IFUCenterX, String IFUCenterY) {

        _Filter = Filter;
        _grating = grating;
        _darkCurrent = darkCurrent;
        _readNoise = readNoise;
        _instrumentCentralWavelength = instrumentCentralWavelength;
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

    public double getInstrumentCentralWavelength() {
        return new Double(_instrumentCentralWavelength) * 1000;
    }

    public double getUnXDispCentralWavelength() {
        return new Double(_instrumentCentralWavelength) * 1000;
    }


    public String getIFUMethod() {
        return _IFUMethod;
    }

    public double getIFUOffset() {
        return new Double(_IFUOffset);
    }

    public double getIFUMinOffset() {
        return new Double(_IFUMinOffset);
    }

    public double getIFUMaxOffset() {
        return new Double(_IFUMaxOffset);
    }


    public int getIFUNumX() {
        return new Integer(_IFUnumX);
    }

    public int getIFUNumY() {
        return new Integer(_IFUnumY);
    }

    public double getIFUCenterX() {
        return new Double(_IFUcenterX);
    }

    public double getIFUCenterY() {
        return new Double(_IFUcenterY);
    }

    public double getFPMask() {
        return 0.15;
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
