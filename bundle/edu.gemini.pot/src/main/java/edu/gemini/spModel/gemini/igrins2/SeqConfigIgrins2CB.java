package edu.gemini.spModel.gemini.igrins2;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.beans.PropertyDescriptor;
import java.util.Map;

final public class SeqConfigIgrins2CB extends HelperSeqCompCB {
    private static final long serialVersionUID = 1L;

    public SeqConfigIgrins2CB(final ISPSeqComponent seqComp) {
        super(seqComp);
    }

    private static void copyToObserve(IConfig c, PropertyDescriptor d) {

        final Object v = c.getParameterValue(SeqConfigNames.INSTRUMENT_CONFIG_NAME, d.getName());
        if (v != null) {
            final DefaultParameter p = DefaultParameter.getInstance(d, v);
            c.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME, p);
        }

    }

    @Override
    public Object clone() {
        return super.clone();
    }

    @Override
    protected void thisApplyNext(final IConfig config, final IConfig prevFull) {
        super.thisApplyNext(config, prevFull);

        copyToObserve(config, Igrins2.EXPOSURE_TIME_PROP());

        config.putParameter(SeqConfigNames.INSTRUMENT_CONFIG_NAME,
                StringParameter.getInstance(InstConstants.INSTRUMENT_NAME_PROP,
                        Igrins2.INSTRUMENT_NAME_PROP()));
    }

    @Override
    public void thisReset(final Map<String, Object> options) {
        super.thisReset(options);
    }
}