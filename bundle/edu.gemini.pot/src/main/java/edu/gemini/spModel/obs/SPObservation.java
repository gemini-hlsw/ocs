//
// $Id: SPObservation.java 44878 2012-04-30 23:25:10Z rnorris $
//
package edu.gemini.spModel.obs;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.ags.*;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obsrecord.ObsExecStatus;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.PioParseException;
import edu.gemini.spModel.seqcomp.IObserveSeqComponent;
import edu.gemini.spModel.obs.ObsParamSetCodecs;
import edu.gemini.spModel.target.TargetParamSetCodecs;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.time.ObsTimeCorrection;
import edu.gemini.spModel.time.ObsTimeCorrectionLog;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SpTypeUtil;
import edu.gemini.spModel.util.ObjectUtil;
import edu.gemini.spModel.util.SPTreeUtil;

import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * The SPObservation item.  Data associated with the SPObservation node
 * in a Science Program.
 */
public class SPObservation extends AbstractDataObject implements ISPStaffOnlyFieldProtected {
    private static final Logger LOG = Logger.getLogger(SPObservation.class.getName());

    // for serialization
    private static final long serialVersionUID = 11L;

    private static final String VERSION = "2014A-1";

    public static final String LIBRARY_ID_PROP = "libraryId";

    /** This attribute records the observation priority. */
    public static final String PRIORITY_PROP = "priority";
    public static final String TOO_OVERRIDE_RAPID_PROP = "tooOverrideRapid";

    /** This attribute records the group status of the observation. */
    public static final String INGROUP_PROP = "inGroup";

    /** This attribute records the observation status. */
    public static final String PHASE2_STATUS_PROP = "phase2Status";
    public static final String EXEC_STATUS_OVERRIDE_PROP = "execStatusOverride";

    /* This attribute records the scheduling block */
    public static final String SCHEDULING_BLOCK_PROP = "schedulingBlock";

    /** This attribute records the observation QA state. */
    public static final String QA_STATE_PROP = "qaState";

    /** Records whether or not to override the QA state. */
    public static final String OVERRIDE_QA_STATE_PROP = "overrideQaState";

    public static final String OBS_TIME_CORRECTION_PROP = "obsTimeCorrection";

    public static final String ORIGINATING_TEMPLATE_PROP = "originatingTemplate";

    public static final String AGS_STRATEGY_PROP = "agsStrategyOverride";

    /**
     * Observation user priority.
     */
    public enum Priority implements DisplayableSpType {

        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        ;

        /** The default Priority value **/
        public static Priority DEFAULT = LOW;


        private String _displayValue;

        Priority(String displayVal) {
          _displayValue = displayVal;
       }


        /** Return a Priority by name **/
        static public Priority getPriority(String name) {
            return getPriority(name, DEFAULT);
        }

        /** Return a Priority by name with a value to return upon error **/
        static public Priority getPriority(String name, Priority nvalue) {
            // Handle pre-2008A cases cleanly.
            if ("TOO".equalsIgnoreCase(name)) return HIGH;

            return SpTypeUtil.oldValueOf(Priority.class, name, nvalue);
        }


        public String displayValue() {
            return _displayValue;
        }
    }

    public static final SPComponentType SP_TYPE = SPComponentType.OBSERVATION_BASIC;

    private String _libraryId;

    // Reference to the originating template, if any
    private SPNodeKey originatingTemplate = null;

    // Set to one of the PRIORITIES
    private Priority _priority = Priority.DEFAULT;

    private boolean _isOverrideRapid = false;

    // The status of the observation
    private ObsPhase2Status _phase2Status = ObsPhase2Status.PI_TO_COMPLETE;
    private Option<ObsExecStatus> _execStatusOverride = None.instance();

    // A String indicating if in a group or not.  Not in group initially.
    // The value is the name of the group the observation is in.
    private String _groupName = null;

    // Corrections to apply to the total time used calculated in the obslog
    // component.
    private ObsTimeCorrectionLog _correctionLog = new ObsTimeCorrectionLog();

