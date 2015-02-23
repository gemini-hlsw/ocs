package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;

import javax.servlet.http.HttpServletRequest;

/**
 * This class holds the information from the Gmos section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GmosParameters extends ITCParameters {
    public static final String INSTRUMENT_FILTER = "instrumentFilter";
    public static final String INSTRUMENT_GRATING = "instrumentDisperser";
    public static final String INSTRUMENT_CENTRAL_WAVELENGTH = "instrumentCentralWavelength";
    public static final String FP_MASK = "instrumentFPMask";
    public static final String SPAT_BINNING = "spatBinning";
    public static final String SPEC_BINNING = "specBinning";
    public static final String IFU_METHOD = "ifuMethod";
    public static final String SINGLE_IFU = "singleIFU";
    public static final String RADIAL_IFU = "radialIFU";
    public static final String INSTRUMENT_LOCATION = "instrumentLocation";

    //Temporary selection for either the old or newer Hamamatsu CCD's
    //Will be removed in semester 2010B
    public static final String CCD_TYPE = "CCDtype";

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String R600_G5304 = "R600_G5304";
    public static final String B1200_G5301 = "B1200_G5301";
    public static final String R150_G5306 = "R150_G5306";
    public static final String B600_G5303 = "B600_G5303";
    public static final String R400_G5305 = "R400_G5305";
    public static final String R831_G5302 = "R831_G5302";

    public static final int R600 = 3;
    public static final int B1200 = 0;
    public static final int R150 = 5;
    public static final int B600 = 2;
    public static final int R400 = 4;
    public static final int R831 = 1;

    public static final String NO_DISPERSER = "none";
    public static final String G_G0301 = "g_G0301";
    public static final String R_G0303 = "r_G0303";
    public static final String I_G0302 = "i_G0302";
    public static final String Z_G0304 = "z_G0304";
    public static final String GG455 = "gg455";
    public static final String OG515 = "og515";
    public static final String RG610 = "rg610";
    public static final String SLIT0_25 = "slit0.25";
    public static final String SLIT0_5 = "slit0.5";
    public static final String SLIT0_75 = "slit0.75";
    public static final String SLIT1_0 = "slit1.0";
    public static final String SLIT1_5 = "slit1.5";
    public static final String SLIT2_0 = "slit2.0";
    public static final String SLIT5_0 = "slit5.0";
    public static final String IFU = "ifu";
    public static final String IFU_OFFSET = "ifuOffset";
    public static final String IFU_MIN_OFFSET = "ifuMinOffset";
    public static final String IFU_MAX_OFFSET = "ifuMaxOffset";
    public static final String NO_SLIT = "none";
    public static final String GMOS_NORTH = "gmosNorth";
    public static final String GMOS_SOUTH = "gmosSouth";

    // Data members
    private final String _Filter;  // filters
    private final String _grating; // Grating or null
    private String _instrumentCentralWavelength;
    private final String _FP_Mask;
    private final String _spatBinning;
    private final String _specBinning;
    private String _IFUMethod;
    private String _IFUOffset;
    private String _IFUMinOffset;
    private String _IFUMaxOffset;
    private final String _instrumentLocation;
    private final String _CCDtype;

    /**
     * Constructs a GmosParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public GmosParameters(ITCMultiPartParser p) {
        _Filter = p.getParameter(INSTRUMENT_FILTER);
        _grating = p.getParameter(INSTRUMENT_GRATING);
        _spatBinning = p.getParameter(SPAT_BINNING);
        _specBinning = p.getParameter(SPEC_BINNING);
        _CCDtype = p.getParameter(CCD_TYPE);
        _instrumentCentralWavelength = p.getParameter(INSTRUMENT_CENTRAL_WAVELENGTH);
        if (_instrumentCentralWavelength.equals(" ")) {
            _instrumentCentralWavelength = "0";
        }
        _FP_Mask = p.getParameter(FP_MASK);
        if (_FP_Mask.equals(IFU)) {
            _IFUMethod = p.getParameter(IFU_METHOD);
            if (_IFUMethod.equals(SINGLE_IFU)) {
                _IFUOffset = p.getParameter(IFU_OFFSET);
            } else if (_IFUMethod.equals(RADIAL_IFU)) {
                _IFUMinOffset = p.getParameter(IFU_MIN_OFFSET);
                _IFUMaxOffset = p.getParameter(IFU_MAX_OFFSET);
            } else
                ITCParameters.notFoundException(" a correct value for the IFU Parameters. ");
        }
        _instrumentLocation = p.getParameter(INSTRUMENT_LOCATION);
    }

    /**
     * Constructs a GmosParameters from a test file.
     */
    public GmosParameters(final String Filter,
                          final String grating,
                          final String instrumentCentralWavelength,
                          final String FP_Mask,
                          final String spatBinning,
                          final String specBinning,
                          final String IFUMethod,
                          final String IFUOffset,
                          final String IFUMinOffset,
                          final String IFUMaxOffset,
                          final String ccdType,
                          final String instrumentLocation) {
        _Filter = Filter;
        _grating = grating;
        _instrumentCentralWavelength = instrumentCentralWavelength;
        _FP_Mask = FP_Mask;
        _spatBinning = spatBinning;
        _specBinning = specBinning;
        _IFUMethod = IFUMethod;
        _IFUOffset = IFUOffset;
        _IFUMinOffset = IFUMinOffset;
        _IFUMaxOffset = IFUMaxOffset;
        _CCDtype = ccdType;
        _instrumentLocation = instrumentLocation;

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
        return new Double(_instrumentCentralWavelength);
    }

    public int getSpectralBinning() {
        return new Integer(_specBinning);
    }

    public int getSpatialBinning() {//System.out.println(new Integer(_spatBinning).intValue());
        return new Integer(_spatBinning);
    }

    public String getCCDtype() {
        return _CCDtype;
    }

    public double getFPMask() {
        //if (_FP_Mask.equals(NOSLIT)) return null;
        if (_FP_Mask.equals(SLIT0_25))
            return 0.25;
        else if (_FP_Mask.equals(SLIT0_5))
            return 0.5;
        else if (_FP_Mask.equals(SLIT0_75))
            return 0.75;
        else if (_FP_Mask.equals(SLIT1_0))
            return 1.0;
        else if (_FP_Mask.equals(SLIT1_5))
            return 1.5;
        else if (_FP_Mask.equals(SLIT2_0))
            return 2.0;
        else if (_FP_Mask.equals(SLIT5_0))
            return 5.0;
        else if (_FP_Mask.equals(IFU))
            return 0.3;
        else
            return -1.0;
    }

    public String getStringSlitWidth() {
        if (_FP_Mask.equals(SLIT0_25))
            return "025";
        else if (_FP_Mask.equals(SLIT0_5))
            return "050";
        else if (_FP_Mask.equals(SLIT0_75))
            return "075";
        else if (_FP_Mask.equals(SLIT1_0))
            return "100";
        else if (_FP_Mask.equals(SLIT1_5))
            return "150";
        else if (_FP_Mask.equals(SLIT2_0))
            return "200";
        else if (_FP_Mask.equals(SLIT5_0))
            return "500";
        else if (_FP_Mask.equals(IFU))
            return "IFU";
        else
            return "none";

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

    public String getInstrumentLocation() {
        return _instrumentLocation;
    }

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Filter:\t" + getFilter() + "\n");
        sb.append("Grating:\t" + getGrating() + "\n");
        sb.append("Instrument Central Wavelength:\t" + getInstrumentCentralWavelength() + "\n");
        sb.append("Focal Plane Mask: \t " + getFPMask() + " arcsec slit \n");
        sb.append("\n");
        return sb.toString();
    }
}
