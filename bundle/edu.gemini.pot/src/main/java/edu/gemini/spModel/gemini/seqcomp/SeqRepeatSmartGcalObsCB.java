package edu.gemini.spModel.gemini.seqcomp;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.config.MetaDataConfig;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.dataflow.GsaSequenceEditor;
import edu.gemini.spModel.gemini.calunit.calibration.*;
import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKeyProvider;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProvider;
import edu.gemini.spModel.gemini.seqcomp.smartgcal.SmartgcalSysConfig;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqRepeatCbOptions;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A configuration builder for the Gemini CalUnit sequence
 * component that include coadds and exposure time.
 */
public abstract class SeqRepeatSmartGcalObsCB implements IConfigBuilder, Cloneable {
    private final SPNodeKey nodeId;
    private final ISPSeqComponent seqComponent;

//    private transient ObsClass _obsClass;
    private transient List<Config> _plannedSteps;
    private transient int _stepCount;
    private transient int _executedSteps;
    private transient CalibrationProvider _calibrationProvider;

    private transient boolean _firstTime;

    public static class BasecalDay extends SeqRepeatSmartGcalObsCB {
        public BasecalDay(ISPSeqComponent seqComp)  {
            super(seqComp);
        }
        protected List<Calibration> filter(List<Calibration> calibrations) {
            List<Calibration> filteredCalibrations = new ArrayList<>();
            for (Calibration c : calibrations) {
                if (c.isBasecalDay()) {
                    filteredCalibrations.add(c);
                }
            }
            return filteredCalibrations;
        }
    }
    public static class BasecalNight extends SeqRepeatSmartGcalObsCB {
        public BasecalNight(ISPSeqComponent seqComp)  {
            super(seqComp);
        }
        protected List<Calibration> filter(List<Calibration> calibrations) {
            List<Calibration> filteredCalibrations = new ArrayList<>();
            for (Calibration c : calibrations) {
                if (c.isBasecalNight()) {
                    filteredCalibrations.add(c);
                }
            }
            return filteredCalibrations;
        }
    }
    public static class Flat extends SeqRepeatSmartGcalObsCB {
        public Flat(ISPSeqComponent seqComp)  {
            super(seqComp);
        }
        protected List<Calibration> filter(List<Calibration> calibrations) {
            List<Calibration> filteredCalibrations = new ArrayList<>();
            for (Calibration c : calibrations) {
                if (c.isFlat()) {
                    filteredCalibrations.add(c);
                }
            }
            return filteredCalibrations;
        }
    }
    public static class Arc extends SeqRepeatSmartGcalObsCB {
        public Arc(ISPSeqComponent seqComp)  {
            super(seqComp);
        }
        protected List<Calibration> filter(List<Calibration> calibrations) {
            List<Calibration> filteredCalibrations = new ArrayList<>();
            for (Calibration c : calibrations) {
                if (c.isArc()) {
                    filteredCalibrations.add(c);
                }
            }
            return filteredCalibrations;
        }
    }


    protected abstract List<Calibration> filter(List<Calibration> calibrations);

    public SeqRepeatSmartGcalObsCB(ISPSeqComponent seqComp)  {
        nodeId        = seqComp.getNodeKey();
        seqComponent  = seqComp;
    }

    public Object clone() {
        SeqRepeatSmartGcalObsCB result;
        try {
            result = (SeqRepeatSmartGcalObsCB) super.clone();
        } catch (CloneNotSupportedException ex) {
            // Won't happen, since Object implements cloneable ...
            throw new InternalError();
        }
        _plannedSteps = null;
        return result;
    }

    @Override
    public void reset(Map<String, Object> options)  {
        _firstTime     = true;
        _plannedSteps  = new ArrayList<>();
        _stepCount     = 0;
        _executedSteps = 0;
        _calibrationProvider = SeqRepeatCbOptions.getCalibrationProvider(options);
    }

    @Override
    public boolean hasNext()  {
        return (_firstTime) || (_stepCount < _plannedSteps.size()) || (_stepCount < _executedSteps);
    }

    @Override
    public void applyNext(IConfig current, IConfig prevFull)  {
        _firstTime = false;

        // The node id will be used to tie steps with sequence iterator nodes.
        MetaDataConfig mdc = MetaDataConfig.extract(current);
        mdc.addNodeKey(nodeId);
        setSmartValues(current, prevFull);
    }

    private ISysConfig getSysConfig(IConfig config, String name) {
        ISysConfig sys = config.getSysConfig(name);
        if (sys == null) {
            sys = new DefaultSysConfig(name);
            config.appendSysConfig(sys);
        }
        return sys;
    }

