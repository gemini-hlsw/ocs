package edu.gemini.itc.gsaoi;

import edu.gemini.itc.shared.InstrumentDetails;

/**
 * This class holds the information from the Gsaoi section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GsaoiParameters implements InstrumentDetails {

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
    private final String _filter;  // filters
    private final String _readMode;

    public GsaoiParameters(final String filter, final String readMode) {
        _filter = filter;
        _readMode = readMode;

        if (_filter.equals("none")) {
            throw new IllegalArgumentException("Must specify a filter or a grism");
        }
    }

    public String getFilter() {
        return _filter;
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
        sb.append("\n");
        return sb.toString();
    }
}
