package edu.gemini.spModel.gemini.gpi;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.util.Map;

/**
 * A configuration builder for the Gpi iterator.
 */
public final class SeqConfigGpiCB extends HelperSeqCompCB {

    /**
     * Constructor for creating this seq comp CB.
     */
    public SeqConfigGpiCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        SeqConfigGpiCB result = (SeqConfigGpiCB) super.clone();
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
                                    Gpi.INSTRUMENT_NAME_PROP));
    }


    public void thisReset(Map options) {
        super.thisReset(options);
    }
}

