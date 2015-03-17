package edu.gemini.itc.acqcam;

import edu.gemini.itc.service.InstrumentDetails;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;

import javax.servlet.http.HttpServletRequest;


/**
 * This class holds the information from the Acquisition Camera section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class AcquisitionCamParameters implements InstrumentDetails {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String INSTRUMENT_FILTER = "instrumentFilter";
    public static final String INSTRUMENT_ND_FILTER = "instrumentNDFilter";

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String CLEAR = "clear";
    public static final String NDA = "NDa";
    public static final String NDB = "NDb";
    public static final String NDC = "NDc";
    public static final String NDD = "NDd";

    // Data members
    private String _colorFilter;  // U, V, B, ...
    private String _ndFilter;  // NDa, NDb, ...  or null for clear

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public AcquisitionCamParameters(HttpServletRequest r) {
        parseServletRequest(r);
    }

    /**
     * Constructs a AcquisitionCamParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public AcquisitionCamParameters(ITCMultiPartParser p) {
        parseMultipartParameters(p);
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) {
        // Parse the acquisition camera section of the form.

        // Get color filter
        _colorFilter = r.getParameter(INSTRUMENT_FILTER);
        if (_colorFilter == null) {
            ITCParameters.notFoundException(INSTRUMENT_FILTER);
        }

        // Get ND filter
        _ndFilter = r.getParameter(INSTRUMENT_ND_FILTER);
        if (_ndFilter == null) {
            ITCParameters.notFoundException(INSTRUMENT_ND_FILTER);
        }
    }

    /**
     * Parse Parameters from a multipart servlet request
     */
    public void parseMultipartParameters(ITCMultiPartParser p) {
        _colorFilter = p.getParameter(INSTRUMENT_FILTER);
        _ndFilter = p.getParameter(INSTRUMENT_ND_FILTER);
    }

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public AcquisitionCamParameters(String colorFilter,
                                    String ndFilter) {
        _colorFilter = colorFilter;
        _ndFilter = ndFilter;
    }

    public String getColorFilter() {
        return _colorFilter;
    }

    public String getNDFilter() {
        return _ndFilter;
    }

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Color Filter:\t" + getColorFilter() + "\n");
        sb.append("ND Filter:\t" + getNDFilter() + "\n");
        sb.append("\n");
        return sb.toString();
    }
}
