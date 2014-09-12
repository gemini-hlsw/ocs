// Copyright 1997-2002
// Association for Universities for Research in Astronomy, Inc.,
//
// $Id: SeqConfigGPOLCB.java 4726 2004-05-14 16:50:12Z brighton $
//
package edu.gemini.spModel.gemini.gpol;

import edu.gemini.pot.sp.ISPSeqComponent;

import edu.gemini.spModel.config.HelperSeqCompCB;


/**
 * A configuration builder for the GPOL iterator.
 */
public final class SeqConfigGPOLCB extends HelperSeqCompCB {
    /**
     * Constructor for creating this seq comp CB.
     */
    public SeqConfigGPOLCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

}
