package edu.gemini.spModel.gemini.gmos;

import edu.gemini.pot.sp.ISPSeqComponent;

import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.IConfig;

import java.util.Map;

/**
 * A configuration builder for the GMOS-S iterator.
 */
public final class SeqConfigGmosSouthCB extends HelperSeqCompCB {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for creating this seq comp CB.
     */
    public SeqConfigGmosSouthCB(ISPSeqComponent seqComp) {
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

        // Insert the instrument name
        config.putParameter(SeqConfigNames.INSTRUMENT_CONFIG_NAME,
                StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP,
                        InstGmosSouth.INSTRUMENT_NAME_PROP));

        InstGmosSouth.WAVELENGTH_INJECTOR.inject(config, prevFull);
        InstGmosSouth.GAIN_SETTING_INJECTOR.inject(config, prevFull);
    }

    public void thisReset(Map<String, Object> options) {
        super.thisReset(options);
    }
}
