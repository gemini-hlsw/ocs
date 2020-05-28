package edu.gemini.spModel.gemini.ghost;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.util.Map;

final public class SeqConfigGhostCB extends HelperSeqCompCB {
    private static final long serialVersionUID = 1L;

    public SeqConfigGhostCB(final ISPSeqComponent seqComp) {
        super(seqComp);
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    @Override
    protected void thisApplyNext(final IConfig config, final IConfig prevFull) {
        super.thisApplyNext(config, prevFull);

        config.putParameter(SeqConfigNames.INSTRUMENT_CONFIG_NAME,
                StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP,
                        Ghost$.MODULE$.INSTRUMENT_NAME_PROP()));
    }

    @Override
    public void thisReset(final Map<String, Object> options) {
        super.thisReset(options);
    }
}