    // Observation wide dataset state -- independent of the individual
    // dataset state.
    private ObsQaState _obsQaState  = ObsQaState.UNDEFINED;
    private boolean _overrideQaState = false;

    // The next scheduling block for the observation. Begin with None.
    private Option<SchedulingBlock> _schedulingBlock =
            new Some(SchedulingBlock.apply(System.currentTimeMillis()));

    private Option<AgsStrategyKey> _agsStrategyOverride = None.instance();

    /**
     * Default constructor.
     */
    public SPObservation() {
        super(SP_TYPE);
        setVersion(VERSION);
        setPriority(Priority.LOW);
    }

    /**
     * Override clone to make sure the list members are copied correctly.
     */
    public Object clone() {
        final SPObservation obs = (SPObservation)super.clone();
        obs._correctionLog = new ObsTimeCorrectionLog(_correctionLog);
        return obs;
    }

    @Override public boolean staffOnlyFieldsEqual(ISPDataObject to) {
        final SPObservation that = (SPObservation) to;
        return ObjectUtil.equals(_execStatusOverride, that._execStatusOverride) &&
               ObjectUtil.equals(_libraryId, that._libraryId) &&
               _correctionLog.hasSameLog(that._correctionLog) &&
               _obsQaState == that._obsQaState &&
               _overrideQaState == that._overrideQaState;
    }

    @Override public boolean staffOnlyFieldsDefaulted() {
        return staffOnlyFieldsEqual(new SPObservation());
    }

    @Override public void setStaffOnlyFieldsFrom(ISPDataObject to) {
        final SPObservation that = (SPObservation) to;
        _execStatusOverride = that._execStatusOverride;
        _libraryId          = that._libraryId;
        _correctionLog      = new ObsTimeCorrectionLog(that._correctionLog);
        _obsQaState         = that._obsQaState;
        _overrideQaState    = that._overrideQaState;
    }

    @Override public void resetStaffOnlyFieldsToDefaults() {
        setStaffOnlyFieldsFrom(new SPObservation());
    }

    public String getLibraryId() {
        return _libraryId;
    }

    public void setLibraryId(String libraryId) {
        //noinspection StringEquality
        if (libraryId == _libraryId) return;
        if ((libraryId != null) && libraryId.equals(_libraryId)) return;
        String prev = _libraryId;
        _libraryId = libraryId;
        firePropertyChange(LIBRARY_ID_PROP, prev, libraryId);
    }


    /**
     * @return the node key for this observation's originating template (if any)
     */
    public SPNodeKey getOriginatingTemplate() {
        return originatingTemplate;
    }

    public void setOriginatingTemplate(SPNodeKey originatingTemplate) {
        final SPNodeKey prev = this.originatingTemplate;
        this.originatingTemplate = originatingTemplate;
        firePropertyChange(ORIGINATING_TEMPLATE_PROP, prev, originatingTemplate);
    }

    /**
     * Get the observation priority.
     */
    public Priority getPriority() {
        return _priority;
    }

    /**
     * Set the Observation priority.
     */
    public void setPriority(Priority newValue) {
        Priority oldValue = _priority;
        if (oldValue != newValue) {
            _priority = newValue;
            firePropertyChange(PRIORITY_PROP, oldValue, newValue);
        }
    }

    public boolean isOverrideRapidToo() {
        return _isOverrideRapid;
    }

    public void setOverrideRapidToo(boolean override) {
        if (override != _isOverrideRapid) {
            _isOverrideRapid = override;
            firePropertyChange(TOO_OVERRIDE_RAPID_PROP, !override, override);
        }
    }

    /**
     * Gets the observation QA state.
     */
    public ObsQaState getOverriddenObsQaState() {
        return _obsQaState;
    }

