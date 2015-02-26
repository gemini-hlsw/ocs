package edu.gemini.itc.trecs;

import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;

import javax.servlet.http.HttpServletRequest;

/**
 * This class holds the information from the Trecs section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class TRecsParameters extends ITCParameters {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String INSTRUMENT_FILTER = "instrumentFilter";
    public static final String INSTRUMENT_WINDOW = "instrumentWindow";
    public static final String INSTRUMENT_GRATING = "instrumentDisperser";
    public static final String INSTRUMENT_CENTRAL_WAVELENGTH = "instrumentCentralWavelength";
    public static final String WELL_DEPTH = "wellDepth";
    public static final String FP_MASK = "instrumentFPMask";
    public static final String SPAT_BINNING = "spatBinning";
    public static final String SPEC_BINNING = "specBinning";

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

    public static final int LORES10 = 0;
    public static final int LORES20 = 1;
    public static final int HIRES10 = 2;

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
    private String _Filter;  // filters
    private String _InstrumentWindow;
    private String _grating; // Grating or null
    private String _instrumentCentralWavelength;
    private String _FP_Mask;
    private String _spatBinning;
    private String _specBinning;

    /**
     * Constructs a TrecsParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public TRecsParameters(HttpServletRequest r) {
        parseServletRequest(r);
    }

    /**
     * Constructs a TRecsParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public TRecsParameters(ITCMultiPartParser p) {
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

        // Get instrument Window
        _InstrumentWindow = r.getParameter(INSTRUMENT_WINDOW);
        if (_InstrumentWindow == null) {
            ITCParameters.notFoundException(INSTRUMENT_WINDOW);
        }

        // Get Grating
        _grating = r.getParameter(INSTRUMENT_GRATING);
        if (_grating == null) {
            ITCParameters.notFoundException(INSTRUMENT_GRATING);
        }

        _spatBinning = r.getParameter(SPAT_BINNING);
        if (_spatBinning == null) {
            ITCParameters.notFoundException(SPAT_BINNING);
        }

        _specBinning = r.getParameter(SPEC_BINNING);
        if (_specBinning == null) {
            ITCParameters.notFoundException(SPEC_BINNING);
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

    }

    public void parseMultipartParameters(ITCMultiPartParser p) {
        _Filter = p.getParameter(INSTRUMENT_FILTER);
        _InstrumentWindow = p.getParameter(INSTRUMENT_WINDOW);
        _grating = p.getParameter(INSTRUMENT_GRATING);
        _spatBinning = p.getParameter(SPAT_BINNING);
        _specBinning = p.getParameter(SPEC_BINNING);
        _instrumentCentralWavelength = p.getParameter(INSTRUMENT_CENTRAL_WAVELENGTH);
        if (_instrumentCentralWavelength.equals(" ")) {
            _instrumentCentralWavelength = "0";
        }
        _FP_Mask = p.getParameter(FP_MASK);
    }

    /**
     * Constructs a TRecsParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public TRecsParameters(String Filter,
                           String instrumentWindow,
                           String grating,
                           String instrumentCentralWavelength,
                           String FP_Mask,
                           String spatBinning,
                           String specBinning) {
        _Filter = Filter;
        _InstrumentWindow = instrumentWindow;
        _grating = grating;
        _instrumentCentralWavelength = instrumentCentralWavelength;
        _FP_Mask = FP_Mask;
        _spatBinning = spatBinning;
        _specBinning = specBinning;

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
        return (new Double(_instrumentCentralWavelength)) * 1000; //Convert um to nm
    }

    public int getSpectralBinning() {//System.out.println(new Integer(_specBinning).intValue());
        return new Integer(_specBinning);
    }

    public int getSpatialBinning() {//System.out.println(new Integer(_spatBinning).intValue());
        return new Integer(_spatBinning);
    }

    public double getFPMask() {
        //if (_FP_Mask.equals(NOSLIT)) return null;
        if (_FP_Mask.equals(SLIT0_21)) return 0.21;
        else if (_FP_Mask.equals(SLIT0_26)) return 0.26;
        else if (_FP_Mask.equals(SLIT0_31)) return 0.31;
        else if (_FP_Mask.equals(SLIT0_36)) return 0.36;
        else if (_FP_Mask.equals(SLIT0_66)) return 0.66;
        else if (_FP_Mask.equals(SLIT0_72)) return 0.72;
        else if (_FP_Mask.equals(SLIT1_32)) return 1.32;
        else return -1.0;
    }

    public String getStringSlitWidth() {
        if (_FP_Mask.equals(SLIT0_21)) return "021";
        else if (_FP_Mask.equals(SLIT0_26)) return "026";
        else if (_FP_Mask.equals(SLIT0_31)) return "031";
        else if (_FP_Mask.equals(SLIT0_26)) return "036";
        else if (_FP_Mask.equals(SLIT0_66)) return "066";
        else if (_FP_Mask.equals(SLIT0_72)) return "072";
        else if (_FP_Mask.equals(SLIT1_32)) return "132";
        else return "none";

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
