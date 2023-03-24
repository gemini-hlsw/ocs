package edu.gemini.spModel.gemini.igrins2;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.IConfig;

import java.util.Map;

final public class SeqConfigIgrins2CB extends HelperSeqCompCB {
    private static final long serialVersionUID = 1L;

    public SeqConfigIgrins2CB(final ISPSeqComponent seqComp) {
        super(seqComp);
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    @Override
    protected void thisApplyNext(final IConfig config, final IConfig prevFull) {
        super.thisApplyNext(config, prevFull);
    }

    @Override
    public void thisReset(final Map<String, Object> options) {
        super.thisReset(options);
    }
}