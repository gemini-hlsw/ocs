package edu.gemini.spModel.gemini.seqcomp;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.AbstractSeqComponentCB;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.dataflow.GsaSequenceEditor;
import edu.gemini.spModel.gemini.calunit.CalUnitConstants;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.*;
import edu.gemini.spModel.gemini.calunit.calibration.CalConfigBuilderUtil;
import edu.gemini.spModel.gemini.ghost.Ghost$;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.GhostSeqRepeatExp;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqRepeatCbOptions;

import java.util.Map;

/**
 * A configuration builder for the Gemini CalUnit sequence component for GHOST.
 */
public class GhostSeqRepeatFlatObsCB extends AbstractSeqComponentCB {
    private static final long serialVersionUID = 1L;

    private transient int curCount;
    private transient int max;
    private transient int limit;
    private transient Map<String, Object> options;

    private transient String obsClass;

    public GhostSeqRepeatFlatObsCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        final GhostSeqRepeatFlatObsCB result = (GhostSeqRepeatFlatObsCB) super.clone();
        result.curCount = 0;
        result.max = 0;
        result.limit = 0;
        result.options = null;
        result.obsClass = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        curCount = 0;
        final GhostSeqRepeatFlatObs c = (GhostSeqRepeatFlatObs) getDataObject();
        max = c.getStepCount();
        limit = SeqRepeatCbOptions.getCollapseRepeat(options) ? 1 : max;

        obsClass = c.getObsClass().sequenceValue();
        this.options = options;
    }

    protected boolean thisHasNext() {
        return curCount < limit;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        ++curCount;

        // Remove any executed smartcal data placed in the config by the
        // GemObservationCB.  This can happen when converting a smart cal to
        // a manual calibration for executed or partially executed sequences.
        CalConfigBuilderUtil.clear(config);

        final GhostSeqRepeatFlatObs c = (GhostSeqRepeatFlatObs) getDataObject();
        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                DefaultParameter.getInstance(CalUnitConstants.LAMP_PROP, Lamp.write(c.getLamps())));
        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                DefaultParameter.getInstance(CalUnitConstants.SHUTTER_PROP, c.getShutter().sequenceValue()));
        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                DefaultParameter.getInstance(CalUnitConstants.FILTER_PROP, c.getFilter().sequenceValue()));
        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                DefaultParameter.getInstance(CalUnitConstants.DIFFUSER_PROP, c.getDiffuser().sequenceValue()));

        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                StringParameter.getInstance(InstConstants.OBS_CLASS_PROP,
                        c.getObsClass().sequenceValue()));

        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                DefaultParameter.getInstance(Ghost$.MODULE$.RED_EXPOSURE_TIME_PROP(), c.getRedExposureTime()));
        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                DefaultParameter.getInstance(Ghost$.MODULE$.RED_EXPOSURE_COUNT_PROP(), c.getRedExposureCount()));

        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                DefaultParameter.getInstance(Ghost$.MODULE$.BLUE_EXPOSURE_TIME_PROP(), c.getBlueExposureTime()));
        config.putParameter(SeqConfigNames.OBSERVE_CONFIG_NAME,
                DefaultParameter.getInstance(Ghost$.MODULE$.BLUE_EXPOSURE_COUNT_PROP(), c.getBlueExposureCount()));

        GsaSequenceEditor.instance.addProprietaryPeriod(config, getSeqComponent().getProgram(), c.getObsClass());

        if (SeqRepeatCbOptions.getAddObsCount(options)) {
            ISysConfig obs = getObsSysConfig(config);
            obs.putParameter(
                    DefaultParameter.getInstance(InstConstants.REPEAT_COUNT_PROP, max));
        }
    }

    private ISysConfig getObsSysConfig(IConfig config) {
        ISysConfig sys = config.getSysConfig(SeqConfigNames.OBSERVE_CONFIG_NAME);
        if (sys == null) {
            sys = new DefaultSysConfig(SeqConfigNames.OBSERVE_CONFIG_NAME);
            config.appendSysConfig(sys);
        }
        return sys;
    }
}

