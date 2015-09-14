// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: GemObservationCB.java 46871 2012-07-20 21:37:21Z swalker $
//

package edu.gemini.spModel.gemini.obscomp;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.config.MetaDataConfig;
import edu.gemini.spModel.config.ObservationCB;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.dataflow.GsaAspect;
import edu.gemini.spModel.dataflow.GsaSequenceEditor;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.gemini.calunit.calibration.CalConfigBuilderUtil;
import edu.gemini.spModel.gemini.calunit.calibration.CalConfigFactory;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKeyProvider;
import edu.gemini.spModel.gemini.seqcomp.smartgcal.SmartgcalSysConfig;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obsrecord.ObsExecRecord;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsCompConstants;

import java.text.DecimalFormat;
import java.util.*;


/**
 * The {@link edu.gemini.spModel.config.IConfigBuilder configuration builder} for an observation node.
 * When an observation is executed, this configuration builder is called
 * upon to step through the available configurations.  It makes use of the
 * configuration builders associated with the contained observation and
 * sequence components.
 *
 * <p> In the first step, two things occur
 * <ol>
 *   <li>the contained observation component's builders (if any) are used
 *       to set the static configuration
 *   <li>the contained sequence component's builder (if any) is used to
 *       set the first step of the sequence.
 * </ol>
 *
 * <p>Subsequent steps call upon the sequence configuration builder
 * successively until all the iteration steps have been completed.
 * <p>This configuration builder has been specialized for Gemini.
 * It manipulates the configurations at each step to make sure that
 * the configurations match the "time line" and that there is always
 * a clear value for ncoadds and exposureTime.
 */
public class GemObservationCB extends ObservationCB {
    /** Observation status: ready to execute */
    public static final String READY = "ready";

    /** Observation status: completed */
    public static final String COMPLETE = "complete";

    /** Observation status: skipped */
    public static final String SKIPPED = "skipped";

    private transient ObsState     _obsState = null;
    private transient ObsContext _obsContext = null;

    private static final String OCS_SYSTEM = "ocs";

    public Object clone() {
        GemObservationCB result = (GemObservationCB) super.clone();
        result._obsState   = null;
        result._obsContext = null;
        return result;
    }


    // A collection of context items.
    private static class ObsContext {
        private static Option<Integer> getScienceBand(ISPObservation obs) {
            final SPProgram p = (SPProgram) obs.getProgram().getDataObject();
            return ImOption.apply(p.getQueueBand()).flatMap(new MapOp<String, Option<Integer>>() {
                @Override public Option<Integer> apply(String s) {
                    try {
                        return new Some<>(Integer.parseInt(s));
                    } catch (NumberFormatException ex) {
                        return None.INTEGER;
                    }
                }
            });
        }

        private final SPObservationID _obsId;
        private final Option<Integer> _scienceBand;
        private final SPInstObsComp _inst;
        private final ObsExecRecord _obsExecRecord;
        private final ConfigSequence _completedSteps;

        ObsContext(ISPObservation obs)  {

            // Get an observation id string, if possible, "" otherwise.
            _obsId = obs.getObservationID();

            _scienceBand = getScienceBand(obs);

            // Loop through the obs components extracting the required info.
            SPInstObsComp inst  = null;
            for (ISPObsComponent obsComp : obs.getObsComponents()) {
                Object dataObj = obsComp.getDataObject();
                if (dataObj instanceof SPInstObsComp) {
                    inst = (SPInstObsComp) dataObj;
                }
            }

            final ISPObsExecLog lc            = obs.getObsExecLog();
            final ObsExecLog log              = (lc == null) ? null : (ObsExecLog) lc.getDataObject();
            final ObsExecRecord obsExecRecord = (log == null) ? null : log.getRecord();
            final ConfigSequence seq          = (log == null) ? null : log.getCompletedSteps();

            _inst           = inst;
            _obsExecRecord  = obsExecRecord;
            _completedSteps = (seq != null) ? seq : new ConfigSequence();
        }

        SPObservationID getObservationId() {
            return _obsId;
        }

        Option<Integer> getScienceBand() {
            return _scienceBand;
        }

        SPInstObsComp getInstrument() {
            return _inst;
        }

        ObsExecRecord getObsRecord() {
            return _obsExecRecord;
        }

        Config[] getCompletedSteps() {
            return _completedSteps.getAllSteps();
        }
    }

    // A private class to retain the observation related parameters
    private static class ObsState {
        private static final String EMPTY_STRING = "";
        private final DecimalFormat _fmt = new DecimalFormat("000");

        private Object _exposureTime = null;
        private String _objectName = EMPTY_STRING;
        private Integer _ncoadds = null;
        private int _dataLabelCounter;

        // For OT-516: Used to Replace status READY with SKIPPED if followed by a COMPLETE
        private List _statusParamList = new ArrayList();

        // Constructor to initialize the data label counter
        ObsState() {
            _dataLabelCounter = 1;
        }

        // Reset the dataLabel counter
        void reset() {
            _dataLabelCounter = 1;
            _statusParamList = new ArrayList();
        }

