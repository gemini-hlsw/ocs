// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqRepeatOffsetCB.java 18053 2009-02-20 20:16:23Z swalker $
//

package edu.gemini.spModel.gemini.seqcomp;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.target.offset.OffsetPos;


public class SeqRepeatOffsetCB extends SeqRepeatOffsetBaseCB<OffsetPos> {
    public SeqRepeatOffsetCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        return super.clone();
    }
}

