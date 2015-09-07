// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqConfigNIRICB.java 27570 2010-10-25 19:17:01Z swalker $
//
package edu.gemini.spModel.gemini.niri;

import edu.gemini.pot.sp.ISPSeqComponent;

import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.IConfig;

import java.util.Map;

/**
 * A configuration builder for the NIRI iterator.
 */
public final class SeqConfigNIRICB extends HelperSeqCompCB {

    /**
     * Constructor for creating this seq comp CB.
     */
    public SeqConfigNIRICB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        SeqConfigNIRICB result = (SeqConfigNIRICB) super.clone();
        return result;
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
                                                        InstNIRI.INSTRUMENT_NAME_PROP));

        InstNIRI.WAVELENGTH_INJECTOR.inject(config, prevFull);
    }

    public void thisReset(Map options) {
        super.thisReset(options);
    }
}
