package edu.gemini.spModel.seqcomp;

import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.config.AbstractSeqComponentCB;

import edu.gemini.pot.sp.ISPSeqComponent;

import java.util.Map;

public class SeqRepeatCB extends AbstractSeqComponentCB {

    // for serialization
    private static final long serialVersionUID = 1L;

    private transient int _curCount;
    private transient int _max;

    public SeqRepeatCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        SeqRepeatCB result = (SeqRepeatCB) super.clone();
        result._curCount = 0;
        result._max = 0;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        _curCount = 0;
        _max = SeqRepeatCbOptions.getCollapseRepeat(options) ? 1 :
                                ((SeqRepeat) getDataObject()).getStepCount();
    }

    protected boolean thisHasNext() {
        return _curCount < _max;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        ++_curCount;

        // do nothing to the configuration
    }

}

