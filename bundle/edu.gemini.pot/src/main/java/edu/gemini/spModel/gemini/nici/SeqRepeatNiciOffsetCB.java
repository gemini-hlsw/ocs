package edu.gemini.spModel.gemini.nici;

import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBaseCB;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.pot.sp.ISPSeqComponent;

import java.util.Map;

/**
 * Configuration builder for
 * {@link edu.gemini.spModel.gemini.nici.SeqRepeatNiciOffset}.
 */
public class SeqRepeatNiciOffsetCB extends SeqRepeatOffsetBaseCB<NiciOffsetPos> {

    public SeqRepeatNiciOffsetCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        return super.clone();
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        super.thisReset(options);
    }

    protected void addPosition(IConfig config, NiciOffsetPos op) {
        super.addPosition(config, op);

        config.putParameter(SYSTEM_NAME,
                StringParameter.getInstance("d", Double.toString(op.getOffsetDistance())));

        String guideWithFPMW = op.isFpmwTracking() ? Boolean.TRUE.toString() : Boolean.FALSE.toString();
        config.putParameter(SYSTEM_NAME,
                StringParameter.getInstance("guideWithFPMW", guideWithFPMW));
    }
}
