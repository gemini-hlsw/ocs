package edu.gemini.itc.niri;

import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.spModel.gemini.niri.Niri.*;

/**
 * NIRI parameters relevant for ITC.
 */
public final class NiriParameters implements InstrumentDetails {

    public static final String LOW_READ_NOISE = "lowNoise";
    public static final String MED_READ_NOISE = "medNoise";
    public static final String HIGH_READ_NOISE = "highNoise";
    public static final String HIGH_WELL_DEPTH = "highWell";
    public static final String LOW_WELL_DEPTH = "lowWell";

    //Replacing Naming convention of Slits.  Moving from Arcsec to pix widths.
    //For now merge code so both naming conventions are supported.
    //Eventuall change code so only pix widths is supported
    public static final String SLIT_2_PIX_CENTER = "2-pix-center";
    public static final String SLIT_4_PIX_CENTER = "4-pix-center";
    public static final String SLIT_6_PIX_CENTER = "6-pix-center";
    public static final String SLIT_2_PIX_BLUE = "2-pix-blue";
    public static final String SLIT_4_PIX_BLUE = "4-pix-blue";
    public static final String SLIT_6_PIX_BLUE = "6-pix-blue";
    public static final String NO_SLIT = "none";

    // Data members
    private final Filter _Filter;  // filters
    private final Disperser _grism; // Grism or null
    private final Camera _camera; // camera F6, F14, or F32
    private final String _readNoise;
    private final String _wellDepth;
    private final Mask _FP_Mask;

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     */
    public NiriParameters(final Filter Filter,
                          final Disperser grism,
                          final Camera camera,
                          final String readNoise,
                          final String wellDepth,
                          final Mask FP_Mask) {
        _Filter = Filter;
        _grism = grism;
        _camera = camera;
        _readNoise = readNoise;
        _wellDepth = wellDepth;
        _FP_Mask = FP_Mask;

    }

    public Filter getFilter() {
        return _Filter;
    }

    public Disperser getGrism() {
        return _grism;
    }

    public Camera getCamera() {
        return _camera;
    }

    public String getReadNoise() {
        return _readNoise;
    }

    public String getWellDepth() {
        return _wellDepth;
    }

    public Mask getFocalPlaneMask() {
        return _FP_Mask;
    }

    public String getFPMaskOffset() {
        switch (_FP_Mask) {
            case MASK_1:
            case MASK_2:
            case MASK_3:
                return "center";
            case MASK_4:
            case MASK_5:
            case MASK_6:
                return "blue";
            default:
                throw new Error();
        }
//        if (_FP_Mask.equals(SLIT0_70_CENTER) ||
//                _FP_Mask.equals(SLIT0_23_CENTER) ||
//                _FP_Mask.equals(SLIT0_46_CENTER) ||
//                _FP_Mask.equals(SLIT_2_PIX_CENTER) ||
//                _FP_Mask.equals(SLIT_4_PIX_CENTER) ||
//                _FP_Mask.equals(SLIT_6_PIX_CENTER) ||
//                _FP_Mask.equals(F32_SLIT_10_PIX_CENTER) ||
//                _FP_Mask.equals(F32_SLIT_7_PIX_CENTER) ||
//                _FP_Mask.equals(F32_SLIT_4_PIX_CENTER))
//            return "center";
//        else if (_FP_Mask.equals(SLIT0_70_BLUE) ||
//                _FP_Mask.equals(SLIT0_23_BLUE) ||
//                _FP_Mask.equals(SLIT0_46_BLUE) ||
//                _FP_Mask.equals(SLIT_2_PIX_BLUE) ||
//                _FP_Mask.equals(SLIT_4_PIX_BLUE) ||
//                _FP_Mask.equals(SLIT_6_PIX_BLUE))
//            return "blue";
//        else
//            return "none";
    }

    public String getStringSlitWidth() {
        // TODO: use size values provided by masks, this will make an update of baseline necessary
        switch (_FP_Mask) {
            case MASK_1:        // f6 2pix center
            case MASK_4:        // f6 2pix blue
                return "023";
            case MASK_2:        // f6 4pix center
            case MASK_5:        // f6 4pix blue
                return "046";
            case MASK_3:        // f6 6pix center
            case MASK_6:        // f6 6pix blue
                return "070";
            default:
                throw new Error();
        }

//        if (_FP_Mask.equals(SLIT0_70_CENTER) ||
//                _FP_Mask.equals(SLIT0_70_BLUE) ||
//                _FP_Mask.equals(SLIT_6_PIX_CENTER) ||
//                _FP_Mask.equals(SLIT_6_PIX_BLUE))
//            return "070";
//        else if (_FP_Mask.equals(SLIT0_23_CENTER) ||
//                _FP_Mask.equals(SLIT0_23_BLUE) ||
//                _FP_Mask.equals(SLIT_2_PIX_BLUE) ||
//                _FP_Mask.equals(SLIT_2_PIX_CENTER))
//            return "023";
//        else if (_FP_Mask.equals(SLIT0_46_CENTER) ||
//                _FP_Mask.equals(SLIT0_46_BLUE) ||
//                _FP_Mask.equals(SLIT_4_PIX_BLUE) ||
//                _FP_Mask.equals(SLIT_4_PIX_CENTER))
//            return "046";
//        else if (_FP_Mask.equals(F32_SLIT_4_PIX_CENTER))
//            return "009";
//        else if (_FP_Mask.equals(F32_SLIT_7_PIX_CENTER))
//            return "014";
//        else if (_FP_Mask.equals(F32_SLIT_10_PIX_CENTER))
//            return "023";
//        else
//            return "none";
    }


    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Filter:\t" + getFilter() + "\n");
        sb.append("Grism:\t" + getGrism() + "\n");
        sb.append("Camera:\t" + getCamera() + "\n");
        sb.append("Focal Plane Mask: \t " + _FP_Mask + " arcsec slit \n");
        sb.append("\n");
        return sb.toString();
    }
}
