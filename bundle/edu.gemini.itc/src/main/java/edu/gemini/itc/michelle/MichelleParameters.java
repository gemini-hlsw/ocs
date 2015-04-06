package edu.gemini.itc.michelle;

import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;

import javax.servlet.http.HttpServletRequest;

/**
 * This class holds the information from the Michelle section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class MichelleParameters implements InstrumentDetails {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String INSTRUMENT_FILTER = "instrumentFilter";
    public static final String INSTRUMENT_GRATING = "instrumentDisperser";
    public static final String INSTRUMENT_CENTRAL_WAVELENGTH = "instrumentCentralWavelength";
    public static final String WELL_DEPTH = "wellDepth";
    public static final String FP_MASK = "instrumentFPMask";

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.

    public static final String KBR = "KBr";
    public static final String KRS5 = "KRS5";

    public static final String LOW_N = "lowN";
    public static final String LOW_Q = "lowQ";
    public static final String MED_N1 = "medN1";
    public static final String MED_N2 = "medN2";
    public static final String ECHELLE_N = "EchelleN";
    public static final String ECHELLE_Q = "EchelleQ";

    public static final int LOWN = 0;
    public static final int LOWQ = 1;
    public static final int MEDN1 = 2;
    public static final int MEDN2 = 3;
    public static final int ECHELLEN = 4;
    public static final int ECHELLEQ = 5;

    public static final String LOW_READ_NOISE = "lowNoise";
    public static final String HIGH_READ_NOISE = "highNoise";
    public static final String HIGH_WELL_DEPTH = "highWell";
    public static final String SLIT0_19 = "slit0.19";
    public static final String SLIT0_38 = "slit0.38";
    public static final String SLIT0_57 = "slit0.57";
    public static final String SLIT0_76 = "slit0.76";
    public static final String SLIT1_52 = "slit1.52";
    public static final String IFU = "ifu";
    public static final String NO_SLIT = "none";

    public static final String WIRE_GRID = "wire_grid";
    public static final String POLARIMETRY = "polarimetry";

    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";

    // Data members
    private String _Filter;  // filters
    private String _grating; // Grating or null
    private String _instrumentCentralWavelength;
    private String _FP_Mask;
    private String _polarimetry;

    /**
     * Constructs a MichelleParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public MichelleParameters(HttpServletRequest r) {
        parseServletRequest(r);
    }

    /**
     * Constructs a MichelleParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public MichelleParameters(ITCMultiPartParser p) {
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

        // Get Instrument Central Wavelength
        _instrumentCentralWavelength =
                r.getParameter(INSTRUMENT_CENTRAL_WAVELENGTH);
        if (_instrumentCentralWavelength == null) {
            ITCParameters.notFoundException(
                    INSTRUMENT_CENTRAL_WAVELENGTH);
        }
        if (_instrumentCentralWavelength.equals(" ")) {
            //ITCParameters.notFoundException(
            //   		"the Spectrum central wavelength.  Please enter a value in the Instrument \n"+
            //   		"optical Properties section and resubmit the form.");
            _instrumentCentralWavelength = "0";
        }

        _FP_Mask = r.getParameter(FP_MASK);
        if (_FP_Mask == null) {
            ITCParameters.notFoundException(FP_MASK);
        }

    }

    public void parseMultipartParameters(ITCMultiPartParser p) {
        _Filter = p.getParameter(INSTRUMENT_FILTER);
        _grating = p.getParameter(INSTRUMENT_GRATING);
        _instrumentCentralWavelength = p.getParameter(INSTRUMENT_CENTRAL_WAVELENGTH);
        if (_instrumentCentralWavelength.equals(" ")) {
            _instrumentCentralWavelength = "0";
        }
        _FP_Mask = p.getParameter(FP_MASK);
        _polarimetry = p.getParameter(POLARIMETRY);
    }

    /**
     * Constructs a MichelleParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public MichelleParameters(String Filter,
                              String grating,
                              String instrumentCentralWavelength,
                              String FP_Mask,
                              String polarimetry) {
        _Filter = Filter;
        _grating = grating;
        _instrumentCentralWavelength = instrumentCentralWavelength;
        _FP_Mask = FP_Mask;
        _polarimetry = polarimetry;

    }

    public String getFilter() {
        return _Filter;
    }

    public String getGrating() {
        return _grating;
    }

    public String getFocalPlaneMask() {
        return _FP_Mask;
    }

    public double getInstrumentCentralWavelength() {
        return (new Double(_instrumentCentralWavelength)) * 1000;
    }

    public double getFPMask() {
        //if (_FP_Mask.equals(NOSLIT)) return null;
        if (_FP_Mask.equals(SLIT0_19))
            return 0.19;
        else if (_FP_Mask.equals(SLIT0_38))
            return 0.38;
        else if (_FP_Mask.equals(SLIT0_57))
            return 0.57;
        else if (_FP_Mask.equals(SLIT0_76))
            return 0.76;
        else if (_FP_Mask.equals(SLIT1_52))
            return 1.52;
        else
            return -1.0;
    }

    public String getStringSlitWidth() {
        if (_FP_Mask.equals(SLIT0_19))
            return "019";
        else if (_FP_Mask.equals(SLIT0_38))
            return "038";
        else if (_FP_Mask.equals(SLIT0_57))
            return "057";
        else if (_FP_Mask.equals(SLIT0_76))
            return "076";
        else if (_FP_Mask.equals(SLIT1_52))
            return "152";
        else
            return "none";

    }

    public boolean polarimetryIsUsed() {
        return _polarimetry.equals(ENABLED);
    }


    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Filter:\t" + getFilter() + "\n");
        sb.append("Grating:\t" + getGrating() + "\n");
        sb.append("Instrument Central Wavelength:\t" +
                getInstrumentCentralWavelength() + "\n");
        sb.append("Focal Plane Mask: \t " + getFPMask() + " arcsec slit \n");
        sb.append("\n");
        return sb.toString();
    }
}