    /**
     * Sets the observation QA state.
     *
     * @param newValue new observation QA state, which may not be
     * <code>null</code>
     */
    public void setOverriddenObsQaState(ObsQaState newValue) {
        if (newValue == null) throw new NullPointerException();

        ObsQaState oldValue = _obsQaState;
        if (oldValue != newValue) {
            _obsQaState = newValue;
            firePropertyChange(QA_STATE_PROP, oldValue, newValue);
        }
    }

    /**
     * Returns <code>true</code> if the QA state has been overriden.  In other
     * words, disassociated from the default value based upon the datasets;
     * <code>false</code> if the value has not been overriden.
     */
    public boolean isOverrideQaState() {
        return _overrideQaState;
    }

    public void setOverrideQaState(boolean newValue) {
        boolean oldValue = _overrideQaState;
        if (oldValue != newValue) {
            _overrideQaState = newValue;
            firePropertyChange(OVERRIDE_QA_STATE_PROP, oldValue, newValue);
        }
    }

    /**
     * Get the observation phase 2 status.
     */
    public ObsPhase2Status getPhase2Status() {
        return _phase2Status;
    }

    /**
     * Set the observation status.
     */
    public void setPhase2Status(ObsPhase2Status newValue) {
        if (newValue == null) throw new IllegalArgumentException("ObsPhase2Status cannot be null");
        final ObsPhase2Status oldValue = _phase2Status;
        if (oldValue != newValue) {
            _phase2Status = newValue;
            firePropertyChange(PHASE2_STATUS_PROP, oldValue, newValue);

            if (newValue != ObsPhase2Status.PHASE_2_COMPLETE) {
                setExecStatusOverride(None.<ObsExecStatus>instance());
            }
        }
    }

    /**
     * Get the scheduling block.
     */
    public Option<SchedulingBlock> getSchedulingBlock() {
        return _schedulingBlock;
    }

    /**
     * Get the scheduling block's starting time.
     */
    public Option<Long> getSchedulingBlockStart() {
        return _schedulingBlock.map(SchedulingBlock::start);
    }

    /**
     * Set the scheduling block.
     */
    public void setSchedulingBlock(Option<SchedulingBlock> newValue) {
        if (newValue == null) throw new IllegalArgumentException("SchedulingBlock cannot be null");
        final Option<SchedulingBlock> oldValue = _schedulingBlock;
        if (!oldValue.equals(newValue)) {
            _schedulingBlock = newValue;
            firePropertyChange(SCHEDULING_BLOCK_PROP, oldValue, newValue);
        }
    }

    /**
     * Get the exec status override.
     */
    public Option<ObsExecStatus> getExecStatusOverride() {
        return _execStatusOverride;
    }

    /**
     * Sets the exec status override.
     */
    public void setExecStatusOverride(Option<ObsExecStatus> newValue) {
        if (newValue == null) throw new IllegalArgumentException("ObsExecStatusOverride cannot be null");
        final Option<ObsExecStatus> oldValue = _execStatusOverride;
        if (!oldValue.equals(newValue)) {
            _execStatusOverride = newValue;
            firePropertyChange(EXEC_STATUS_OVERRIDE_PROP, oldValue, newValue);
        }
    }

