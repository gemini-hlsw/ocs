package edu.gemini.spModel.gemini.flamingos2;

import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.pot.sp.ISPSeqComponent;

import java.util.Map;

/**
 * A configuration builder for the Flamingos2 iterator.
 */
public final class SeqConfigFlamingos2CB extends HelperSeqCompCB {

    /**
     * Constructor for creating this seq comp CB.
     */
    public SeqConfigFlamingos2CB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        return super.clone();
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
                                                        Flamingos2.INSTRUMENT_NAME_PROP));

        Flamingos2.WAVELENGTH_INJECTOR.inject(config, prevFull);
    }


    public void thisReset(Map<String, Object> options) {
        super.thisReset(options);
    }
}

