// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SeqConfigNames.java 38186 2011-10-24 13:21:33Z swalker $
//

package edu.gemini.spModel.seqcomp;


import edu.gemini.spModel.config2.ItemKey;

/**
 * This interface centralizes the names of the common parameter set
 * configuration names.
 */
public final class SeqConfigNames {

    /**
     * The system name for "Observer" parameters - exposure time, observe
     * type, coadds.
     */
    public static final String OBSERVE_CONFIG_NAME = "observe";
    public static final ItemKey OBSERVE_KEY = new ItemKey(OBSERVE_CONFIG_NAME);

    /**
     * The configuration system name for calibration parameters.
     */
    public static final String CALIBRATION_CONFIG_NAME = "calibration";
    public static final ItemKey CALIBRATION_KEY = new ItemKey(CALIBRATION_CONFIG_NAME);

    /**
     * The configuration system name for telescope parameters.
     */
    public static final String TELESCOPE_CONFIG_NAME = "telescope";
    public static final ItemKey TELESCOPE_KEY = new ItemKey(TELESCOPE_CONFIG_NAME);

    /**
     * The configuration system name for a generic instrument.
     */
    public static final String INSTRUMENT_CONFIG_NAME = "instrument";
    public static final ItemKey INSTRUMENT_KEY = new ItemKey(INSTRUMENT_CONFIG_NAME);

    public static final String META_DATA_CONFIG_NAME = "meta";
    public static final ItemKey META_DATA_KEY = new ItemKey(META_DATA_CONFIG_NAME);

    /**
     * The configuration name for the observe context temporary
     * system.
     */
//    public static final String OBSERVE_CONTEXT_NAME = "observeContext";

    /**
     * The configuration name for the controlling system.
     */
    public static final String OCS_CONFIG_NAME = "ocs";
    public static final ItemKey OCS_KEY = new ItemKey(OCS_CONFIG_NAME);

    /**
     * The configuration name for any timeline specific information.
     */
//    public static final String TIMELINE_NAME = "timelineInfo";

}
