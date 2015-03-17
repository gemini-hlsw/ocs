package edu.gemini.itc.niri;

import edu.gemini.itc.service.InstrumentDetails;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCParameters;

import javax.servlet.http.HttpServletRequest;

/**
 * This class holds the information from the Niri section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class NiriParameters implements InstrumentDetails {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String INSTRUMENT_FILTER = "instrumentFilter";
    public static final String INSTRUMENT_GRISM = "instrumentDisperser";
    public static final String INSTRUMENT_CAMERA = "instrumentCamera";
    public static final String READ_NOISE = "readNoise";
    public static final String WELL_DEPTH = "wellDepth";
    public static final String FP_MASK = "instrumentFPMask";

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String F6 = "F6";
    public static final String F14 = "F14";
    public static final String F32 = "F32";
    public static final String JGRISM = "J-grism";
    public static final String HGRISM = "H-grism";
    public static final String KGRISM = "K-grism";
    public static final String LGRISM = "L-grism";
    public static final String MGRISM = "M-grism";
    public static final String NOGRISM = null;
    public static final int J = 0;
    public static final int H = 1;
    public static final int K = 2;
    public static final int L = 3;
    public static final int M = 4;
    public static final String LOW_READ_NOISE = "lowNoise";
    public static final String MED_READ_NOISE = "medNoise";
    public static final String HIGH_READ_NOISE = "highNoise";
    public static final String HIGH_WELL_DEPTH = "highWell";
    public static final String LOW_WELL_DEPTH = "lowWell";
    public static final String SLIT0_23_CENTER = "slit0.23center";
    public static final String SLIT0_23_BLUE = "slit0.23blue";
    public static final String SLIT0_46_CENTER = "slit0.46center";
    public static final String SLIT0_46_BLUE = "slit0.46blue";
    public static final String SLIT0_70_CENTER = "slit0.70center";
    public static final String SLIT0_70_BLUE = "slit0.70blue";

    //Replacing Naming convention of Slits.  Moving from Arcsec to pix widths.
    //For now merge code so both naming conventions are supported.
    //Eventuall change code so only pix widths is supported
    public static final String SLIT_2_PIX_CENTER = "2-pix-center";
    public static final String SLIT_4_PIX_CENTER = "4-pix-center";
    public static final String SLIT_6_PIX_CENTER = "6-pix-center";
    public static final String SLIT_2_PIX_BLUE = "2-pix-blue";
    public static final String SLIT_4_PIX_BLUE = "4-pix-blue";
    public static final String SLIT_6_PIX_BLUE = "6-pix-blue";
    public static final String F32_SLIT_10_PIX_CENTER = "f32-10-pix-center";
    public static final String F32_SLIT_7_PIX_CENTER = "f32-7-pix-center";
    public static final String F32_SLIT_4_PIX_CENTER = "f32-4-pix-center";
    public static final String SLIT0_1 = "slit0.1";
    public static final String SLIT0_15 = "slit0.15";
    public static final String SLIT0_23 = "slit0.23";
    public static final String SLIT0_46 = "slit0.46";
    public static final String NO_SLIT = "none";

    // Data members
    private String _Filter;  // filters
    private String _grism; // Grism or null
    private String _camera; // camera F6, F14, or F32
    private String _readNoise;
    private String _wellDepth;
    private String _FP_Mask;

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public NiriParameters(HttpServletRequest r) {
        parseServletRequest(r);
    }

    /**
     * Constructs a NiriParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public NiriParameters(ITCMultiPartParser p) {
        parseMultipartParameters(p);
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) {
        // Parse the acquisition camera section of the form.

        // Get Broad Band filter
        _Filter = r.getParameter(INSTRUMENT_FILTER);
        if (_Filter == null) {
            ITCParameters.notFoundException(INSTRUMENT_FILTER);
        }


        // Get Grism
        _grism = r.getParameter(INSTRUMENT_GRISM);
        if (_grism == null) {
            ITCParameters.notFoundException(INSTRUMENT_GRISM);
        }

        if (_Filter.equals("none") && _grism.equals("none")) {
            throw new IllegalArgumentException("Must specify a filter or a grism");
        }

        // Get Camera Used
        _camera = r.getParameter(INSTRUMENT_CAMERA);
        if (_camera == null) {
            ITCParameters.notFoundException(INSTRUMENT_CAMERA);
        }

        //Get High or low read noise
        _readNoise = r.getParameter(READ_NOISE);
        if (_readNoise == null) {
            ITCParameters.notFoundException(READ_NOISE);
        }

        //Get High or low read noise
        _wellDepth = r.getParameter(WELL_DEPTH);
        if (_wellDepth == null) {
            ITCParameters.notFoundException(WELL_DEPTH);
        }

        _FP_Mask = r.getParameter(FP_MASK);
        if (_FP_Mask == null) {
            ITCParameters.notFoundException(FP_MASK);
        }

    }

    public void parseMultipartParameters(ITCMultiPartParser p) {
        _Filter = p.getParameter(INSTRUMENT_FILTER);
        _grism = p.getParameter(INSTRUMENT_GRISM);
        if (_Filter.equals("none") && _grism.equals("none")) {
            throw new IllegalArgumentException("Must specify a filter or a grism");
        }
        _camera = p.getParameter(INSTRUMENT_CAMERA);
        _readNoise = p.getParameter(READ_NOISE);
        _wellDepth = p.getParameter(WELL_DEPTH);
        _FP_Mask = p.getParameter(FP_MASK);
    }

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public NiriParameters(String Filter,
                          String grism,
                          String camera,
                          String readNoise,
                          String wellDepth,
                          String FP_Mask) {
        _Filter = Filter;
        _grism = grism;
        _camera = camera;
        _readNoise = readNoise;
        _wellDepth = wellDepth;
        _FP_Mask = FP_Mask;

    }

    public String getFilter() {
        return _Filter;
    }

    public String getGrism() {
        return _grism;
    }

    public String getCamera() {
        return _camera;
    }

    public String getReadNoise() {
        return _readNoise;
    }

    public String getWellDepth() {
        return _wellDepth;
    }

    public String getFocalPlaneMask() {
        return _FP_Mask;
    }

    public double getFPMask() {
        //if (_FP_Mask.equals(NOSLIT)) return null;
        if (_FP_Mask.equals(SLIT0_70_CENTER) ||
                _FP_Mask.equals(SLIT_6_PIX_CENTER))
            return 0.75; //old value 0.68;
        else if (_FP_Mask.equals(SLIT0_70_BLUE) ||
                _FP_Mask.equals(SLIT_6_PIX_BLUE))
            return 0.7;
        else if (_FP_Mask.equals(SLIT0_23_CENTER) ||
                _FP_Mask.equals(SLIT0_23_BLUE) ||
                _FP_Mask.equals(SLIT_2_PIX_CENTER) ||
                _FP_Mask.equals(SLIT_2_PIX_BLUE))
            return 0.23;
        else if (_FP_Mask.equals(SLIT0_46_CENTER) ||
                _FP_Mask.equals(SLIT_4_PIX_CENTER))
            return 0.47;
        else if (_FP_Mask.equals(SLIT0_46_BLUE) ||
                _FP_Mask.equals(SLIT_4_PIX_BLUE))
            return 0.46;
        else if (_FP_Mask.equals(F32_SLIT_10_PIX_CENTER))
            return 0.22;
        else if (_FP_Mask.equals(F32_SLIT_7_PIX_CENTER))
            return 0.144;
        else if (_FP_Mask.equals(F32_SLIT_4_PIX_CENTER))
            return 0.09;
        else
            return -1.0;
    }

    public String getFPMaskOffset() {
        if (_FP_Mask.equals(SLIT0_70_CENTER) ||
                _FP_Mask.equals(SLIT0_23_CENTER) ||
                _FP_Mask.equals(SLIT0_46_CENTER) ||
                _FP_Mask.equals(SLIT_2_PIX_CENTER) ||
                _FP_Mask.equals(SLIT_4_PIX_CENTER) ||
                _FP_Mask.equals(SLIT_6_PIX_CENTER) ||
                _FP_Mask.equals(F32_SLIT_10_PIX_CENTER) ||
                _FP_Mask.equals(F32_SLIT_7_PIX_CENTER) ||
                _FP_Mask.equals(F32_SLIT_4_PIX_CENTER))
            return "center";
        else if (_FP_Mask.equals(SLIT0_70_BLUE) ||
                _FP_Mask.equals(SLIT0_23_BLUE) ||
                _FP_Mask.equals(SLIT0_46_BLUE) ||
                _FP_Mask.equals(SLIT_2_PIX_BLUE) ||
                _FP_Mask.equals(SLIT_4_PIX_BLUE) ||
                _FP_Mask.equals(SLIT_6_PIX_BLUE))
            return "blue";
        else
            return "none";
    }

    public String getStringSlitWidth() {
        if (_FP_Mask.equals(SLIT0_70_CENTER) ||
                _FP_Mask.equals(SLIT0_70_BLUE) ||
                _FP_Mask.equals(SLIT_6_PIX_CENTER) ||
                _FP_Mask.equals(SLIT_6_PIX_BLUE))
            return "070";
        else if (_FP_Mask.equals(SLIT0_23_CENTER) ||
                _FP_Mask.equals(SLIT0_23_BLUE) ||
                _FP_Mask.equals(SLIT_2_PIX_BLUE) ||
                _FP_Mask.equals(SLIT_2_PIX_CENTER))
            return "023";
        else if (_FP_Mask.equals(SLIT0_46_CENTER) ||
                _FP_Mask.equals(SLIT0_46_BLUE) ||
                _FP_Mask.equals(SLIT_4_PIX_BLUE) ||
                _FP_Mask.equals(SLIT_4_PIX_CENTER))
            return "046";
        else if (_FP_Mask.equals(F32_SLIT_4_PIX_CENTER))
            return "009";
        else if (_FP_Mask.equals(F32_SLIT_7_PIX_CENTER))
            return "014";
        else if (_FP_Mask.equals(F32_SLIT_10_PIX_CENTER))
            return "023";
        else
            return "none";
    }


    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Filter:\t" + getFilter() + "\n");
        sb.append("Grism:\t" + getGrism() + "\n");
        sb.append("Camera:\t" + getCamera() + "\n");
        sb.append("Focal Plane Mask: \t " + getFPMask() + " arcsec slit \n");
        sb.append("\n");
        return sb.toString();
    }
}
