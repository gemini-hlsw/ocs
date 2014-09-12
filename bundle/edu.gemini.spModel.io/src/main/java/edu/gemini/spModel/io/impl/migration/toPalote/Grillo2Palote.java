//
// $Id: Grillo2Palote.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.io.impl.migration.toPalote;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.GeminiRuntimeException;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.dataset.*;
import edu.gemini.spModel.event.*;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.ObsQaState;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obslog.ObsQaLog;
import edu.gemini.spModel.pio.Container;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.time.ObsTimeCorrection;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.obsrecord.ObsExecRecord;
import edu.gemini.spModel.config.DatasetConfigService;
import java.util.logging.Logger;
import java.util.logging.Level;


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


/**
 * Code to convert the Grillo model to the Palote model.
 */
public final class Grillo2Palote {
    private static final Logger LOG = Logger.getLogger(Grillo2Palote.class.getName());

    public static DatasetQaState getDatasetQaState(Container obsCont) {
        String obsStatusStr = Pio.getValue(obsCont, "Observation/status");
        if ("QA(fail)".equals(obsStatusStr)) {
            return DatasetQaState.FAIL;
        } else if ("QA(pass)".equals(obsStatusStr)) {
            return DatasetQaState.PASS;
        } else if ("QA(usable)".equals(obsStatusStr)) {
            return DatasetQaState.USABLE;
        } else {
            return DatasetQaState.UNDEFINED;
        }
    }

    public static ObsQaState getObsQaState(Container obsCont) {
        String obsStatusStr = Pio.getValue(obsCont, "Observation/status");
        if ("QA(fail)".equals(obsStatusStr)) {
            return ObsQaState.FAIL;
        } else if ("QA(pass)".equals(obsStatusStr)) {
            return ObsQaState.PASS;
        } else if ("QA(usable)".equals(obsStatusStr)) {
            return ObsQaState.USABLE;
        } else {
            return ObsQaState.UNDEFINED;
        }
    }


    private static final long END_VISIT_GAP = 1000 * 60 * 60;  // ONE HOUR
    private static boolean _visitEnded(long lastEventTime, long nextEventTime) {
        return ((nextEventTime - lastEventTime) > END_VISIT_GAP); // if an hour passed ...
    }

    private static long _getTotalUsedTime(Container obsCont) {
        double secs;
        secs = Pio.getDoubleValue(obsCont, "Observation/totalUsedTime", 0.0);
        return (long) (secs * 1000);
    }

    /**
     * Converts the given <code>obs</code> to be a Palote observation, with the
     * Palote version of the observing log.
     *
     * @param obs ISPObservation parsed from XML (where the Grillo specific
     * features like the DataStore obs component have been ignored)
     * @param obsCont Container that holds the XML from which the ISPObservation
     * was parsed
     * @param fact reference to the factory used to create the new ObsLog
     * component if needed
     */
    public static void toPalote(ISPObservation obs, Container obsCont, ISPFactory fact)
            throws SPException {
        SPObservationID obsId = obs.getObservationID();

        // First, look for the ObsLogDataObject.   If it exists, then there is
        // nothing to do.
        final ObsLog nodes = ObsLog.getIfExists(obs);
        if (nodes != null) return;

        // Figure out the DataflowStep and DatasetQaState to use, and convert
        // the existing dataset records.
        DatasetQaState qa = getDatasetQaState(obsCont);
        DatasetRecord[] records = DatasetConverter.getDatasetRecords(obsId, obsCont, qa);

        // Convert the old history list to new ObsExecEvents.
        List evts = EventConverter.getEventList(obsId, records, obsCont);

        // Now, if there are no datasets and there are no events, then do
        // nothing.
        if ((records.length == 0) && (evts.size() == 0)) return;

        // Create the ObsLog.
        final ISPObsQaLog qaLog = fact.createObsQaLog(obs.getProgram(), null);
        obs.setObsQaLog(qaLog);
        final ISPObsExecLog execLog = fact.createObsExecLog(obs.getProgram(), null);
        obs.setObsExecLog(execLog);

        // Get the the data objects we need: one for the obs itself, one for the
        // old DataStore, and one for the obs log component.
        final SPObservation obsDo = (SPObservation) obs.getDataObject();
        final ObsQaLog qaLogDo = (ObsQaLog) qaLog.getDataObject();
        final ObsExecLog execLogDo = (ObsExecLog) execLog.getDataObject();

        // Set the observation wide qa state.
        final ObsQaState obsQaState = getObsQaState(obsCont);
        obsDo.setOverriddenObsQaState(obsQaState);
        obsDo.setOverrideQaState(true);

        // Hash the records by label.
        final Map<DatasetLabel, DatasetRecord> recMap = new HashMap<DatasetLabel, DatasetRecord>();
        for (int i=0; i<records.length; ++i) {
            recMap.put(records[i].getLabel(), records[i]);
        }

        // Go through the old history list, adding corresponding new
        // ObsExecEvents.
        final ObsExecRecord obsRec = execLogDo.getRecord();
        long lastTime = 0;
        for (Iterator it=evts.iterator(); it.hasNext(); ) {
            ObsExecEvent evt = (ObsExecEvent) it.next();
            long time = evt.getTimestamp();

            // If we guess that a visit ended, inject a start visit event.
            if (_visitEnded(lastTime, time)) {
                obsRec.addEvent(new StartVisitEvent(time, obsId), null);
            }
            lastTime = time;

            // Add the event to the log.
            if (!(evt instanceof StartDatasetEvent)) {
                obsRec.addEvent(evt, null);
                continue;
            }

            // Since this is a start dataset event, we need to get the
            // corresponding Config and update the newly generated
            // DatasetRecord (to set its dataflow step and qa).
            StartDatasetEvent sde = (StartDatasetEvent) evt;
            DatasetLabel label = sde.getDataset().getLabel();
            Config conf = DatasetConfigService.deriveConfigForDataset(label, obs, ConfigValMapInstances.TO_DISPLAY_VALUE).getOrNull();
            obsRec.addEvent(evt, conf);

            // Update the dataset record.
            DatasetRecord convertedRecord = recMap.get(label);
            if (convertedRecord == null) {
                // programming error -- the start dataset event wouldn't have
                // been created if the dataset record didn't exist
                String msg = "Could not find converted record for: " + label;
                LOG.log(Level.WARNING, msg);
                throw new GeminiRuntimeException(msg);
            }

            qaLogDo.setQaState(label, convertedRecord.qa.qaState);
            qaLogDo.setComment(label, convertedRecord.qa.comment);

            // add an end dataset event so that this dataset is marked complete
            obsRec.addEvent(new EndDatasetEvent(evt.getTimestamp(), label), null);
        }

        execLog.setDataObject(execLogDo);
        qaLog.setDataObject(qaLogDo);

        // Now, create a obs time adjustment record to make the total used
        // time match what we calculate minus corrections.
        long targetTime     = _getTotalUsedTime(obsCont);
        long calculatedTime = execLogDo.getRecord().getTotalTime();
        long correction     = targetTime - calculatedTime;

        TimeValue tv;
        tv = TimeValue.millisecondsToTimeValue(correction, TimeValue.Units.minutes);
        long now = System.currentTimeMillis();

        ObsClass obsClass = ObsClassService.lookupObsClass(obs);
        ChargeClass chargeClass = (obsClass != null? obsClass.getDefaultChargeClass()
                : ChargeClass.DEFAULT);

        obsDo.addObsTimeCorrection(new ObsTimeCorrection(tv, now, chargeClass,
                "Initial import correction"));

        obs.setDataObject(obsDo);
    }
}
