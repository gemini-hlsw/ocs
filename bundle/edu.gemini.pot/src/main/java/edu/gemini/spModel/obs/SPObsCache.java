//
// $Id: SPObsCache.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.obs;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.GeminiRuntimeException;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.dataset.DataflowStatus;
import edu.gemini.spModel.dataset.DatasetQaStateSums;
import edu.gemini.spModel.obs.plannedtime.PlannedStepSummary;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.time.ObsTimes;
import edu.gemini.util.skycalc.calc.TargetCalculator;

import java.io.Serializable;

/**
 * A cache of information that is associated with the {@link SPObservation} but
 * not maintained in the observation.  These are derived properties that base
 * their value on the {@link edu.gemini.spModel.obsrecord.ObsExecRecord}, etc.
 */
public final class SPObsCache implements ISPEventMonitor, ISPCloneable, Serializable {
    private static long serialVersionUID = 1L;

    private static final String CLIENT_DATA_KEY = "SPObsCache";

    public static SPObsCache getObsCache(ISPObservation obs) {
        return (SPObsCache) obs.getTransientClientData(CLIENT_DATA_KEY);
    }

    public static void setObsCache(ISPObservation obs, SPObsCache cache) {
        obs.putTransientClientData(CLIENT_DATA_KEY, cache);
    }

    public static Option<TargetCalculator> getTargetCalculator(ISPObservation obs) {
        final SPObsCache cache = getObsCache(obs);
        if (cache == null) return None.instance();
        return cache.getTargetCalculator();
    }

    public static void setTargetCalculator(ISPObservation obs, Option<TargetCalculator> targetCalculator) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) cache = new SPObsCache();
        cache.setTargetCalculator(targetCalculator);
        setObsCache(obs, cache);
    }

    public static Option<Instrument> getInstrument(ISPObservation obs) {
        return ImOption.apply(getObsCache(obs)).flatMap(c -> c.getInstrument());
    }

    public static void setInstrument(ISPObservation obs, Option<Instrument> instrument) {
        final SPObsCache c = ImOption.apply(getObsCache(obs)).getOrElse(() -> new SPObsCache());
        c.setInstrument(instrument);
        setObsCache(obs, c);
    }

    public static ObsClass getObsClass(ISPObservation obs) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) return null;
        return cache.getObsClass();
    }

    public static void setObsClass(ISPObservation obs, ObsClass obsClass) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) cache = new SPObsCache();
        cache.setObsClass(obsClass);
        setObsCache(obs, cache);
    }

    public static ObsTimes getCorrectedObsTimes(ISPObservation obs) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) return null;
        return cache.getCorrectedObsTimes();
    }

    public static void setCorrectedObsTimes(ISPObservation obs, ObsTimes obsTimes) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) cache = new SPObsCache();
        cache.setCorrectedObsTimes(obsTimes);
        setObsCache(obs, cache);
    }

    public static ObsTimes getRawObsTimes(ISPObservation obs) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) return null;
        return cache.getRawObsTimes();
    }

    public static void setRawObsTimes(ISPObservation obs, ObsTimes obsTimes) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) cache = new SPObsCache();
        cache.setRawObsTimes(obsTimes);
        setObsCache(obs, cache);
    }

    public static PlannedTimeSummary getPlannedTime(ISPObservation obs) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) return null;
        return cache.getPlannedTime();
    }

    public static void setPlannedTime(ISPObservation obs, PlannedTimeSummary time) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) cache = new SPObsCache();
        cache.setPlannedTime(time);
        setObsCache(obs, cache);
    }

    public static PlannedStepSummary getPlannedSteps(ISPObservation obs) {
        SPObsCache cache = getObsCache(obs);
		if (cache == null) return null;
		return cache.getPlannedSteps();
	}


    public static void setPlannedSteps(ISPObservation obs, PlannedStepSummary steps) {
        SPObsCache cache = getObsCache(obs);
		if (cache == null) cache = new SPObsCache();
		cache.setPlannedSteps(steps);
		setObsCache(obs, cache);
	}

    public static scala.Option<DataflowStatus> getDatasetDisposition(ISPObservation obs) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) return null;
        return cache.getDatasetDisposition();
    }

    public static void setDatasetDisposition(ISPObservation obs, scala.Option<DataflowStatus> dispo) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) cache = new SPObsCache();
        cache.setDatasetDisposition(dispo);
        setObsCache(obs, cache);
    }

    public static DatasetQaStateSums getDatasetQaStateSums(ISPObservation obs) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) return null;
        return cache.getDasetQaStateSums();
    }

    public static void setDatasetQaStateSums(ISPObservation obs, DatasetQaStateSums sums) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) cache = new SPObsCache();
        cache.setDatasetQaStateSums(sums);
        setObsCache(obs, cache);
    }

    public static Integer getStepCount(ISPObservation obs) {
        final SPObsCache cache = getObsCache(obs);
        if (cache == null) return null;
        return cache.getStepCount();
    }

    public static void setStepCount(ISPObservation obs, Integer stepCount) {
        SPObsCache cache = getObsCache(obs);
        if (cache == null) cache = new SPObsCache();
        cache.setStepCount(stepCount);
        setObsCache(obs, cache);
    }

    // seems a bit bold to cache the entire config sequence, especially given
    // that it will remain in memory forever in the odb once cached

