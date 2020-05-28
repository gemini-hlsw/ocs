package edu.gemini.spModel.gemini.ghost;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.HelperSeqCompCB;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final public class SeqConfigGhostCB extends HelperSeqCompCB {
    private static final long serialVersionUID = 1L;

    private static final List<String> EXP_PROPS =
      Collections.unmodifiableList(
        Arrays.asList(
            Ghost$.MODULE$.BLUE_EXPOSURE_COUNT_PROP().getName(),
            Ghost$.MODULE$.BLUE_EXPOSURE_TIME_PROP().getName(),
            Ghost$.MODULE$.RED_EXPOSURE_COUNT_PROP().getName(),
            Ghost$.MODULE$.RED_EXPOSURE_TIME_PROP().getName()
        )
      );

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

        // A small hack to swap these to the "observe" system to be consistent
        // everywhere.
        for (String propName : EXP_PROPS) {
            final Object o = config.getParameterValue(
                    SeqConfigNames.INSTRUMENT_CONFIG_NAME,
                    propName
            );
            config.removeParameter(SeqConfigNames.INSTRUMENT_CONFIG_NAME, propName);
            if (o != null) {
                config.putParameter(
                    SeqConfigNames.OBSERVE_CONFIG_NAME,
                    DefaultParameter.getInstance(propName, o)
                );
            }
        }
    }

    @Override
    public void thisReset(final Map<String, Object> options) {
        super.thisReset(options);
    }
}