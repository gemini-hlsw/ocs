package edu.gemini.itc.michelle;

import edu.gemini.itc.shared.InstrumentDetails;

/**
 * This class holds the information from the Michelle section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class MichelleParameters implements InstrumentDetails {

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

    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";

    // Data members
    private final String _Filter;  // filters
    private final String _grating; // Grating or null
    private final String _instrumentCentralWavelength;
    private final String _FP_Mask;
    private final String _polarimetry;

    /**
     * Constructs a MichelleParameters from a servlet request
     */
    public MichelleParameters(final String Filter,
                              final String grating,
                              final String instrumentCentralWavelength,
                              final String FP_Mask,
                              final String polarimetry) {
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
        sb.append("Focal Plane Mask: \t " + _FP_Mask + " arcsec slit \n");
        sb.append("\n");
        return sb.toString();
    }
}
