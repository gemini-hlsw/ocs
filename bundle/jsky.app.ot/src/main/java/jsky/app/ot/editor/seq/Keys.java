package jsky.app.ot.editor.seq;

import edu.gemini.spModel.config.MetaDataConfig;
import edu.gemini.spModel.config2.ItemKey;

final class Keys {
    static final ItemKey CALIBRATION_KEY= new ItemKey("calibration");
    static final ItemKey INSTRUMENT_KEY = new ItemKey("instrument");
    static final ItemKey OBSERVE_KEY    = new ItemKey("observe");
    static final ItemKey TELESCOPE_KEY  = new ItemKey("telescope");

    static final ItemKey DATALABEL_KEY   = new ItemKey("observe:dataLabel");

    static final ItemKey CAL_CLASS_KEY   = new ItemKey("calibration:class");
    static final ItemKey CAL_VERSION_KEY = new ItemKey("calibration:version");

    static final ItemKey OBS_CLASS_KEY   = new ItemKey("observe:class");
    static final ItemKey OBS_COADDS_KEY  = new ItemKey("observe:coadds");
    static final ItemKey OBS_ELAPSED_KEY = new ItemKey("observe:elapsedTime");
    static final ItemKey OBS_EXP_TIME_KEY= new ItemKey("observe:exposureTime");
    static final ItemKey OBS_OBJECT_KEY  = new ItemKey("observe:object");
    static final ItemKey OBS_STATUS_KEY  = new ItemKey("observe:status");
    static final ItemKey OBS_TYPE_KEY    = new ItemKey("observe:observeType");

    static final ItemKey TEL_BASE_NAME   = new ItemKey(new ItemKey("telescope:Base"), "name");
    static final ItemKey TEL_P_KEY       = new ItemKey("telescope:p");
    static final ItemKey TEL_Q_KEY       = new ItemKey("telescope:q");
    static final ItemKey TEL_VERSION_KEY = new ItemKey("telescope:version");

    static final ItemKey INST_EXP_TIME_KEY   = new ItemKey("instrument:exposureTime");
    static final ItemKey INST_TIME_ON_SRC_KEY= new ItemKey("instrument:timeOnSource");
    static final ItemKey INST_COADDS_KEY     = new ItemKey("instrument:coadds");
    static final ItemKey INST_VERSION_KEY    = new ItemKey("instrument:version");
    static final ItemKey INST_INSTRUMENT_KEY = new ItemKey("instrument:instrument");
    static final ItemKey INST_DISPERSER_KEY  = new ItemKey("instrument:disperser");
    static final ItemKey INST_ACQ_MIRROR     = new ItemKey("instrument:acquisitionMirror");

    static final ItemKey SP_NODE_KEY = new ItemKey(MetaDataConfig.NAME + ":" + MetaDataConfig.SP_NODE);
}
