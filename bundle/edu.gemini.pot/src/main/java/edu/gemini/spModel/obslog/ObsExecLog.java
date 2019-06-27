//
// $Id: ObsLogDataObject.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.obslog;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.config.ConfigBridge;
import edu.gemini.spModel.config.DatasetConfigService;
import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.config.map.ConfigValMapUtil;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.event.ObsExecEvent;
import edu.gemini.spModel.gemini.calunit.calibration.CalConfigFactory;
import edu.gemini.spModel.gemini.calunit.calibration.CalConfigPio;
import edu.gemini.spModel.gemini.init.SimpleNodeInitializer;
import edu.gemini.spModel.obsrecord.ObsExecRecord;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.PioParseException;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Contains the automatically updating part of the observing log.
 */
public final class ObsExecLog extends AbstractDataObject implements ISPMergeable<ObsExecLog> {
    private static final Logger LOG = Logger.getLogger(ObsExecLog.class.getName());

    private static final long serialVersionUID = 3L;

    public static final SPComponentType SP_TYPE = SPComponentType.OBS_EXEC_LOG;

    public static final ISPNodeInitializer<ISPObsExecLog, ObsExecLog> NI =
        new SimpleNodeInitializer<>(SP_TYPE, () -> new ObsExecLog());


    private ObsExecRecord _obsExecRecord;

    // Holds completed configuration steps (for now at least, just G-CAL related).
    // So this stuff is stored as a compressed string in ObsRecord, which
    // really renders it useless other than to display to the user.  For the
    // UX project, we had to have a way of always using the same calibration
    // items for executed steps for Smart G-CAL).  That history is stored here.
    private ConfigSequence completedSteps;

    public ObsExecLog() {
        super(SP_TYPE);
        _obsExecRecord = new ObsExecRecord();
    }

    public Object clone() {
        // don't really want to support this method ...
        ObsExecLog res = (ObsExecLog) super.clone();
        res._obsExecRecord = _obsExecRecord.copy();
        if (completedSteps != null) {
            res.completedSteps = new ConfigSequence(completedSteps);
        }
        return res;
    }

    /**
     * Gets this observation's {@link edu.gemini.spModel.obsrecord.ObsExecRecord}, which contains executed
     * datasets, execution events, and configuration information as well as
     * time accounting data.
     *
     * @return the {@link edu.gemini.spModel.obsrecord.ObsExecRecord} for this observation
     */
    public ObsExecRecord getRecord() {
        return _obsExecRecord;
    }

    public boolean isEmpty() { return _obsExecRecord.isEmpty(); }

    public ConfigSequence getCompletedSteps() {
        return (completedSteps == null) ? new ConfigSequence() : new ConfigSequence(completedSteps);
    }

    public void setCompletedSteps(ConfigSequence seq, DatasetLabel label) {
        final TreeSet<DatasetLabel> ts = new TreeSet<>();
        ts.add(label);
        setCompletedSteps(seq, ts);
    }

    private static final ItemKey DATALABEL_KEY = new ItemKey("observe:dataLabel");

    private static boolean labelMatches(Config c, DatasetLabel label) {
        final Object o = c.getItemValue(DATALABEL_KEY); // this is a String for some reason :/
        if ((o == null) || !o.toString().equals(label.toString())) {
            LOG.warning("Expected to find dataset label '" + label + "' but was instead: " + o);
            return false;
        } else {
            return true;
        }
    }

    public void setCompletedSteps(ConfigSequence seq, SortedSet<DatasetLabel> labels) {
        if (labels.size() > 0) {
            final SPObservationID oid = labels.first().getObservationId();
            for (DatasetLabel label : labels) {
                if (!label.getObservationId().equals(oid)) {
                    LOG.warning("Trying to add steps from different observations to the exec log: " + oid + ", and " + label.getObservationId());
                    return;
                }
            }

            if (completedSteps == null) completedSteps = new ConfigSequence();


            // Get start and end indices keeping in mind that we have to fill in
            // any missing configurations between the end of the existing
            // completed steps and the beginning of the labels we're adding now.
            final int first = labels.first().getIndex() - 1;
            final int start = (first > completedSteps.size()) ? completedSteps.size() : first;
            final int end   = labels.last().getIndex() - 1;

            // Sequences can be edited at any time and there may not be a step
            // that corresponds to one or more datasets.
            if (start<0 || end>=seq.size()) {
                LOG.warning("Datasets do not correspond to observation sequence: obsId=" + oid + ", labels=" + labels.toString());
                return;
            }

            for (int i=start; i<=end; ++i) {
                final Config full = seq.getStep(i);
                if (!labelMatches(full, new DatasetLabel(oid, i+1))) return;

                // Limit this to just the required information for g-cal.
                final Config c = CalConfigFactory.minimal(full);
                if (i < completedSteps.size()) {
                    completedSteps.setStep(i, c);
                } else {
                    completedSteps.addStep(c);
                }
            }
        }
    }


