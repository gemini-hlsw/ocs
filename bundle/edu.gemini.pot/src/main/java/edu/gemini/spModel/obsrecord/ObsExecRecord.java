package edu.gemini.spModel.obsrecord;

import edu.gemini.pot.sp.Instrument;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.GeminiRuntimeException;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.dataset.*;
import edu.gemini.spModel.event.*;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.PioParseException;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.time.ObsTimes;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.Serializable;
import java.util.*;

/**
 * The ObsRecord contains record of the events, datasets, and the
 * configurations that produced the datasets for an observation.
 */
public final class ObsExecRecord implements Serializable {
    private static final Logger LOG = Logger.getLogger(ObsExecRecord.class.getName());

    private static final long serialVersionUID = 2L;

    public static final String PARAM_SET = "obsExecRecord";
    public static final String DATASETS_PARAM_SET = "datasets";
    public static final String EVENTS_PARAM_SET   = "events";


    // Map from DatasetLabel to DatasetRecord.
    private final TreeMap<DatasetLabel, DatasetExecRecord> _datasets = new TreeMap<>();

    // Events, sorted and separated into distinct visits.
    private final PrivateVisitList _visits;

    // Handles association between configs and dataset labels.
    private final ConfigStore _configStore;

    // A dataset is tentative after the start dataset event, until its
    // end dataset event is received.  Tentative datasets are removed from
    // the data structures when an event is received that indicates the
    // end dataset is not coming.
    private Dataset _tentativeDataset;
    private Config  _tentativeConfig;

    /**
     * Constructs and empty ObsRecord, with no datasets, events, or
     * configurations.
     */
    public ObsExecRecord() {
        this(new PrivateVisitList(), new CompressedConfigStore());
    }

    private ObsExecRecord(PrivateVisitList visits, CompressedConfigStore configStore) {
        _visits      = visits;
        _configStore = configStore;
    }

    /**
     * Creates an ObsRecord that is a copy of this one.
     */
    public synchronized ObsExecRecord copy() {
        final ObsExecRecord that = new ObsExecRecord(new PrivateVisitList(_visits),
                                                     new CompressedConfigStore(_configStore));

        // Copy the datasets.
        for (Map.Entry<DatasetLabel, DatasetExecRecord> me : _datasets.entrySet()) {
            final DatasetLabel   label = me.getKey();
            final DatasetExecRecord dr =  me.getValue();
            that._datasets.put(label, dr); // DatasetExecRec is immutable
        }

        // Copy the tentative info
        that._tentativeDataset = _tentativeDataset;
        if (_tentativeConfig != null) {
            that._tentativeConfig  = new DefaultConfig(_tentativeConfig);
        }

        return that;
    }

    public boolean isEmpty() {
        return (_visits.getLastEventTime() <= 0) && _datasets.isEmpty();
    }

    /**
     * Creates an ObsRecord from its corresponding representation as a
     * {@link ParamSet}.
     *
     * @param paramSet the parameter set information describing this ObsRecord
     *
     * @throws PioParseException if there is a problem parsing the
     * parameter set information
     */
    public ObsExecRecord(ParamSet paramSet) throws PioParseException {

        // Add the dataset records.
        ParamSet datasetsParamSet = paramSet.getParamSet(DATASETS_PARAM_SET);
        if (datasetsParamSet != null) {
            List<ParamSet> lst = datasetsParamSet.getParamSets(DatasetExecRecord.ParamSet());
            for (ParamSet datasetParamSet : lst) {
                DatasetExecRecord dr = DatasetExecRecord.apply(datasetParamSet);
                _datasets.put(dr.label(), dr);
            }
        }

        // Add the configs.
        ParamSet configMapParamSet = paramSet.getParamSet(SimpleConfigStore.CONFIG_MAP_PARAM_SET);
        _configStore = new CompressedConfigStore(configMapParamSet);

        // Add the events.
        _visits = new PrivateVisitList();
        ParamSet eventsParamSet = paramSet.getParamSet(EVENTS_PARAM_SET);
        if (eventsParamSet != null) {
            List<ParamSet> lst = eventsParamSet.getParamSets(ObsExecEvent.PARAM_SET);
            for (ParamSet eventParamSet : lst) {
                final ExecEvent evt = ExecEvent.create(eventParamSet);
                if (!(evt instanceof ObsExecEvent)) {
                    throw new PioParseException("unexpected event type: " + evt.getClass());
                }
                _visits.add((ObsExecEvent) evt);
            }
        }
    }