    private ISysConfig getObserveConfig(IConfig config) {
        return getSysConfig(config, SeqConfigNames.OBSERVE_CONFIG_NAME);
    }

    private List<Config> getPlannedSteps(IConfig current, IConfig prev) {
        ISysConfig instrumentConfig = current.getSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        if (instrumentConfig == null) instrumentConfig = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        ISysConfig prevInstrumentConfig = prev.getSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        if (prevInstrumentConfig != null) {
            prevInstrumentConfig = (ISysConfig) prevInstrumentConfig.clone();
            prevInstrumentConfig.mergeParameters(instrumentConfig);
            instrumentConfig = prevInstrumentConfig;
        }

        if (_plannedSteps.size() == 0) {
            // by default no calibrations are available
            List<Calibration> calibrations = Collections.emptyList();
            // check if there is a calibration provider available (i.e. instrument supports smart calibrations)
            CalibrationKeyProvider provider = SmartgcalSysConfig.extract(current).getCalibrationKeyProvider();
            if (provider != null) {
                // extract the calibration lookup key for this instrument configuration
                CalibrationKey key = provider.extractKey(instrumentConfig);
                calibrations = filter(_calibrationProvider.getCalibrations(key));
            }

            int stepCount = 0;
            for (Calibration cal : calibrations) {
                for (int i=0; i<cal.getObserve(); ++i) {
                    _plannedSteps.add(CalConfigFactory.complete(cal, stepCount++));
                }
            }
        }

        return _plannedSteps;
    }

    private boolean isComplete(IConfig current) {
        return MetaDataConfig.extract(current).isComplete() && (SmartgcalSysConfig.extract(current).getStepNumber() >= 0);
    }

    private void setSmartValues(IConfig current, IConfig prev)  {

        List<Config> steps = getPlannedSteps(current, prev);

        if (isComplete(current)) {
            _executedSteps = SmartgcalSysConfig.extract(current).getExecutedSteps();

            // Fundamental items will already be in the sequence.  Fill in the
            // derived items.
            IndexedCalibrationStep ics = new IConfigBackedIndexedCalibrationStep(current, prev);
            Config c = CalConfigFactory.complete(ics);
            CalConfigBuilderUtil.updateIConfig(c, current, prev);

        } else if (steps.size() == 0) {
            getObserveConfig(current).putParameter(StringParameter.getInstance(InstConstants.OBSERVE_TYPE_PROP, InstConstants.CAL_OBSERVE_TYPE));
            SmartgcalSysConfig ssc = SmartgcalSysConfig.extract(current);
            ssc.setMappingError(true);
        } else {
            Config step = steps.get(_stepCount);
            setObsClass(current, step);
            CalConfigBuilderUtil.updateIConfig(step, current, prev);
        }
        ++_stepCount;
    }

    private void setObsClass(IConfig current, Config step)  {
        SeqRepeatSmartGcalObs c = (SeqRepeatSmartGcalObs) seqComponent.getDataObject();
        final ObsClass obsClass;
        if (c.getObsClass() == null) {
            // auto mode -> calculate observe class for calibration (depends on node type and baseline calibration type (night/day))
            obsClass = calculateAutoObsClass(step);
        } else {
            // manual mode -> we use the value that is set in the GUI component
            obsClass = c.getObsClass();
        }
        getObserveConfig(current).putParameter(StringParameter.getInstance(InstConstants.OBS_CLASS_PROP, obsClass.sequenceValue()));
        GsaSequenceEditor.instance.addProprietaryPeriod(current, seqComponent.getProgram(), obsClass);
    }

    private ObsClass calculateAutoObsClass(Config step) {
        Boolean isBaselineNight = (Boolean) step.getItemValue(CalDictionary.BASECAL_NIGHT_ITEM.key);
        ObsClass autoObsClass;
        if (this instanceof BasecalDay) {
            autoObsClass = ObsClass.DAY_CAL;
        } else if (this instanceof BasecalNight) {
            autoObsClass  = ObsClass.PARTNER_CAL;
        } else if (this instanceof Flat) {
            autoObsClass = isBaselineNight ? ObsClass.PARTNER_CAL : ObsClass.PROG_CAL;
        } else if (this instanceof Arc) {
            autoObsClass = isBaselineNight ? ObsClass.PARTNER_CAL : DefaultArcObsClass.forNode(seqComponent);
        } else {
            throw new InternalError("unknown node type");
        }
        return autoObsClass;
    }
}
