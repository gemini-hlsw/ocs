// Copyright 1997-2002
// Association for Universities for Research in Astronomy, Inc.
//
// $Id: SeqConfigPhoenixCB.java 27568 2010-10-25 18:03:42Z swalker $
//
package edu.gemini.spModel.gemini.phoenix;

import edu.gemini.pot.sp.ISPSeqComponent;

import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.IConfig;

/**
 * A configuration builder for the Phoenix iterator.
 */
public final class SeqConfigPhoenixCB extends HelperSeqCompCB {

    /**
     * Constructor for creating this seq comp CB.
     */
    public SeqConfigPhoenixCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    /**
     * This thisApplyNext overrides the HelperSeqCompCB
     * so that the integration time, exposure time and ncoadds can
     * be inserting in the observe system.
     */
    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        super.thisApplyNext(config, prevFull);

        config.putParameter(SeqConfigNames.INSTRUMENT_CONFIG_NAME,
                            StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP,
                                                        InstPhoenix.INSTRUMENT_NAME_PROP));

    }

}
