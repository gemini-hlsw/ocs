package edu.gemini.itc.acqcam;

import edu.gemini.itc.shared.InstrumentDetails;

/**
 * This class holds the information from the Acquisition Camera section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class AcquisitionCamParameters implements InstrumentDetails {

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String CLEAR = "clear";
    public static final String NDA = "NDa";
    public static final String NDB = "NDb";
    public static final String NDC = "NDc";
    public static final String NDD = "NDd";

    // Data members
    private final String _colorFilter;  // U, V, B, ...
    private final String _ndFilter;  // NDa, NDb, ...  or null for clear

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     */
    public AcquisitionCamParameters(final String colorFilter, final String ndFilter) {
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
