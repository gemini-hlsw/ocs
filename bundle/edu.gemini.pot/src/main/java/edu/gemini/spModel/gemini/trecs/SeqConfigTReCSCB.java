// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqConfigTReCSCB.java 27669 2010-10-28 17:44:55Z swalker $
//

package edu.gemini.spModel.gemini.trecs;

import edu.gemini.pot.sp.ISPSeqComponent;

import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.IConfig;

import java.util.Map;

/**
 * A configuration builder for the TReCS iterator.
 */
public final class SeqConfigTReCSCB extends HelperSeqCompCB {

    /**
     * Constructor for creating this seq comp CB.
     */
    public SeqConfigTReCSCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        SeqConfigTReCSCB result = (SeqConfigTReCSCB) super.clone();
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
                                                        InstTReCS.INSTRUMENT_NAME_PROP));

        InstTReCS.WAVELENGTH_INJECTOR.inject(config, prevFull);
    }

    public void thisReset(Map options) {
        super.thisReset(options);
    }
}
