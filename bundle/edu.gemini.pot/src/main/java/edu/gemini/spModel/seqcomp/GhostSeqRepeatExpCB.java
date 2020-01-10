package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.AbstractSeqComponentCB;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.dataflow.GsaSequenceEditor;
import edu.gemini.spModel.gemini.ghost.Ghost$;
import edu.gemini.spModel.obscomp.InstConstants;

import java.util.Map;

/**
 * A configuration builder for GHOST science object observe sequence component that
 * includes red and blue exposure times.
 */
public class GhostSeqRepeatExpCB extends AbstractSeqComponentCB {
    private static final String SYSTEM_NAME = SeqConfigNames.OBSERVE_CONFIG_NAME;
    private static final long serialVersionUID = 1L;

    private transient int _curCount;
    private transient int _max;
    private transient int _limit;
    private transient String _objectName;
    private transient Map<String, Object> _options;

    public GhostSeqRepeatExpCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        GhostSeqRepeatExpCB result = (GhostSeqRepeatExpCB) super.clone();
        result._curCount = 0;
        result._max = 0;
        result._limit = 0;
        result._objectName = null;
        result._options = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        _curCount = 0;
        final GhostExpTimeSeqComponent c = (GhostExpTimeSeqComponent) getDataObject();
        _max = c.getStepCount();
        _limit = SeqRepeatCbOptions.getCollapseRepeat(options) ? 1 : _max;
        _objectName = c.getType().readableStr;
        _options = options;
    }

    protected boolean thisHasNext() {
        return _curCount < _limit;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        ++_curCount;
        final GhostExpSeqComponent c = (GhostExpSeqComponent) getDataObject();
        config.putParameter(SYSTEM_NAME,
                StringParameter.getInstance(InstConstants.OBSERVE_TYPE_PROP,
                        c.getObserveType()));

        config.putParameter(SYSTEM_NAME, StringParameter.getInstance(InstConstants.OBJECT_PROP,
                _objectName));

        config.putParameter(SYSTEM_NAME,
                DefaultParameter.getInstance(Ghost$.MODULE$.RED_EXPOSURE_TIME_PROP(), c.getRedExposureTime()));

        config.putParameter(SYSTEM_NAME,
                DefaultParameter.getInstance(Ghost$.MODULE$.RED_EXPOSURE_COUNT_PROP(), c.getRedExposureCount()));

        config.putParameter(SYSTEM_NAME,
                DefaultParameter.getInstance(Ghost$.MODULE$.BLUE_EXPOSURE_TIME_PROP(), c.getBlueExposureTime()));

        config.putParameter(SYSTEM_NAME,
                DefaultParameter.getInstance(Ghost$.MODULE$.BLUE_EXPOSURE_COUNT_PROP(), c.getBlueExposureCount()));

        config.putParameter(SYSTEM_NAME,
                StringParameter.getInstance(InstConstants.OBS_CLASS_PROP,
                        c.getObsClass().sequenceValue()));

        GsaSequenceEditor.instance.addProprietaryPeriod(config, getSeqComponent().getProgram(), c.getObsClass());

        if (!SeqRepeatCbOptions.getAddObsCount(_options)) return;
        config.putParameter(SYSTEM_NAME,
                DefaultParameter.getInstance(InstConstants.REPEAT_COUNT_PROP, _max));
    }
}
