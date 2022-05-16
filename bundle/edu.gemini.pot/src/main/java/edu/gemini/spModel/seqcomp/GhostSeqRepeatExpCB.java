package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config.AbstractSeqComponentCB;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.dataflow.GsaSequenceEditor;
import edu.gemini.spModel.gemini.ghost.*;
import edu.gemini.spModel.obscomp.InstConstants;

import edu.gemini.spModel.syntax.JavaDurationOps;

import java.beans.PropertyDescriptor;
import java.time.Duration;
import java.util.Map;

/**
 * A configuration builder for GHOST science object observe sequence component that
 * includes red and blue exposure times and counts.
 */
final public class GhostSeqRepeatExpCB extends AbstractSeqComponentCB {
    private static final String SYSTEM_NAME = SeqConfigNames.OBSERVE_CONFIG_NAME;
    private static final long serialVersionUID = 1L;

    private transient int _curCount;
    private transient int _max;
    private transient int _limit;
    private transient String _objectName;
    private transient Map<String, Object> _options;

    public GhostSeqRepeatExpCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        GhostSeqRepeatExpCB result = (GhostSeqRepeatExpCB) super.clone();
        result._curCount = 0;
        result._max = 0;
        result._limit = 0;
        result._objectName = null;
        result._options = null;
        return result;
    }

    @Override
    protected void thisReset(Map<String, Object> options) {
        _curCount = 0;
        final GhostExpSeqComponent c = (GhostExpSeqComponent) getDataObject();
        _max = c.getStepCount();
        _limit = SeqRepeatCbOptions.getCollapseRepeat(options) ? 1 : _max;
        _objectName = c.getType().readableStr;
        _options = options;
    }

    protected boolean thisHasNext() {
        return _curCount < _limit;
    }

    private static Duration secondsToDuration(Double secs) {
        return Duration.ofMillis(Math.round(secs * 1000.0));
    }

    private static <A> A lookup(IConfig config, Option<IConfig> prev, String name, A defaultValue) {
        final Option<Object> a0 = ImOption.apply(
            config.getParameterValue(SeqConfigNames.INSTRUMENT_CONFIG_NAME, name)
        );

        final Option<Object> a1 = a0.orElse(prev.flatMap(p -> ImOption.apply(
            p.getParameterValue(SeqConfigNames.INSTRUMENT_CONFIG_NAME, name)
        )));

        //noinspection unchecked
        return (A) a1.getOrElse(defaultValue);
    }

    private static GhostReadNoiseGain getGain(IConfig config, Option<IConfig> prev, PropertyDescriptor prop) {
        return lookup(config, prev, prop.getName(), GhostReadNoiseGain.DEFAULT);
    }

    private static GhostBinning getBinning(IConfig config, Option<IConfig> prev, PropertyDescriptor prop) {
        return lookup(config, prev, prop.getName(), GhostBinning.DEFAULT);
    }

    private static GhostCameras getGhostCameras(GhostExpSeqComponent c, IConfig config, IConfig prevFull) {

        final Option<IConfig> prev = ImOption.apply(prevFull);

        final GhostCamera.Red red = new GhostCamera.Red(
            c.getRedExposureCount(),
            secondsToDuration(c.getRedExposureTime()),
            getGain(config, prev, Ghost.RED_READ_NOISE_GAIN_PROP()),
            getBinning(config, prev, Ghost.RED_BINNING_PROP())
        );

        final GhostCamera.Blue blue = new GhostCamera.Blue(
            c.getBlueExposureCount(),
            secondsToDuration(c.getBlueExposureTime()),
            getGain(config, prev, Ghost.BLUE_READ_NOISE_GAIN_PROP()),
            getBinning(config, prev, Ghost.BLUE_BINNING_PROP())
        );

        return new GhostCameras(red, blue);
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        ++_curCount;
        final GhostExpSeqComponent c = (GhostExpSeqComponent) getDataObject();
        config.putParameter(SYSTEM_NAME,
                StringParameter.getInstance(InstConstants.OBSERVE_TYPE_PROP,
                        c.getObserveType()));

        config.putParameter(SYSTEM_NAME, StringParameter.getInstance(InstConstants.OBJECT_PROP,
                _objectName));

        config.putParameter(SYSTEM_NAME,
            DefaultParameter.getInstance(
                InstConstants.EXPOSURE_TIME_PROP,
                new JavaDurationOps(getGhostCameras(c, config, prevFull).exposure()).fractionalSeconds()
            )
        );

        // We add the exposure time parameters to the "observe" system, but not
        // "instrument".  We don't want to override the "instrument" values for
        // subsequent steps.
        GhostExposureTimeProvider.addToConfig(config, SYSTEM_NAME, c);

        config.putParameter(SYSTEM_NAME,
                StringParameter.getInstance(InstConstants.OBS_CLASS_PROP,
                        c.getObsClass().sequenceValue()));

        GsaSequenceEditor.instance.addProprietaryPeriod(config, getSeqComponent().getProgram(), c.getObsClass());

        if (!SeqRepeatCbOptions.getAddObsCount(_options)) return;
        config.putParameter(SYSTEM_NAME,
                DefaultParameter.getInstance(InstConstants.REPEAT_COUNT_PROP, _max));
    }
}
