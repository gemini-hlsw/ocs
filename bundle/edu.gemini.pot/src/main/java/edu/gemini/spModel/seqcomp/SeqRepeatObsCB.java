package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.config.AbstractSeqComponentCB;
import edu.gemini.spModel.dataflow.GsaSequenceEditor;
import edu.gemini.spModel.obscomp.InstConstants;

import java.util.Map;

/**
 * A configuration builder for the science object observe sequence
 * component.
 */
public class SeqRepeatObsCB extends AbstractSeqComponentCB {

    // for serialization
    private static final long serialVersionUID = 1L;

    private static final String SYSTEM_NAME = SeqConfigNames.OBSERVE_CONFIG_NAME;

    private transient int _curCount;
    private transient int _limit;
    private transient int _max;
    private transient Map<String, Object> _options;

    public SeqRepeatObsCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        SeqRepeatObsCB result = (SeqRepeatObsCB) super.clone();
        result._curCount = 0;
        result._max = 0;
        result._limit = 0;
        result._options = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        _curCount = 0;
        _max = ((IObserveSeqComponent) getDataObject()).getStepCount();
        _limit = SeqRepeatCbOptions.getCollapseRepeat(options) ? 1 : _max;
        _options = options;
    }

    protected boolean thisHasNext() {
        return _curCount < _limit;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        // Is there an exposureTime

        IObserveSeqComponent c = (IObserveSeqComponent) getDataObject();
        ++_curCount;
        config.putParameter(SYSTEM_NAME,
                            StringParameter.getInstance(InstConstants.OBSERVE_TYPE_PROP,
                                                        c.getObserveType()));
        config.putParameter(SYSTEM_NAME,
                            StringParameter.getInstance(InstConstants.OBS_CLASS_PROP,
                                                        c.getObsClass().sequenceValue()));

        GsaSequenceEditor.instance.addProprietaryPeriod(config, getSeqComponent().getProgram(), c.getObsClass());

        if (!SeqRepeatCbOptions.getAddObsCount(_options)) return;
        config.putParameter(SYSTEM_NAME,
           DefaultParameter.getInstance(InstConstants.REPEAT_COUNT_PROP, _max));
    }

}