    /** Return an array of available observation status values. */
    public static ObsPhase2Status[] getObservationStatusChoices(ISPObservation obs, boolean isPI, boolean isNGO, boolean isStaff) {
        // staff can set any status value
        if (isStaff) return ObsPhase2Status.values();

        ObsPhase2Status[] result;
        if (isNGO) {
            // NGOs can set these values
            // SCT-230: if not TOO, include ON_HOLD
            result = new ObsPhase2Status[] {
                ObsPhase2Status.PI_TO_COMPLETE,
                ObsPhase2Status.NGO_TO_REVIEW,
                ObsPhase2Status.NGO_IN_REVIEW,
                ObsPhase2Status.GEMINI_TO_ACTIVATE,
            };
            if (!Too.isToo(obs)) {
                result = new ObsPhase2Status[] {
                    ObsPhase2Status.PI_TO_COMPLETE,
                    ObsPhase2Status.NGO_TO_REVIEW,
                    ObsPhase2Status.NGO_IN_REVIEW,
                    ObsPhase2Status.GEMINI_TO_ACTIVATE,
                    ObsPhase2Status.ON_HOLD,
                };
            }
        } else if (isPI) {
            // PIs can set these values
            final ObsPhase2Status phase2Status = ((SPObservation) obs.getDataObject()).getPhase2Status();
            if (Too.isToo(obs) &&
                (phase2Status == ObsPhase2Status.ON_HOLD || phase2Status == ObsPhase2Status.PHASE_2_COMPLETE)) {
                // When a TOO observation (has priority set) is set to On Hold,
                // the PI can set it directly to Ready.
                result = new ObsPhase2Status[]{
                    ObsPhase2Status.ON_HOLD,
                    ObsPhase2Status.PHASE_2_COMPLETE,
                };
            } else {
                result = new ObsPhase2Status[]{
                    ObsPhase2Status.PI_TO_COMPLETE,
                    ObsPhase2Status.NGO_TO_REVIEW,
                };
            }
        } else {
            result = new ObsPhase2Status[0];
        }
        return result;
    }

    // --

    /** Return true if NGO members should be allowed to edit the observation. */
    public boolean isEditableForNGO() {
        switch (_phase2Status) {
            case PI_TO_COMPLETE:
            case NGO_TO_REVIEW:
            case NGO_IN_REVIEW:
                return true;
            default:
                return false;
        }
    }

    /** Return true if the PI should be allowed to edit the observation. */
    public static boolean isEditableForPI(ISPObservation obs) {
        final ObsPhase2Status phase2Status = ((SPObservation) obs.getDataObject()).getPhase2Status();
        return (phase2Status == ObsPhase2Status.PI_TO_COMPLETE ||
                (Too.isToo(obs) && (phase2Status == ObsPhase2Status.ON_HOLD)));
    }

    /**
     * Determine if the observation requires a guide star to be selected.
     * @param obs the observation
     * @return true if the observation is not a target of opportunity and not a day calibration
     */
    public static boolean needsGuideStar(ISPObservation obs) {
        return (!Too.isToo(obs) || hasDefinedTarget(obs))
                && !ObsClassService.lookupObsClass(obs).equals(ObsClass.DAY_CAL)
                && hasScienceObserve(obs.getSeqComponent());
    }

    // Determines if the observation has a base position with non zero (i.e.,
    // default) coordinates.  An observation with a ToO target doesn't need a
    // guide star obviously but once the ToO is instantiated into a real
    // observation at a determined location then it does need a guide star.
    // With today's target model there isn't a good way to do this so I want to
    // leave this method private and only use it in conjunction with the
    // needsGuideStar check.  With the good target model to come, we should just
    // check whether it is a ToO target and get rid of this method altogether.
    private static boolean hasDefinedTarget(ISPObservation obs) {
        final ISPObsComponent targetComp = SPTreeUtil.findTargetEnvNode(obs);
        if (targetComp == null) {
            return false;
        } else {
            final TargetObsComp toc = (TargetObsComp) targetComp.getDataObject();
            return !toc.getAsterism().isToo();
        }
    }

    /**
     * Determine if the tree rooted at the ISPSeqComponent contains a regular observe of the science class using DFS.
     * @param seqComponent the root of the tree
     * @return true if the tree contains a regular observe of the science class, and false otherwise
     */
    private static boolean hasScienceObserve(ISPSeqComponent seqComponent) {
        if (seqComponent.getType() == SPComponentType.OBSERVER_OBSERVE
                && seqComponent.getDataObject() instanceof IObserveSeqComponent
                && ((IObserveSeqComponent) seqComponent.getDataObject()).getObserveType().equals(InstConstants.SCIENCE_OBSERVE_TYPE))
            return true;

        for (ISPSeqComponent c : seqComponent.getSeqComponents())
            if (hasScienceObserve(c)) return true;
        return false;
    }


