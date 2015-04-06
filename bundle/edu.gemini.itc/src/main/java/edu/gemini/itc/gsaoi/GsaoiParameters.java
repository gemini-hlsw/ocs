package edu.gemini.itc.gsaoi;

import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;

import javax.servlet.http.HttpServletRequest;

/**
 * This class holds the information from the Gsaoi section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GsaoiParameters implements InstrumentDetails {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String INSTRUMENT_FILTER = "instrumentFilter";
    public static final String INSTRUMENT_CAMERA = "instrumentCamera";
    public static final String READ_MODE = "readMode";

    public static final String BRIGHT_OBJECTS_READ_MODE = "bright";
    public static final String FAINT_OBJECTS_READ_MODE = "faint";
    public static final String VERY_FAINT_OBJECTS_READ_MODE = "veryFaint";

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final int J = 0;
    public static final int H = 1;
    public static final int K = 2;
    public static final int L = 3;
    public static final int M = 4;

    // Data members
    private String _filter;  // filters
    private String _camera;
    private String _readMode;

    /**
     * Constructs a GsaoiParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public GsaoiParameters(HttpServletRequest r) {
        parseServletRequest(r);
    }

    /**
     * Constructs a GsaoiParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */
    public GsaoiParameters(ITCMultiPartParser p) {
        parseMultipartParameters(p);
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) {

        // Get Broad Band filter
        _filter = r.getParameter(INSTRUMENT_FILTER);
        if (_filter == null) {
            ITCParameters.notFoundException(INSTRUMENT_FILTER);
        }

        if (_filter.equals("none")) {
            throw new IllegalArgumentException("Must specify a filter");
        }

        // Get Camera Used
        _camera = r.getParameter(INSTRUMENT_CAMERA);
        if (_camera == null) {
            ITCParameters.notFoundException(INSTRUMENT_CAMERA);
        }

        //Get read mode
        _readMode = r.getParameter(READ_MODE);
        if (_readMode == null) {
            ITCParameters.notFoundException(READ_MODE);
        }
    }

    public void parseMultipartParameters(ITCMultiPartParser p) {
        _filter = p.getParameter(INSTRUMENT_FILTER);
        if (_filter.equals("none")) {
            throw new IllegalArgumentException("Must specify a filter or a grism");
        }
        _camera = p.getParameter(INSTRUMENT_CAMERA);
        _readMode = p.getParameter(READ_MODE);
    }

    public GsaoiParameters(String filter,
                           String camera,
                           String readMode) {
        _filter = filter;
        _camera = camera;
        _readMode = readMode;
    }

    public String getFilter() {
        return _filter;
    }

    public String getCamera() {
        return _camera;
    }

    public String getReadMode() {
        return _readMode;
    }


    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Filter:\t" + getFilter() + "\n");
//        sb.append("Camera:\t" + getCamera() + "\n");
        sb.append("\n");
        return sb.toString();
    }
}
