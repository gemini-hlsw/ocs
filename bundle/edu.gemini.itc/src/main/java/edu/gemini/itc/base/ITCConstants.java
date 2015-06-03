package edu.gemini.itc.base;

/**
 * This interface defines constants used across the ITC application.
 */
public interface ITCConstants {
    /**
     * Speed of light in kilometers/second
     */
    double C = 299800.00;

    /**
     * Base location of data files relative to classpath
     */
    String LIB = "";

    /**
     * Base location of source spectra
     */
    String SED_LIB = LIB + "/sed";

    /**
     * Base location of various transmission files
     */
    String TRANSMISSION_LIB = LIB + "/transmission";

    /**
     * Base location of various calculation files
     */
    String CALC_LIB = LIB + "/calc";

    /**
     * Location of sky background files
     */
    String SKY_BACKGROUND_LIB = LIB + "/sky_emiss";

    /**
     * Base name for the optical sky background files
     */
    String OPTICAL_SKY_BACKGROUND_FILENAME_BASE = "skybg";

    /**
     * Location of telescope background files
     */
    String TELESCOPE_BACKGROUND_LIB = LIB + "/tele_emiss";

    /**
     * Base name of telescope background files
     */
    String TELESCOPE_BACKGROUND_FILENAME_BASE = "tel";

    /**
     * Base name of telescope background files GS
     */
    String GS_TELESCOPE_BACKGROUND_FILENAME_BASE = "telEmiss";

    /**
     * Base name for near ir sky background files
     */
    String NEAR_IR_SKY_BACKGROUND_FILENAME_BASE = "nearIR_skybg";

    /**
     * Base name for mid ir sky background files
     */
    String MID_IR_SKY_BACKGROUND_FILENAME_BASE = "midIR_skybg";

    /**
     * Location of image quality files
     */
    String IM_QUAL_LIB = LIB + "/imqual";

    /**
     * Base name image quality files
     */
    String IM_QUAL_BASE = "imqual_";

    /**
     * Data file suffix
     */
    String DATA_SUFFIX = ".dat";

    /**
     * Filename for the flux in peak pixel LUT
     */
    String FLUX_IN_PEAK_PIXEL_FILENAME = "/flux_in_peak_pixel";

    /**
     * Filename for the Slit throughput LUT
     */
    String SLIT_THROUGHPUT_FILENAME = "/slit_throughput";

    String VISIBLE = "03-08";

    String NEAR_IR = "1-5";

    String MID_IR = "7-26";

    String HI_RES = "HI-Res";


}