    /**
     * Set the name of the group to which this observation belongs
     * (null for no group).
     */
    public void setGroup(String groupName) {
        if (_groupName == null && groupName == null) {
            return;
        }
        if (groupName == null || _groupName == null || ! groupName.equals(_groupName)) {
            String oldGroupName = _groupName;
            _groupName = groupName;
            firePropertyChange(INGROUP_PROP, oldGroupName, _groupName);
        }
    }

    /**
     * Return the name of the group to which this observation belongs, or
     * null if it does not belong to a group.
     * Note that this field is only set in older observations, and is cleared
     * after import. Don't call this method.
     */
    public String getGroup() {
        return _groupName;
    }

    /**
     * Adds the given {@link ObsTimeCorrection} to the log.
     *
     * @param otc correction to add to the list
     */
    public void addObsTimeCorrection(ObsTimeCorrection otc) {
        _correctionLog.add(otc);
        firePropertyChange(OBS_TIME_CORRECTION_PROP, null, null);
    }

    /**
     * Gets the complete log of corrections as an array (which the caller is
     * free to modify).
     *
     * @return corrections to apply, if any; an empty array if there are no
     * corrections
     */
    public ObsTimeCorrection[] getObsTimeCorrections() {
        return _correctionLog.getCorrections();
    }

    /**
     * Sets the complete log of corrections from the given array.  The caller
     * is free to modify the <code>log</code> argument afterwords without
     * impacting the state of this object.
     *
     * @param log corrections to apply, if any; an empty array or
     * <code>null</code> if there are no corrections
     */
    public void setObsTimesCorrections(ObsTimeCorrection[] log) {
        if ((log == null) || (log.length == 0)) {
            if (_correctionLog.size() > 0) {
                _correctionLog = new ObsTimeCorrectionLog();
            }
            return;
        }

        _correctionLog = new ObsTimeCorrectionLog();
        for (ObsTimeCorrection aLog : log) {
            _correctionLog.add(aLog);
        }
    }

    /**
     * Reset the correction log to empty.
     */
    public void resetObsTimeCorrections() {
        _correctionLog = new ObsTimeCorrectionLog();
    }

    /**
     * Gets the sum of all the corrections with the given ChargeClass
     * as a value in milliseconds.
     */
    public long getTotalObsTimeCorrection(ChargeClass chargeClass) {
        return _correctionLog.sumCorrections(chargeClass);
    }

    /**
     * Gets the total of all corrections split into their various
     * {@link ChargeClass}es.
     */
    public ObsTimeCharges sumObsTimeCorrections() {
        return _correctionLog.sumCorrections();
    }

    public Option<AgsStrategyKey> getAgsStrategyOverride() {
        return _agsStrategyOverride;
    }