        void advanceDataLabelCounter() {
            ++_dataLabelCounter;
        }

        // Get dataset id based upon the current observation id
        DatasetLabel getDataLabel(SPObservationID obsId) {
            return (obsId == null) ? null : new DatasetLabel(obsId, _dataLabelCounter);
        }

        // Return the observed status of the given observation as a string:
        // READY or COMPLETE.
        String getStatus(ObsExecRecord rec, DatasetLabel label) {
            if (rec == null) return READY;

            if (rec.getDatasetExecRecord(label) == null) {
                return READY;
            }
            if (rec.isInProgress(label)) {
                return READY;
            }

            return COMPLETE;
        }

        // Is the step complete, i.e., executed?
        boolean isComplete(ObsContext ctx) {
            final SPObservationID obsId = ctx.getObservationId();
            if (obsId == null) return false;
            return COMPLETE.equals(getStatus(ctx.getObsRecord(), getDataLabel(obsId)));
        }

        void initConfig(IConfig current, IConfig prev, ObsContext ctx) {
            MetaDataConfig mdc = MetaDataConfig.extract(current);
            mdc.setStepCount(_dataLabelCounter);
            if (ctx.getInstrument() instanceof CalibrationKeyProvider) {
                SmartgcalSysConfig.extract(current).setCalibrationKeyProvider((CalibrationKeyProvider)ctx.getInstrument());
            }
            SmartgcalSysConfig.extract(current).setMappingError(false);

            boolean complete = isComplete(ctx);
            mdc.setComplete(complete);

            if (complete) {
                Config[] configs = ctx.getCompletedSteps();
                int index = _dataLabelCounter-1;
                if (configs.length > index) {
                    Config c = CalConfigFactory.minimal(configs[index]);
                    CalConfigBuilderUtil.updateIConfig(c, current, prev);

                    // Must tell SeqRepeatSmartGcalObsCB how many executed
                    // steps to expect.
                    SmartgcalSysConfig.extract(current).setExecutedSteps(configs, index);
                }
            }
        }

        // This method takes the resultant apply config and updates
        // its internal store, or if there is an observe with no exposure
        // time and coadds, it adds them.
        void updateConfig(IConfig config, ObsContext ctx) {
            if ((config == null) || (ctx == null)) return;

            SPInstObsComp inst = ctx.getInstrument();
            if (inst != null) inst.updateConfig(config);

            SPObservationID obsId = ctx.getObservationId();

            ISysConfig sc = config.getSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
            if (sc != null) {
                if (sc.containsParameter(InstConstants.EXPOSURE_TIME_PROP)) {
                    Object exTime = sc.getParameterValue(InstConstants.EXPOSURE_TIME_PROP);
                    // If there is a new, non-null, non-empty value, update
                    if (exTime != null) updateExposureTime(exTime);
                }

                // Same for ncoadds
                if (sc.containsParameter(InstConstants.COADDS_PROP)) {
                    Integer ncoadds = (Integer) sc.getParameterValue(InstConstants.COADDS_PROP);
                    // If there is a new, non-null, non-empty value, update
                    if (ncoadds != null) updateCoadds(ncoadds);
                }
            }

            // Get the object name of the base position
            sc = config.getSysConfig(SeqConfigNames.TELESCOPE_CONFIG_NAME);
            if (sc != null) {
                if (sc.containsParameter(TargetEnvironment.BASE_NAME)) {
                    ISysConfig sc2 = (ISysConfig) sc.getParameterValue(TargetEnvironment.BASE_NAME);
                    if (sc2 != null) {
                        String objectName = (String) sc2.getParameterValue(TargetObsCompConstants.NAME_PROP);
                        if ((objectName != null) && (!objectName.equals(EMPTY_STRING))) {
                            updateObjectName(objectName);
                        }
                    }
                }
            }

            // Now check for observe and update if needed
            sc = config.getSysConfig(SeqConfigNames.OBSERVE_CONFIG_NAME);
            if (sc == null) {
                sc = new DefaultSysConfig(SeqConfigNames.OBSERVE_CONFIG_NAME);
                config.appendSysConfig(sc);
            }
            String observeType = (String) sc.getParameterValue(InstConstants.OBSERVE_TYPE_PROP);
            // Add the dataset label in
            DatasetLabel datasetLabel = getDataLabel(obsId);
            final String labelStr = (datasetLabel == null) ? _fmt.format(_dataLabelCounter) : datasetLabel.toString();
            IParameter dlabel = StringParameter.getInstance(InstConstants.DATA_LABEL_PROP, labelStr);
            sc.putParameter(dlabel);

            // Add the status label in, if applicable
            String status = getStatus(ctx.getObsRecord(), datasetLabel);
            if (status != null) {
                IParameter statusParam = StringParameter.getInstance(InstConstants.STATUS_PROP, status);
                _checkStatus(statusParam);
                sc.putParameter(statusParam);
            }

            final ISysConfig obsConfig = sc;
            ctx.getScienceBand().foreach(band -> {
                final IParameter p = DefaultParameter.getInstance(InstConstants.SCI_BAND, band);
                obsConfig.putParameter(p);
            });

            if (observeType != null && observeType.equals(InstConstants.SCIENCE_OBSERVE_TYPE)) {
                if (!sc.containsParameter(InstConstants.OBJECT_PROP)) {
                    String value = getObjectName();
                    if (!value.equals(EMPTY_STRING)) {
                        IParameter ip = StringParameter.getInstance(InstConstants.OBJECT_PROP, value);
                        sc.putParameter(ip);
                    }
                }
                if (!sc.containsParameter(InstConstants.EXPOSURE_TIME_PROP)) {
                    // Add in the exposure time
                    // Check for a blank value, this can happen if there is
                    // no obs component that gives a default value
                    Object value = getExposureTime();
                    if (value != null) {
                        IParameter ip = DefaultParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP, value);
                        sc.putParameter(ip);
                    }
                }
                if (!sc.containsParameter(InstConstants.COADDS_PROP)) {
                    Integer value = getCoadds();
                    if (value != null) {
                        IParameter ip = DefaultParameter.getInstance(InstConstants.COADDS_PROP, value);
                        sc.putParameter(ip);
                    }
                }
            }
        }

