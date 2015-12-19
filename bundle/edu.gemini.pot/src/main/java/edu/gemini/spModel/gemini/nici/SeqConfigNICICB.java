package edu.gemini.spModel.gemini.nici;

import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME;
import static edu.gemini.spModel.obscomp.InstConstants.INSTRUMENT_NAME_PROP;
import edu.gemini.pot.sp.ISPSeqComponent;

import java.util.Map;

public final class SeqConfigNICICB extends HelperSeqCompCB {

    public SeqConfigNICICB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        return super.clone();
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        super.thisApplyNext(config, prevFull);

        // Always add the instrument name
        config.putParameter(INSTRUMENT_CONFIG_NAME,
            StringParameter.getInstance(INSTRUMENT_NAME_PROP, InstNICI.INSTRUMENT_NAME_PROP));

        InstNICICB.injectWavelength(config, prevFull);
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        super.thisReset(options);
    }

}
