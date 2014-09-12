package edu.gemini.spModel.obs;

import edu.gemini.pot.sp.ISPObsExecLog;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.config.ConfigBridge;
import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obsrecord.ObsExecStatus;
import edu.gemini.spModel.type.DescribableSpType;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

/**
 * Observation status values.
 */
public enum ObservationStatus implements DisplayableSpType, DescribableSpType {

    PHASE2(        ObsPhase2Status.PI_TO_COMPLETE,      "Phase 2",        "In Phase 2"),
    FOR_REVIEW(    ObsPhase2Status.NGO_TO_REVIEW,       "For Review",     "Ready for review by contact scientist"),
    IN_REVIEW(     ObsPhase2Status.NGO_IN_REVIEW,       "In Review",      "Under review by NGO staff"),
    FOR_ACTIVATION(ObsPhase2Status.GEMINI_TO_ACTIVATE,  "For Activation", "Checked by NGO staff and ready for final verification by Gemini staff"),
    ON_HOLD(       ObsPhase2Status.ON_HOLD,             "On Hold",        "Target of opportunity observations, awaiting target definition"),
    READY(         ObsPhase2Status.PHASE_2_COMPLETE,    "Ready",          "Ready to execute or schedule"),
    ONGOING(       ObsPhase2Status.PHASE_2_COMPLETE,    "Ongoing",        "State while observations are underway"),
    OBSERVED(      ObsPhase2Status.PHASE_2_COMPLETE,    "Observed",       "Exectuted. Data awaiting QA") {
        @Override public boolean isActive() { return false; }
    },
    INACTIVE(      ObsPhase2Status.INACTIVE,            "Inactive",       "Observations that should not be done, but do not want to delete from the programs") {
        @Override public boolean isActive() { return false; }
    }
    ;

    /** The default ObservationStatus value **/
    public static final ObservationStatus DEFAULT = PHASE2;

    private final ObsPhase2Status _phase2;
    private final String _displayValue;
    private final String _description;

    ObservationStatus(ObsPhase2Status phase2, String displayVal, String description) {
        _phase2       = phase2;
        _displayValue = displayVal;
        _description  = description;
    }

    public ObsPhase2Status phase2() {
        return _phase2;
    }

    public String displayValue() {
        return _displayValue;
    }

    public String description() {
        return _description;
    }

    public boolean isActive() { return true; }

    public String toString() {
        return _displayValue;
    }

    public boolean isScheduleable() { return this == READY || this == ONGOING; }

    /** Test to see if the src observation status comes before this observation status **/
    public boolean isGreaterThan(ObservationStatus that) {
        return ordinal() > that.ordinal();
    }

    /** Test to see if the src observation status comes after this observation status **/
    public boolean isLessThan(ObservationStatus that) {
        return ordinal() < that.ordinal();
    }

    /** Return a ObservationStatus by name **/
    public static ObservationStatus getObservationStatus(String name) {
        return getObservationStatus(name, DEFAULT);
    }

    /** Return a ObservationStatus by name with a value to
     *  return upon error
     **/
    public static ObservationStatus getObservationStatus(String name, ObservationStatus nvalue) {
        // For backward compatibility
        if (name.equals("Repeat") || name.equals("Queued") || name.equals("Executing")) {
            return ONGOING;
        } else if (name.startsWith("QA(")) {
            return OBSERVED;
        } else {
            return SpTypeUtil.oldValueOf(ObservationStatus.class, name, nvalue);
        }
    }

    public static ObsExecStatus execStatusFor(ISPObservation obs) {
        try {
            obs.getProgramReadLock();
            final ISPObsExecLog log = obs.getObsExecLog();
            if (log == null) return ObsExecStatus.PENDING;
            final ObsExecLog obj = (ObsExecLog) log.getDataObject();

            Integer stepCount = SPObsCache.getStepCount(obs);
            if (stepCount == null) {
                stepCount = ConfigBridge.extractSequence(obs, null, ConfigValMapInstances.IDENTITY_MAP).size();
                SPObsCache.setStepCount(obs, stepCount);
            }
            return obj.getRecord().getExecStatus(stepCount);
        } finally {
            obs.returnProgramReadLock();
        }
    }

    public static ObservationStatus computeFor(ISPObservation obs) {
        try {
            obs.getProgramReadLock();
            final SPObservation   obj = (SPObservation) obs.getDataObject();
            final ObsPhase2Status p2 = obj.getPhase2Status();

            if (p2 == ObsPhase2Status.PHASE_2_COMPLETE) {
                final ObsExecStatus over = obj.getExecStatusOverride().getOrNull();
                final ObsExecStatus exec = (over == null) ? execStatusFor(obs) : over;

                switch (exec) {
                    case PENDING:  return READY;
                    case ONGOING:  return ONGOING;
                    case OBSERVED: return OBSERVED;
                    default: throw new RuntimeException("Unexpected Exec Status: " + exec);
                }
            } else {
                for (ObservationStatus os : ObservationStatus.values()) if (os.phase2() == p2) return os;
                throw new RuntimeException("Unexpected Phase 2 Status: " + p2);
            }
        } finally {
            obs.returnProgramReadLock();
        }
    }
}