    public synchronized ObsExecStatus getExecStatus(int stepCount) {
        if (_visits.getLastEventTime() == 0) return ObsExecStatus.PENDING;

        if (stepCount > _datasets.size()) return ObsExecStatus.ONGOING;
        else return _visits.getObsExecStatus();
//        return (stepCount <= _datasets.size()) ? ObsExecStatus.OBSERVED : ObsExecStatus.ONGOING;
    }

    public synchronized ParamSet toParamSet(PioFactory factory) {
        ParamSet pSet = factory.createParamSet(PARAM_SET);

        // Add the dataset records.
        ParamSet datasetsParamSet = factory.createParamSet(DATASETS_PARAM_SET);
        for (DatasetExecRecord dr : _datasets.values()) {
            datasetsParamSet.addParamSet(dr.pset(factory));
        }
        pSet.addParamSet(datasetsParamSet);

        // Add the events.
        List<ObsExecEvent> events = _visits.getAllEventList();
        ParamSet eventsParamSet = factory.createParamSet(EVENTS_PARAM_SET);
        for (ObsExecEvent evt : events) {
            eventsParamSet.addParamSet(evt.toParamSet(factory));
        }
        pSet.addParamSet(eventsParamSet);

        // Add configurations.
        ParamSet configsParamSet = _configStore.toParamSet(factory);
        pSet.addParamSet(configsParamSet);

        return pSet;
    }

    /**
     * Gets the {@link Site} for the site at which this observation was executed.
     */
    public synchronized Site getSite() {
        Site sd = _visits.divineSite();
        if (sd == null) {
            // sketchy guess based upon timezone ...
            TimeZone tz = TimeZone.getDefault();
            if (tz.hasSameRules(Site.GS.timezone())) {
                sd = Site.GS;
            } else {
                sd = Site.GN;
            }
        }
        return sd;
    }

    /**
     * Actions to perform when an event is received (before adding the event
     * to the event list).
     */
    private class AddEventAction implements ExecAction {
        private Config _config;

        AddEventAction(Config config) {
            _config = config;
        }

        public void abortObserve(ExecEvent event) {
            _cleanupTentativeDataset();
        }

        public void overlap(ExecEvent event) {
            // do nothing
        }

        public void pauseObserve(ExecEvent event) {
            // do nothing
        }

        public void continueObserve(ExecEvent event) {
            // do nothing
        }

        public void stopObserve(ExecEvent event) {
            // do nothing: (changed for OCS-84)
        }

        public void startVisit(ExecEvent event) {
            _cleanupTentativeDataset();
        }

        public void slew(ExecEvent event) {
            _cleanupTentativeDataset();
        }

        public void startSequence(ExecEvent event) {
            _cleanupTentativeDataset();
        }

        public void startDataset(ExecEvent event) {
            StartDatasetEvent sde = (StartDatasetEvent) event;
            Dataset dataset = sde.getDataset();
            _handleStartDataset(dataset, _config);
        }

        public void endDataset(ExecEvent event) {
            EndDatasetEvent ede = (EndDatasetEvent) event;
            _handleEndDataset(ede.getDatasetLabel());
        }

        public void endSequence(ExecEvent event) {
            _cleanupTentativeDataset();
        }

        public void endVisit(ExecEvent event) {
            _cleanupTentativeDataset();
        }

