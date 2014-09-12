package edu.gemini.spModel.gemini.gpi;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBaseCB;

import java.util.Map;

/**
 * Configuration builder for
 * {@link edu.gemini.spModel.gemini.gpi.SeqRepeatGpiOffset}.
 * See OT-103.
 * @deprecated
 */
@Deprecated
public class SeqRepeatGpiOffsetCB extends SeqRepeatOffsetBaseCB<GpiOffsetPos> {

    public SeqRepeatGpiOffsetCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    @Override
    public Object clone() {
        SeqRepeatGpiOffsetCB res = (SeqRepeatGpiOffsetCB) super.clone();
        return res;
    }

    @Override
    protected void thisReset(Map options) {
        super.thisReset(options);
    }

    @Override
    protected void addPandQ(IConfig config, GpiOffsetPos op) {
        config.putParameter(SYSTEM_NAME,
                            StringParameter.getInstance("ifsXOffset", op.getXAxisAsString()));
        config.putParameter(SYSTEM_NAME,
                StringParameter.getInstance("ifsYOffset", op.getYAxisAsString()));
    }
}
