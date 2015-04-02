package edu.gemini.itc.flamingos2;

import edu.gemini.itc.shared.InstrumentDetails;

/**
 * This class holds the information from the Acquisition Camera section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class Flamingos2Parameters implements InstrumentDetails {

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String CLEAR = "Open";

    // Grism names
    public static final String JHGRISM = "JH";
    public static final String HKGRISM = "HK";
    public static final String R3KGRISM = "R3K";
    public static final String NOGRISM = "None";

    // Data members
    private final String _colorFilter;  // U, V, B, ...
    private final String _grism;
    private final String _readNoise;
    private final String _fpMask;

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     */
    public Flamingos2Parameters(final String colorFilter, final String grismName, final String fpMask, final String readNoise) {
        _colorFilter = colorFilter;
        _grism = grismName;
        _fpMask = fpMask;
        _readNoise = readNoise;
    }

    public String getColorFilter() {
        return _colorFilter;
    }

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Color Filter:\t" + getColorFilter() + "\n");
        sb.append("\n");
        return sb.toString();
    }

    public String getFPMask() {
        return _fpMask;
    }

    public String getReadNoise() {
        return _readNoise;
    }

    public String getGrism() {
        return _grism;
    }
}
