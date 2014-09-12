// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SeqRepeatBiasObsCB.java 38419 2011-11-07 14:37:51Z swalker $
//

package edu.gemini.spModel.seqcomp;

import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.config.AbstractSeqComponentCB;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.obscomp.InstConstants;

import edu.gemini.pot.sp.ISPSeqComponent;

import java.util.Map;

/**
 * A configuration builder for the bias observation.
 * This component is special because it only supports coadds.
 */
public class SeqRepeatBiasObsCB extends AbstractSeqComponentCB {

    private static final String SYSTEM_NAME = SeqConfigNames.OBSERVE_CONFIG_NAME;

    // for serialization
    private static final long serialVersionUID = 1L;

    private transient int _curCount;
    private transient int _max;
    private transient int _limit;
    private transient boolean _firstTime;
    private transient Map _options;

    public SeqRepeatBiasObsCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        SeqRepeatBiasObsCB result = (SeqRepeatBiasObsCB) super.clone();
        result._curCount = 0;
        result._max = 0;
        result._limit = 0;
        result._firstTime = false;
        result._options = null;
        return result;
    }

    protected void thisReset(Map options) {
        _curCount = 0;
        ICoaddExpSeqComponent c = (ICoaddExpSeqComponent) getDataObject();
        _max = c.getStepCount();
        _limit = SeqRepeatCbOptions.getCollapseRepeat(options) ? 1 : _max;
        // Set _firstTime to true so coadds is returned
        _firstTime = true;
        _options = options;
    }

    protected boolean thisHasNext() {
        return _curCount < _limit;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        ++_curCount;
        ICoaddExpSeqComponent c = (ICoaddExpSeqComponent) getDataObject();
        config.putParameter(SYSTEM_NAME, StringParameter.getInstance(InstConstants.OBSERVE_TYPE_PROP,
                c.getObserveType()));
        config.putParameter(SYSTEM_NAME, StringParameter.getInstance(InstConstants.OBS_CLASS_PROP,
                c.getObsClass().sequenceValue()));
        config.putParameter(SYSTEM_NAME, StringParameter.getInstance(InstConstants.OBJECT_PROP,
                "Bias"));

        // See OT-73
        // Was moved from TextSequenceFunctor
        config.putParameter(SYSTEM_NAME, DefaultParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP, 0.0));

        if (_firstTime) {
            config.putParameter(SYSTEM_NAME, DefaultParameter.getInstance(InstConstants.COADDS_PROP, c.getCoaddsCount()));
        }

        if (!SeqRepeatCbOptions.getAddObsCount(_options)) return;
        config.putParameter(SYSTEM_NAME,
           DefaultParameter.getInstance(InstConstants.REPEAT_COUNT_PROP, _max));
    }

}

