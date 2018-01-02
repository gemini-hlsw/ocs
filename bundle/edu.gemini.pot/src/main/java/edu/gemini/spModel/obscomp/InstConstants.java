// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: InstConstants.java 38189 2011-10-24 14:44:06Z swalker $
//
package edu.gemini.spModel.obscomp;

import edu.gemini.spModel.config2.ItemKey;

import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.OBSERVE_KEY;

/**
 * Attribute names shared in common with many instruments.
 */
public class InstConstants {


    public static final String COADDS_PROP = "coadds";
    public static final int DEF_COADDS     = 1;
    public static final ItemKey COADDS_KEY = new ItemKey(OBSERVE_KEY, COADDS_PROP);

    public static final String DATA_LABEL_PROP = "dataLabel";

    public static final String EXPOSURE_TIME_PROP = "exposureTime";
    public static final double DEF_EXPOSURE_TIME  = 10.0;
    public static final ItemKey EXPOSURE_TIME_KEY = new ItemKey(OBSERVE_KEY, EXPOSURE_TIME_PROP);

    public static final String OBSERVING_WAVELENGTH_PROP = "observingWavelength";
    public static final ItemKey OBSERVING_WAVELENGTH_KEY = new ItemKey(INSTRUMENT_KEY, OBSERVING_WAVELENGTH_PROP);

    public static final String OBSERVATIONID_PROP = "observationId";

    public static final String POS_ANGLE_PROP = "posAngle";
    public static final double DEF_POS_ANGLE = 0.0;

    public static final String PROGRAMID_PROP = "programId";

    public static final String OBS_CLASS_PROP = "class";
    public static final String REPEAT_COUNT_PROP = "repeatCount";
    public static final int DEF_REPEAT_COUNT = 1;

    public static final String STATUS_PROP = "status";

    public static final String UID_PROP = "uid";

    //public static final String OFFSET_PROP = "offset";

    //public static final String VERSION_PROP = "version";

    // The type of the observation (science|flat|bias)
    public static final String OBSERVE_TYPE_PROP = "observeType";
    public static final String SCIENCE_OBSERVE_TYPE = "OBJECT";
    public static final String BIAS_OBSERVE_TYPE = "BIAS";
    public static final String DARK_OBSERVE_TYPE = "DARK";
    public static final String FLAT_OBSERVE_TYPE = "FLAT";
    public static final String ARC_OBSERVE_TYPE = "ARC";
    public static final String CAL_OBSERVE_TYPE = "CAL";
    public static final String DEF_OBSERVE_TYPE = SCIENCE_OBSERVE_TYPE;

    // A property defining the target object name
    public static final String OBJECT_PROP = "object";

    // A property defining the instrument name
    public static final String INSTRUMENT_NAME_PROP = "instrument";

    // value for default offset position link value
    public static final String PARK   = "park";
    public static final String FREEZE = "freeze";

    public static final String SCI_BAND = "sciBand";

    public static final ItemKey INST_INSTRUMENT_KEY = new ItemKey(INSTRUMENT_KEY, INSTRUMENT_NAME_PROP);

}