    public void setAgsStrategyOverride(Option<AgsStrategyKey> s) {
        if (!s.equals(_agsStrategyOverride)) {
            final Option<AgsStrategyKey> old = _agsStrategyOverride;
            _agsStrategyOverride = s;
            firePropertyChange(AGS_STRATEGY_PROP, old, s);
        }
    }

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, LIBRARY_ID_PROP, _libraryId);
        Pio.addParam(factory, paramSet, PRIORITY_PROP, getPriority().name());
        Pio.addBooleanParam(factory, paramSet, TOO_OVERRIDE_RAPID_PROP, isOverrideRapidToo());
        if (_groupName != null) {
            Pio.addParam(factory, paramSet, INGROUP_PROP, _groupName);
        }
        Pio.addParam(factory, paramSet, PHASE2_STATUS_PROP, getPhase2Status().name());
        if (!_execStatusOverride.isEmpty()) {
            Pio.addParam(factory, paramSet, EXEC_STATUS_OVERRIDE_PROP, getExecStatusOverride().getValue().name());
        }

        _schedulingBlock.foreach(sb -> {
            final ParamSet ps = ObsParamSetCodecs.SchedulingBlockParamSetCodec().encode(SCHEDULING_BLOCK_PROP, sb);
            paramSet.addParamSet(ps);
        });

        Pio.addParam(factory, paramSet, QA_STATE_PROP, getOverriddenObsQaState().name());
        Pio.addBooleanParam(factory, paramSet, OVERRIDE_QA_STATE_PROP, _overrideQaState);

        if (originatingTemplate != null) {
            Pio.addParam(factory, paramSet, ORIGINATING_TEMPLATE_PROP, originatingTemplate.toString());
        }

        if (_correctionLog.size() > 0) {
            paramSet.addParamSet(_correctionLog.toParamSet(factory));
        }

        if (!_agsStrategyOverride.isEmpty()) {
            Pio.addParam(factory, paramSet, AGS_STRATEGY_PROP, _agsStrategyOverride.getValue().id());
        }

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        setLibraryId(Pio.getValue(paramSet, LIBRARY_ID_PROP, null));

        String v = Pio.getValue(paramSet, PRIORITY_PROP);
        if (v != null) {
            setPriority(Priority.getPriority(v));
        }

        v = Pio.getValue(paramSet, TOO_OVERRIDE_RAPID_PROP);
        if (v == null) {
            v = Pio.getValue(paramSet, "tooPriority");
            if (TooType.valueOf(v) == TooType.standard) {
                setOverrideRapidToo(true);
            }
        } else {
            if ("true".equalsIgnoreCase(v)) {
                setOverrideRapidToo(true);
            }
        }

        v = Pio.getValue(paramSet, INGROUP_PROP);
        if (v != null) {
            setGroup(v);
        }

        v = Pio.getValue(paramSet, PHASE2_STATUS_PROP);
        if (v != null) {
            setPhase2Status(ObsPhase2Status.valueOf(v));
        }
        v = Pio.getValue(paramSet, EXEC_STATUS_OVERRIDE_PROP);
        if (v == null) {
            setExecStatusOverride(None.instance());
        } else {
            setExecStatusOverride(new Some<>(ObsExecStatus.valueOf(v)));
        }

        // Set the scheduling block if it exists
        final ParamSet sbParamSet = paramSet.getParamSet(SCHEDULING_BLOCK_PROP);
        if (sbParamSet == null) {
            setSchedulingBlock(None.instance());
        } else {
            SchedulingBlock sb = ObsParamSetCodecs.SchedulingBlockParamSetCodec().unsafeDecode(sbParamSet);
            setSchedulingBlock(new Some<>(sb));
        }

        v = Pio.getValue(paramSet, QA_STATE_PROP);
        if (v != null) {
            ObsQaState qaState = ObsQaState.parseType(v);
            if (qaState == null) qaState = ObsQaState.UNDEFINED;
            setOverriddenObsQaState(qaState);
        }

        v = Pio.getValue(paramSet, ORIGINATING_TEMPLATE_PROP);
        if (v != null) {
            originatingTemplate = new SPNodeKey(v);
        }

        // Use a default value of true so that existing observations will keep
        // the QA state assigned to them.
        setOverrideQaState(Pio.getBooleanValue(paramSet, OVERRIDE_QA_STATE_PROP, true));

        ParamSet otcl = paramSet.getParamSet(ObsTimeCorrectionLog.PARAM_SET_NAME);
        if (otcl != null) {
            try {
                _correctionLog = new ObsTimeCorrectionLog(otcl);
            } catch (PioParseException ex) {
                // not clear what to do in this case
                _correctionLog = new ObsTimeCorrectionLog();
                LOG.log(Level.WARNING, "couldn't parse the time correction log", ex);
            }
        }

        v = Pio.getValue(paramSet, AGS_STRATEGY_PROP);
        if (v == null) {
            _agsStrategyOverride = None.instance();
        } else {
            _agsStrategyOverride = ImOption.apply(AgsStrategyKey$.MODULE$.fromStringOrNull(v));
        }
    }
}