//    public static ConfigSequence getConfigSequence(ISPObservation obs) {
//        final SPObsCache cache = getObsCache(obs);
//        if (cache == null) return null;
//        return cache.getConfigSequence();
//    }
//
//    public static void setConfigSequence(ISPObservation obs, ConfigSequence configSequence) {
//        SPObsCache cache = getObsCache(obs);
//        if (cache == null) cache = new SPObsCache();
//        cache.setConfigSequence(configSequence);
//        setObsCache(obs, cache);
//    }

    // The target calculator.
    private Option<TargetCalculator> _targetCalculator;

    // The observation's instrument, if any.
    private Option<Instrument> _instrument = ImOption.empty();

    // The observation class (The value is determined by examining the sequence
    // and is cached here)
    private ObsClass _obsClass;

    // The ObsClass and the ObsRecord are examined to calculate the obs time,
    // and the value is cached here.
    private ObsTimes _correctedObsTimes;

    // Cache of just the raw obs times, without any corrections applied.
    private ObsTimes _rawObsTimes;

    // Cache of planned time (ms).
    private PlannedTimeSummary _plannedTime;

    // Cache of setup timme, planned time, and exec status for each sequence step
    private PlannedStepSummary _plannedSteps;

    // Cache of the minimum dataflow step for the observation, if any.
    private scala.Option<DataflowStatus> _datasetDisposition;

    // Sums of dataset qa state values among the datasets in this obs.
    private DatasetQaStateSums _qaSums;

    // Count of steps in the execution sequence.
    private Integer _stepCount;

    public Object clone() {
        SPObsCache that;
        try {
            that = (SPObsCache) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new GeminiRuntimeException("clone bug");
        }

        // everything is immutable
        return that;
    }

    /**
     * Return the cached ObsClass, if set.  See also {@link ObsClassService}.
     *
     * @return the cached value for the observation's {@link ObsClass} if set;
     * <code>null</code> otherwise
     */
    public ObsClass getObsClass() {
        return _obsClass;
    }

    /**
     * Set the cached copy of the observation class.  See also
     * {@link ObsClassService}.
     */
    public void setObsClass(ObsClass obsClass) {
        _obsClass = obsClass;
    }

    public Option<TargetCalculator> getTargetCalculator() {
        return _targetCalculator;
    }

    public void setTargetCalculator(Option<TargetCalculator> targetCalculator) {
        _targetCalculator = targetCalculator;
    }

    public Option<Instrument> getInstrument() {
        return _instrument;
    }

    public void setInstrument(Option<Instrument> instrument) {
        _instrument = instrument;
    }

    public ObsTimes getCorrectedObsTimes() {
        return _correctedObsTimes;
    }

    public void setCorrectedObsTimes(ObsTimes obsTimes) {
        _correctedObsTimes = obsTimes;
    }

    public ObsTimes getRawObsTimes() {
        return _rawObsTimes;
    }

    public void setRawObsTimes(ObsTimes obsTimes) {
        _rawObsTimes = obsTimes;
    }

    public PlannedTimeSummary getPlannedTime() {
        return _plannedTime;
    }

    public void setPlannedTime(PlannedTimeSummary plannedTime) {
        _plannedTime = plannedTime;
    }

    public PlannedStepSummary getPlannedSteps() {
		return _plannedSteps;
	}

    public void setPlannedSteps(PlannedStepSummary plannedSteps) {
        _plannedSteps = plannedSteps;
    }

    scala.Option<DataflowStatus> getDatasetDisposition() {
        return _datasetDisposition;
    }

    void setDatasetDisposition(scala.Option<DataflowStatus> dispo) {
        _datasetDisposition = dispo;
    }

    DatasetQaStateSums getDasetQaStateSums() {
        return _qaSums;
    }

    void setDatasetQaStateSums(DatasetQaStateSums sums) {
        _qaSums = sums;
    }

//    public ConfigSequence getConfigSequence() {
//        return _configSequence;
//    }
//
//    public void setConfigSequence(ConfigSequence configSequence) {
//        _configSequence = configSequence;
//    }

    public Integer getStepCount() {
        return _stepCount;
    }

    public void setStepCount(Integer stepCount) {
        _stepCount = stepCount;
    }

    private void _clearCachedValues() {
        _obsClass           = null;
        _correctedObsTimes  = null;
        _rawObsTimes        = null;
        _plannedTime        = null;
        _plannedSteps		= null;
        _datasetDisposition = null;
        _qaSums             = null;
//        _configSequence     = null;
        _stepCount          = null;
        _targetCalculator   = null;
        _instrument         = ImOption.empty();
    }

    public void structureChanged(SPStructureChange change) {
        _clearCachedValues();
    }

    public void propertyChanged(SPCompositeChange change) {
        final String myProp = SPUtil.getTransientClientDataPropertyName(CLIENT_DATA_KEY);
        if (!(myProp.equals(change.getPropertyName())))
        _clearCachedValues();
    }
}