        // OT-516: Replace READY status with SKIPPED, if followed by a COMPLETE.
        // See SequenceOutputXML._buildSequence() and TextSequenceFunctor._makeDataVector()
        // for another part of this. This is going back and changing the value of any READY
        // status parameters to SKIPPED, since READY should only come after the last
        // COMPLETE. This seems like a bit of a hack, since the parameter values are being
        // changed after they were added to the ISysConfig, but at least its well commented...
        private void _checkStatus(IParameter statusParam) {
            if (statusParam.getValue().equals(COMPLETE)) {
                for (Object obj : _statusParamList) {
                    IParameter p = (IParameter) obj;
                    if (p.getValue().equals(READY)) {
                        p.setValue(SKIPPED);
                    }
                }
            }

            //noinspection unchecked
            _statusParamList.add(statusParam);
        }

        // Update the exposureTime
        void updateExposureTime(Object xtime) {
            _exposureTime = xtime;
        }

        // Get the exposureTime
        Object getExposureTime() {
            return _exposureTime;
        }

        // Get the target object name
        String getObjectName() {
            return _objectName;
        }

        // update the target object name
        void updateObjectName(String name) {
            _objectName = name;
        }

        // Update the coadds
        void updateCoadds(Integer ncoadds) {
            _ncoadds = ncoadds;
        }

        // Get the coadds
        Integer getCoadds() {
            return _ncoadds;
        }
    }


    /**
     * The public reset method of ObservationCB sets a reset
     * to all the obs components that contain a configuration builder
     * and then recursively to all sequence components that have
     * configuration builders.
     */
    public void reset(Map options)  {
        super.reset(options);

        ISPObservation obs = _getObsNode();

        _obsContext = new ObsContext(obs);
        getObsState().reset();
    }

    // A private routine to manage the creation of the transient
    // ObsContext
    private ObsState getObsState() {
        if (_obsState == null) _obsState = new ObsState();
        return _obsState;
    }

    /**
     * Constructs with the observation that will be sequenced.
     */
    public GemObservationCB(ISPObservation obs) {
        super(obs);
    }

    // private routine to add the special ocs config
    // ****** This needs to be handled better with a hidden component, etc.
    private void _addOCSSystem(IConfig config)  {
        ISPObservation obs = _getObsNode();

        ISysConfig dc = config.getSysConfig(OCS_SYSTEM);
        if (dc == null) {
            dc = new DefaultSysConfig(OCS_SYSTEM);
            // This puts the OCS at the top -- Arghh!
            config.putSysConfig(dc);
        }

        SPProgramID progId = obs.getProgramID();
        String progIdStr = (progId == null) ? "" : progId.stringValue();
        SPObservationID obsId = obs.getObservationID();
        String obsIdStr = (obsId == null) ? "" : obsId.stringValue();

        IParameter param = StringParameter.getInstance(InstConstants.PROGRAMID_PROP, progIdStr);
        dc.putParameter(param);
        param = StringParameter.getInstance(InstConstants.OBSERVATIONID_PROP, obsIdStr);
        dc.putParameter(param);

        // Add proprietary metadata flag
        final GsaAspect gsa = GsaAspect.lookup(obs.getProgram());
        GsaSequenceEditor.instance.addHeaderVisibility(config, gsa);
    }

    @Override
    public void applyNext(IConfig config, IConfig prevFull)  {
        // Check for the first time before the super class makes it false
        boolean isFirstTime = _isFirstTime();

        ObsState os = getObsState();
        os.initConfig(config, prevFull, _obsContext);

        super.applyNext(config, prevFull);

        // If is the first time, add the ocs config
        if (isFirstTime) _addOCSSystem(config);

        // Update the configuration if needed
        os.updateConfig(config, _obsContext);
        os.advanceDataLabelCounter();
    }
}