        public void startIdle(ExecEvent event) {
            LOG.log(Level.WARNING, "Received startIdle event in ObsRecord");
            throw new GeminiRuntimeException("Received startIdle event");
        }

        public void endIdle(ExecEvent event) {
            LOG.log(Level.WARNING, "Received endIdle event in ObsRecord");
            throw new GeminiRuntimeException("Received endIdle event");
        }
    }

    private synchronized void _handleStartDataset(Dataset dataset, Config config) {
        _cleanupTentativeDataset();

        final DatasetLabel label = dataset.getLabel();
        _tentativeDataset = dataset;
        if (config == null) config = new DefaultConfig();
        _tentativeConfig  = config;

        LOG.info(String.format("Now expecting end dataset for '%s'.", label));

        // Add the dataset to the map, creating the DatasetRecord wrapper
        // for it.  Only do this for new datasets though. It would be best
        // to wait for the endDataset event, but we need to allow comments
        // to be entered with the datasets, and we need to allow this before
        // the dataset has been completed.
        final DatasetExecRecord existingRec = _datasets.get(label);
        if (existingRec == null) {
            // Create the new DatasetRecord.  It will be tentative until
            // confirmed by the Data Manager.
            final DatasetExecRecord rec = DatasetExecRecord.apply(dataset);
            _datasets.put(label, rec);
            _configStore.addConfigAndLabel(config, label);
        }
    }

    private synchronized void _handleEndDataset(DatasetLabel label) {
        // expect the _tentativeLabel to be equal to the end dataset
        // event's label, if not print a warning and continue
        if (_tentativeDataset == null) {
            LOG.log(Level.WARNING, "Received an end dataset for '" + label +
                     "', but wasn't expecting it");
            return;
        }
        if (!label.equals(_tentativeDataset.getLabel())) {
            LOG.log(Level.WARNING, "Received an end dataset for '" + label +
                     "', but expected '" + _tentativeDataset.getLabel() + "'");
            _cleanupTentativeDataset();
            return;
        }

        // Update the existing dataset with the "tentative" information.
        DatasetExecRecord rec = _datasets.get(label);
        if (rec == null) {
            LOG.log(Level.WARNING, "Received an end dataset for '" + label +
                     "', but there is no longer a record for it.");
            _cleanupTentativeDataset();
            return;
        }
        _configStore.addConfigAndLabel(_tentativeConfig, label);
        _tentativeDataset = null;
        _tentativeConfig  = null;
        LOG.info(String.format("Processed end dataset for '%s'.", label));
    }

    // Called to cleanup data structures when we don't receive the events
    // we expect.
    private synchronized void _cleanupTentativeDataset() {
        if (_tentativeDataset == null) return;
        DatasetLabel label = _tentativeDataset.getLabel();
        LOG.log(Level.WARNING, "Never received endDataset for: " + label);

        try {
            DatasetExecRecord rec = _datasets.get(label);
            if (rec != null) {
                if (rec.summit().isAvailable()) {
                    // no longer tentative -- must exist in the working store
                    return;
                }
            }

            _configStore.remove(label);
            _datasets.remove(label);
        } finally {
            _tentativeDataset = null;
            _tentativeConfig  = null;
        }
    }

    /**
     * Adds an ObsExecEvent to this ObsRecord.  If the event is a
     * {@link StartDatasetEvent}, then the <code>config</code> parameter is
     * associated with the dataset included in the event.
     *
     * @param evt new observation execution event
     * @param config configuration to associate with the dataset contained
     * in <code>evt</code>, if it is a {@link StartDatasetEvent}
     */
    public synchronized void addEvent(ObsExecEvent evt, Config config) {
        evt.doAction(new AddEventAction(config));
        _visits.add(evt);
    }

