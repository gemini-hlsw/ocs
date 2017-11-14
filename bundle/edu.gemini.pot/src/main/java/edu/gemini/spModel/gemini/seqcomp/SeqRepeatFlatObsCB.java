package edu.gemini.spModel.gemini.seqcomp;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.AbstractSeqComponentCB;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.dataflow.GsaSequenceEditor;
import edu.gemini.spModel.gemini.calunit.calibration.CalConfigBuilderUtil;
import edu.gemini.spModel.gemini.calunit.calibration.CalConfigFactory;
import edu.gemini.spModel.gemini.calunit.calibration.MutableIndexedCalibrationStep;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqRepeatCbOptions;

import java.util.Map;

/**
 * A configuration builder for the Gemini CalUnit sequence
 * component that include coadds and exposure time.
 */
public class SeqRepeatFlatObsCB extends AbstractSeqComponentCB {
    private transient int _curCount;
    private transient int _max;
    private transient int _limit;
    private transient Map<String, Object> _options;

    private transient String _obsClass;

    private transient MutableIndexedCalibrationStep mics;

    public SeqRepeatFlatObsCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        SeqRepeatFlatObsCB result = (SeqRepeatFlatObsCB) super.clone();
        result._curCount = 0;
        result._max      = 0;
        result._limit    = 0;
        result._options  = null;
        result._obsClass = null;
        result.mics      = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        _curCount = 0;
        SeqRepeatFlatObs c = (SeqRepeatFlatObs) getDataObject();
        _max = c.getStepCount();
        _limit = SeqRepeatCbOptions.getCollapseRepeat(options) ? 1 : _max;

        _obsClass = c.getObsClass().sequenceValue();
        _options = options;

        mics = new MutableIndexedCalibrationStep();
        mics.setLamps(c.getLamps());
        mics.setShutter(c.getShutter());
        mics.setFilter(c.getFilter());
        mics.setDiffuser(c.getDiffuser());
        mics.setExposureTime(c.getExposureTime());
        mics.setCoadds(c.getCoaddsCount());
        mics.setObsClass(_obsClass);
    }

    protected boolean thisHasNext() {
        return _curCount < _limit;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        ++_curCount;

        // Remove any executed smartcal data placed in the config by the
        // GemObservationCB.  This can happen when converting a smart cal to
        // a manual calibration for executed or partially executed sequences.
        CalConfigBuilderUtil.clear(config);

        // Now write the configuration for this manual cal into the sequence.
        Config c = CalConfigFactory.complete(mics);
        CalConfigBuilderUtil.updateIConfig(c, config, prevFull);

        final SeqRepeatFlatObs f = (SeqRepeatFlatObs) getDataObject();
        GsaSequenceEditor.instance.addProprietaryPeriod(config, getSeqComponent().getProgram(), f.getObsClass());

        if (SeqRepeatCbOptions.getAddObsCount(_options)) {
            ISysConfig obs = getObsSysConfig(config);
            obs.putParameter(
               DefaultParameter.getInstance(InstConstants.REPEAT_COUNT_PROP, _max));
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

