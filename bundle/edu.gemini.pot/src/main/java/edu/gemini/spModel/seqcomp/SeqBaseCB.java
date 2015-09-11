// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqBaseCB.java 37893 2011-10-06 15:25:48Z swalker $
//

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

    protected void thisReset(Map options) {
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