    /**
     * Gets the {@link ObsVisit}s for this record.  The caller is free to
     * modify the returned array without impacting the state of this object.
     *
     * @return array of ObsVisit corresponding to the visits that this
     * observation has seen
     */
    public synchronized ObsVisit[] getVisits(Option<Instrument> instrument, ObsClass oc, ObsQaRecord qa) {
        return _visits.getObsVisits(instrument, oc, qa, _configStore);
    }

    /**
     * Gets the {@link ObsVisit}s for this ObsRecord whose start time falls
     * in the given range.
     *
     * @param startTime start of the time range of interest (inclusive)
     * @param endTime end of the time range of interest (exclusive)
     *
     * @return {@link ObsVisit}s whose start time falls between
     * <code>startTime</code> (inclusive) and <code>endTime</code> exclusive
     */
    public synchronized ObsVisit[] getVisits(Option<Instrument> instrument, ObsClass oc, ObsQaRecord qa, long startTime, long endTime) {
        return _visits.getObsVisits(instrument, oc, qa, _configStore, startTime, endTime);
    }

    /**
     * Gets the time of the last event that was recorded for this observation,
     * if any.
     */
    public synchronized long getLastEventTime() {
        return _visits.getLastEventTime();
    }

    /**
     * Gets the total time used by all visits, regardless of
     * {@link edu.gemini.spModel.time.ChargeClass}.  Time between visits is
     * ignored. This value represents the real time that was spent executing
     * this observation, to date.
     *
     * @return sum of the times used by each visit
     */
    public synchronized long getTotalTime() {
        return _visits.getTotalTime();
    }

    /**
     * Gets the time used by this observation broken down into the various
     * charge classes.  The <code>mainChargeClass</code> must be specified in
     * order to know to which category the bulk of the total time should be
     * associated.
     *
     * @param mainChargeClass the charge class to which the bulk of the
     * time should be charged
     *
     * @return total times which should be charged to the various categories
     * (see {@link ChargeClass})
     */
    public ObsTimeCharges getTimeCharges(
        Option<Instrument> instrument,
        ObsClass           oc,
        ObsQaRecord        qa,
        ChargeClass        mainChargeClass
    ) {
        return _visits.getTimeCharges(instrument, oc, mainChargeClass, qa, _configStore);
    }

    /**
     * Gets the {@link ObsTimes} (uncorrected by manual adjustments) as
     * calculated based upon the events recorded in this ObsRecord.
     *
     * @param mainChargeClass charge class for the observation as a whole;
     * the bulk of the execution time will be applied to this charge class
     * with adjustments being made for datasets of other charge classes
     *
     * @return calculated (uncorrected) observing times for this observation
     */
    public ObsTimes getTimes(
        Option<Instrument> instrument,
        ObsClass           oc,
        ObsQaRecord        qa,
        ChargeClass        mainChargeClass
    ) {
        return new ObsTimes(getTotalTime(), getTimeCharges(instrument, oc, qa, mainChargeClass));
    }

    /**
     * Gets the highest priority dataflow status associated with the
     * datasets produced by this observation, if any.  The idea is to determine
     * the minimum amount of dataflow progress that has been made by the
     * observation's datasets as a whole.
     *
     * @return the least advanced dataflow step for any dataset associated with
     * the observation; <code>null</code> if there are no datasets
     */
    public synchronized scala.Option<DataflowStatus> getMinimumDisposition(ObsQaRecord qaRecord) {
        return DataflowStatus$.MODULE$.rollUp(qaRecord.datasetRecordsFromJava(_datasets.values()));
    }

    /**
     * Gets the DatasetRecord associated with the given <code>label</code>.
     *
     * @param label identifies the DatasetRecord to retrieve
     *
     * @return DatasetRecord associated with <code>label</code>
     */
    public synchronized DatasetExecRecord getDatasetExecRecord(DatasetLabel label) {
        return label == null ? null : _datasets.get(label);
    }

