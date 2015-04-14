package edu.gemini.itc.trecs;

import edu.gemini.itc.shared.InstrumentDetails;

/**
 * This class holds the information from the Trecs section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class TRecsParameters implements InstrumentDetails {

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    //Windows
    public static final String KBR = "KBr";
    public static final String KRS5 = "KRS5";
    public static final String ZNSE = "ZnSe";
    //Gratings
    public static final String LORES10_G5401 = "LoRes-10";
    public static final String LORES20_G5402 = "LoRes-20";
    public static final String HIRES10_G5403 = "HiRes-10";

    //Filters
    public static final String F_117 = "f117";
    public static final String F_ARIII = "ArIII";
    public static final String F_NEII = "NeII";
    public static final String F_NEIICONT = "NeIIcont";
    public static final String F_PAH113 = "PAH11.3";
    public static final String F_PAH86 = "PAH8.6";
    public static final String F_QA = "Qa";
    public static final String F_Qb = "Qb";
    public static final String F_QSHORT = "Qshort";
    public static final String F_QWIDE = "Q";
    public static final String F_SIV = "SIV";
    public static final String F_Si1 = "Si-1";
    public static final String F_Si2 = "Si-2";
    public static final String F_Si3 = "Si-3";
    public static final String F_Si4 = "Si-4";
    public static final String F_Si5 = "Si-5";
    public static final String F_Si6 = "Si-6";
    public static final String F_K = "K";
    public static final String F_L = "L";
    public static final String F_M = "M";
    public static final String F_N = "N";

    public static final String NO_DISPERSER = "none";
    public static final String LOW_READ_NOISE = "lowNoise";
    public static final String HIGH_READ_NOISE = "highNoise";
    public static final String HIGH_WELL_DEPTH = "highWell";
    public static final String LOW_WELL_DEPTH = "lowWell";
    public static final String SLIT0_21 = "slit0.21";
    public static final String SLIT0_26 = "slit0.26";
    public static final String SLIT0_31 = "slit0.31";
    public static final String SLIT0_36 = "slit0.36";
    public static final String SLIT0_66 = "slit0.66";
    public static final String SLIT0_72 = "slit0.72";
    public static final String SLIT1_32 = "slit1.32";
    public static final String IFU = "ifu";
    public static final String NO_SLIT = "none";

    // Data members
    private final String _Filter;  // filters
    private final String _InstrumentWindow;
    private final String _grating; // Grating or null
    private final double _instrumentCentralWavelength;
    private final String _FP_Mask;

    /**
     * Constructs a TRecsParameters from a servlet request
     */
    public TRecsParameters(final String Filter,
                           final String instrumentWindow,
                           final String grating,
                           final double instrumentCentralWavelength,
                           final String FP_Mask) {
        _Filter = Filter;
        _InstrumentWindow = instrumentWindow;
        _grating = grating;
        _instrumentCentralWavelength = instrumentCentralWavelength * 1000; // convert to [nm]
        _FP_Mask = FP_Mask;
    }

    public String getFilter() {
        return _Filter;
    }

    public String getInstrumentWindow() {
        return _InstrumentWindow;
    }

    public String getGrating() {
        return _grating;
    }

    public String getFocalPlaneMask() {
        return _FP_Mask;
    }

    public double getInstrumentCentralWavelength() {
        return _instrumentCentralWavelength;
    }

}
