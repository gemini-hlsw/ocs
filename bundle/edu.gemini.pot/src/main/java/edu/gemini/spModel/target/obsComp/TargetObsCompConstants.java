// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TargetObsCompConstants.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.obsComp;

/**
 * Some shared constants for reading and writing targets and the target list.
 */
public final class TargetObsCompConstants {
    public static final String GUIDE_WITH_OIWFS_PROP = "guideWithOIWFS";

    public static final String NAME_PROP = "name";
    public static final String C1_PROP = "c1";
    public static final String C2_PROP = "c2";
    public static final String SYSTEM_PROP = "system";
    public static final String PROPER_MOTION_PROP = "properMotion";
    public static final String PM1_PROP = "pm1";
    public static final String PM2_PROP = "pm2";
    public static final String RV_PROP = "rv";
    public static final String EPOCH_PROP = "epoch";
    public static final String PARALLAX_PROP = "parallax";

    // Additional conic target properties
    public static final String INCLINATION_PROP = "inclination";
    public static final String ANODE_PROP = "anode";
    public static final String PERIHELION_PROP = "perihelion";
    public static final String AQ_PROP = "aq";
    public static final String E_PROP = "e";
    public static final String LM_PROP = "LM";
    public static final String N_PROP = "N";
    public static final String EPOCH_OF_PERI_PROP = "epochOfPerihelion";
    public static final String OBJECT = "object";

    public static final String PROPER_MOTION_UNITS = "sec-arcsec/year";

    /**
     * The name of the TargetEnv configuration
     */
    public static final String CONFIG_NAME = "telescope";

}