    /**
     * Adds or replaces the DatasetRecord associated with the
     * {@link DatasetLabel} of the given <code>record</code>.  Typically,
     * DatasetRecords are created in response to the arrival of
     * {@link StartDatasetEvent}s.  However, if the event is not received or
     * is thrown away due to event ordering problems, this method allows
     * {@link edu.gemini.spModel.dataset.DatasetExecRecord}s to be added.
     *
     * <p>Nothing guarantees that the supplied record even pertains to the
     * observation that contains this record, so care must be taken
     *
     * @param record dataset that will either be added to the observing
     * record, or will replace the existing record with the same label
     *
     * @param config configuration to associate with the record; if
     * <code>null</code> then any existing configuration information is
     * retained
     */
    public synchronized void putDatasetExecRecord(DatasetExecRecord record, Config config) {
        DatasetLabel label = record.label();
        _datasets.put(label, record);

        if (config == null) {
            // Caller did not specify a config.  So don't replace an existing
            // config if there is one, but do add an empty one if necessary.
            if (!_configStore.containsDataset(label)) {
                config = new DefaultConfig();
                _configStore.addConfigAndLabel(config, label);
            }
        } else {
            // Caller specified a config, so use it.
            _configStore.addConfigAndLabel(config, label);
        }
    }

    /**
     * Removes the record and configuration information associated with the
     * given <code>label</code>.  This method is not typically used during
     * normal operations.
     * @param label label of the dataset to remove
     */
    public synchronized void removeDatasetRecord(DatasetLabel label) {
        _datasets.remove(label);
        _configStore.remove(label);
    }


    /**
     * Determines whether the dataset associated with <code>label</code> has
     * been completed.
     *
     * @param label identifies the dataset to check for completeness
     *
     * @return <code>true</code> if the associated dataset is complete,
     * <code>false</code> otherwise
     */
    public synchronized boolean inSummitStorage(DatasetLabel label) {
        DatasetExecRecord rec = _datasets.get(label);
        return (rec != null) && rec.summit().isAvailable();
    }

    /**
     * Returns <code>true</code> if the given label is currently being
     * executed.  In other words, if we have received the start dataset event
     * but not the end dataset event.
     */
    public synchronized boolean isInProgress(DatasetLabel label) {
        return _tentativeDataset != null && label.equals(_tentativeDataset.getLabel());
    }

    /**
     * Gets all the dataset records for datasets that have been recorded by
     * this observation.
     *
     * @return array of DatasetRecord for datasets associated with this
     * observation
     */
    public synchronized List<DatasetExecRecord> getAllDatasetExecRecords() {
        return new ArrayList<>(_datasets.values());
    }

    public synchronized SortedSet<DatasetLabel> getDatasetLabels() {
        return new TreeSet<>(_datasets.keySet());
    }

    /**
     * Gets the number of datasets contained in the ObsRecord.
     */
    public synchronized int getDatasetCount() {
        return _datasets.size();
    }

    /**
     * Gets the (unmodifiable) configuration that was in effect when the
     * dataset with the given <code>label</code> was started.
     *
     * @param label dataset label for the dataset whose configuration should
     * be returned
     *
     * @return Config that was used to configure the telescope at the time
     * that the dataset with the given <code>label</code> was created;
     * <code>null</code> if there is no configuration information
     */
    public synchronized Config getConfigForDataset(DatasetLabel label) {
        return _configStore.getConfigForDataset(label);
    }

    /**
     * Gets the ObsClass associated with the given dataset.  This will be the
     * ObsClass that was present on the observe iterator that produced the
     * dataset at the time that the dataset was produced.
     *
     * @param label dataset label for the dataset whose associated ObsClass
     * should be returned
     *
     * @return ObsClass that was present on the observe iterator from which the
     * dataset associated with <code>label</code> was generated at the time
     * that the dataset was generated
     */
    public synchronized ObsClass getObsClassForDataset(DatasetLabel label) {
        return _configStore.getObsClass(label);
    }

}
