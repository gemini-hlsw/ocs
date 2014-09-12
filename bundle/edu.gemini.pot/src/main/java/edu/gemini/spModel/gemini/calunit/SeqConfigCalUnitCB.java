// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SeqConfigCalUnitCB.java 4726 2004-05-14 16:50:12Z brighton $
//
package edu.gemini.spModel.gemini.calunit;

import edu.gemini.pot.sp.ISPSeqComponent;

import edu.gemini.spModel.config.HelperSeqCompCB;


/**
 * A configuration builder for the CalUnit iterator.
 */
public final class SeqConfigCalUnitCB extends HelperSeqCompCB {
    /**
     * Constructor for creating this seq comp CB.
     */
    public SeqConfigCalUnitCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

}