    /**
     * Delegates to {@link edu.gemini.spModel.obsrecord.ObsExecRecord#toParamSet(PioFactory)}.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);
        paramSet.addParamSet(getRecord().toParamSet(factory));

        if ((completedSteps != null) && (completedSteps.size() > 0)) {
            ParamSet cs = CalConfigPio.toParamset(completedSteps, factory);
            paramSet.addParamSet(cs);
        }
        return paramSet;
    }

    public synchronized void setParamSet(ParamSet paramSet) {
        final ParamSet ps0 = paramSet.getParamSet(ObsExecRecord.PARAM_SET);
        // look for the old "obsRecord" if we don't find the correct name.
        final ParamSet obsRecordParamSet = (ps0 != null) ? ps0 : paramSet.getParamSet("obsRecord");

        if (obsRecordParamSet == null) {
            _obsExecRecord = new ObsExecRecord();
        } else {
            try {
                _obsExecRecord = new ObsExecRecord(obsRecordParamSet);
            } catch (PioParseException ex) {
                LOG.log(Level.WARNING, "Problem parsing ObsRecord ParamSet", ex);
                _obsExecRecord = new ObsExecRecord(); // not clear what to do in this case
            }
        }

        ParamSet ps = (ParamSet) paramSet.getChild(CalConfigPio.SEQUENCE_NAME);
        if (ps != null) {
            completedSteps = CalConfigPio.toConfigSequence(ps);
        }
    }

    // Selects the obs exec log with the latest last event time, which must
    // also contain a super set of the datasets of the other (otherwise returns
    // null).
    @Override public ObsExecLog mergeOrNull(ObsExecLog that) {
        LOG.warning("Merging ObsExecLogs");

        final ObsExecLog sup;
        final ObsExecLog sub;
        if (_obsExecRecord.getLastEventTime() >= that._obsExecRecord.getLastEventTime()) {
            sup = this;
            sub = that;
        } else {
            sup = that;
            sub = this;
        }

        final Set<DatasetLabel> supSet = sup._obsExecRecord.getDatasetLabels();
        final Set<DatasetLabel> subSet = sub._obsExecRecord.getDatasetLabels();
        return supSet.containsAll(subSet) ? (ObsExecLog) sup.clone() : null;
    }

    private static final class SequenceData {
        final DatasetLabel label;
        final ConfigSequence seq;
        final Option<Config> config;
        final int index;

        SequenceData(ISPObservation obs, DatasetLabel label) {
            this.label  = label;
            this.seq    = computeSequence(obs);
            this.index  = label.getIndex() - 1;
            this.config = DatasetConfigService.configForStep(seq, index);
        }

        private static ConfigSequence computeSequence(ISPObservation obs) {
            return ConfigBridge.extractSequence(obs, null, ConfigValMapInstances.IDENTITY_MAP);
        }
    }

    /**
     * Updates the ObsLog components of the corresponding observation (creating
     * them if necessary) and adding event information (if provided).
     *
     * @param obsId reference to the observation whose obs log component should be updated
     * @param label dataset being recorded (if any)
     * @param evt obs exec event being recorded (if any)
     */
    public static void updateObsLog(final IDBDatabaseService db, final SPObservationID obsId, final Option<DatasetLabel> label, final Option<ObsExecEvent> evt) {

        ObsLog.update(db, obsId, (obs, log) -> {
            final Option<SequenceData> seqData = label.map(new MapOp<DatasetLabel, SequenceData>() {
                @Override public SequenceData apply(DatasetLabel datasetLabel) {
                    return new SequenceData(obs, datasetLabel);
                }
            });

            try {
                // Add the event to the obs record, if one was provided.
                evt.foreach(oee -> {
                    // Extract the config out of the sequence, if any.
                    final Option<Config> rawConfig = seqData.flatMap(new MapOp<SequenceData, Option<Config>>() {
                        @Override public Option<Config> apply(SequenceData sd) { return sd.config; }
                    });

                    // Add the event, but map the config to String/display values.
                    log.execLogDataObject.getRecord().addEvent(oee, rawConfig.map(new MapOp<Config, Config>() {
                        @Override
                        public Config apply(Config config) {
                            return ConfigValMapUtil.mapValues(config, ConfigValMapInstances.TO_DISPLAY_VALUE);
                        }
                    }).getOrNull());
                });


                // Write the completed step into the obslog component
                // (for smart gcal).
                seqData.foreach(sd -> log.execLogDataObject.setCompletedSteps(sd.seq, sd.label));

            } catch (Exception ex) {
                LOG.log(Level.WARNING, String.format("Could not update %s ObsLog for event: %s", obsId, evt.getOrNull()), ex);
                throw new RuntimeException(ex);
            }
        });
    }

}
