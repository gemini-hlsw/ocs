package edu.gemini.spModel.gemini.seqcomp.smartgcal;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKeyProvider;

/**
 * A configuration collection for working with smart gcals.
 */
public final class SmartgcalSysConfig {
    public static final String  NAME              = "smartgcal";
    public static final ItemKey MATCHER           = key("*");

    public static final String  MAPPING_ERROR     = "maperror";
    public static final ItemKey MAPPING_ERROR_KEY = key(MAPPING_ERROR);

    public static final String  STEP_NUMBER       = "step";
    public static final ItemKey STEP_KEY          = key(STEP_NUMBER);

    public static final String  EXECUTED_STEPS    = "executed";
    public static final ItemKey EXECUTED_KEY      = key(EXECUTED_STEPS);

    public static final String  KEY_PROVIDER      = "keyProvider";
    public static final ItemKey KEY_PROVIDER_KEY  = key(KEY_PROVIDER);

    private static ItemKey key(String name) { return new ItemKey(NAME + ":" + name); }

    /**
     * A ConfigSequence Predicate that matches configs that contain a mapping
     * error flag.
     */
    public static final ConfigSequence.Predicate MAPPING_ERROR_PREDICATE = new ConfigSequence.Predicate() {
        public boolean matches(Config c) {
            Object val = c.getItemValue(SmartgcalSysConfig.MAPPING_ERROR_KEY);
            return Boolean.TRUE.equals(val);
        }
    };

    private final ISysConfig sys;

    SmartgcalSysConfig(ISysConfig sys) {
        this.sys = sys;
    }

    public Object getValue(String key, Object def) {
        IParameter p = sys.getParameter(key);
        if (p == null) return def;
        Object val =  p.getValue();
        return (val == null) ? def : val;
    }

    public SmartgcalSysConfig setValue(String key, Object value) {
        sys.putParameter(DefaultParameter.getInstance(key, value));
        return this;
    }

    /** Returns true if this step could not be mapped by the smartgcal node. */
    public boolean isMappingError() { return (Boolean) getValue(MAPPING_ERROR, Boolean.FALSE); }
    public void setMappingError(boolean mappingError) { setValue(MAPPING_ERROR, mappingError); }

    /** Returns the step number of this dataset relative to others produced by the same smart node. */
    public int getStepNumber() { return (Integer) getValue(STEP_NUMBER, -1); }
    public void setStepNumber(int stepNumber) { setValue(STEP_NUMBER, stepNumber); }

    /** Gets the total number of executed steps to expect in the sequence. */
    public int getExecutedSteps() { return (Integer) getValue(EXECUTED_STEPS, -1); }
    public void setExecutedSteps(int executedSteps) { setValue(EXECUTED_STEPS, executedSteps); }

    public void setExecutedSteps(Config[] sequence, int sequenceStep) {
        int executedSteps = computeExecutedSteps(sequence, sequenceStep);
        if (executedSteps > 0) setExecutedSteps(executedSteps);
    }

    /** Gets the calibration provider (i.e. the instrument) that can extract a key from the configuration. */
    public CalibrationKeyProvider getCalibrationKeyProvider() { return (CalibrationKeyProvider) getValue(KEY_PROVIDER, null); }
    public void setCalibrationKeyProvider(CalibrationKeyProvider provider) { setValue(KEY_PROVIDER, provider); }

    /**
     * Computes the number of executed steps that should be expected for the
     * current gcal node (if any).
     */
    public static int computeExecutedSteps(Config[] sequence, int sequenceStep) {
        if (sequenceStep >= sequence.length) return -1;

        Integer smartstep = (Integer) sequence[sequenceStep].getItemValue(STEP_KEY);
        if ((smartstep == null) || (smartstep < 0)) return -1;

        for (int i=sequenceStep+1; i<sequence.length; ++i) {
            Integer next = (Integer) sequence[i].getItemValue(STEP_KEY);
            if ((next == null) || (next < 0) || (next <= smartstep)) break;
            smartstep = next;
        }
        return smartstep + 1;
    }

    public static SmartgcalSysConfig extract(IConfig conf) {
        ISysConfig sysConfig = conf.getSysConfig(NAME);
        if (sysConfig == null) {
            sysConfig = new DefaultSysConfig(NAME, true);
            conf.appendSysConfig(sysConfig);
        }
        return new SmartgcalSysConfig(sysConfig);
    }

}
