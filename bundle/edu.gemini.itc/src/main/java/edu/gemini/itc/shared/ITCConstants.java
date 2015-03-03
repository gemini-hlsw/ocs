// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

// $Id: ITCConstants.java,v 1.11 2004/01/12 16:35:57 bwalls Exp $
//
package edu.gemini.itc.shared;

/**
 * This interface defines constants used across the ITC application.
 */
public interface ITCConstants {
    /**
     * Speed of light in kilometers/second
     */
    public static final double C = 299800.00;

    /**
     * Base location of data files relative to classpath
     */
    public static final String LIB = "";
    /**
     * Directory for the instrument Libraries
     */
    public static final String INST_LIB = "";
    /**
     * Base location of source spectra
     */
    public static final String SED_LIB = LIB + "/sed";

    /**
     * Base location of various transmission files
     */
    public static final String TRANSMISSION_LIB = LIB + "/transmission";

    /**
     * Base location of various calculation files
     */
    public static final String CALC_LIB = LIB + "/calc";

    /**
     * Location of sky background files
     */
    public static final String SKY_BACKGROUND_LIB = LIB + "/sky_emiss";

    /**
     * Base name for the optical sky background files
     */
    public static final String OPTICAL_SKY_BACKGROUND_FILENAME_BASE = "skybg";

    /**
     * Location of telescope background files
     */
    public static final String TELESCOPE_BACKGROUND_LIB = LIB + "/tele_emiss";

    /**
     * Base name of telescope background files
     */
    public static final String TELESCOPE_BACKGROUND_FILENAME_BASE = "tel";

    /**
     * Base name of telescope background files GS
     */
    public static final String GS_TELESCOPE_BACKGROUND_FILENAME_BASE = "telEmiss";

    /**
     * Base name for near ir sky background files
     */
    public static final String NEAR_IR_SKY_BACKGROUND_FILENAME_BASE =
            "nearIR_skybg";

    /**
     * Base name for mid ir sky background files
     */
    public static final String MID_IR_SKY_BACKGROUND_FILENAME_BASE =
            "midIR_skybg";

    /**
     * Location of image quality files
     */
    public static final String IM_QUAL_LIB = LIB + "/imqual";

    /**
     * Base name image quality files
     */
    public static final String IM_QUAL_BASE = "imqual_";

    /**
     * Data file suffix
     */
    public static final String DATA_SUFFIX = ".dat";

    /**
     * Filename for the flux in peak pixel LUT
     */
    public static final String FLUX_IN_PEAK_PIXEL_FILENAME =
            "/flux_in_peak_pixel";

    /**
     * Filename for the Slit throughput LUT
     */
    public static final String SLIT_THROUGHPUT_FILENAME =
            "/slit_throughput";

    public static final String VISIBLE = "03-08";

    public static final String NEAR_IR = "1-5";

    public static final String MID_IR = "7-26";

    public static final String HI_RES = "HI-Res";

    public static final String SERVER_TAG =
            //"\"http://itc.gemini.edu:8080/itc/servlet/images?type=txt&filename="
            "\"http://devitc.gemini.edu:8080/itc/servlet/images?type=txt&filename=";

}
