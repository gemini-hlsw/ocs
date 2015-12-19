package edu.gemini.spModel.seqcomp;

import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.config.AbstractSeqComponentCB;

import edu.gemini.pot.sp.ISPSeqComponent;
import java.util.Map;

//
// NOTE: the root sequence component config builder is ignored.  The
// GemObservationCB skips it and goes right to the children of the root.
// These methods are never called and this class is never really used.
//

public class SeqBaseCB extends AbstractSeqComponentCB {

    // for serialization
    private static final long serialVersionUID = 1L;

    private transient boolean _firstTime = true;

    public SeqBaseCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        SeqBaseCB result = (SeqBaseCB) super.clone();
        result._firstTime   = true;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
    }

    protected boolean thisHasNext() {
        if (_firstTime) {
            _firstTime = false;
            return true;
        }
        return false;
    }

    protected void thisApplyNext(IConfig config, IConfig fullPrev) {
        // do nothing to the configuration
    }
}

