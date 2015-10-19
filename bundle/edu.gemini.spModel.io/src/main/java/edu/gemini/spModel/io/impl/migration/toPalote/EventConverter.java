//
// $Id: EventConverter.java 6228 2005-05-30 16:08:39Z shane $
//

package edu.gemini.spModel.io.impl.migration.toPalote;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.event.*;
import edu.gemini.spModel.pio.Container;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioPath;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

/**
 * Converts the Grillo PIO representation of an event (a history list item) to
 * a Palote {@link edu.gemini.spModel.event.ObsExecEvent}.
 */
class EventConverter {
    private static Logger LOG = Logger.getLogger(EventConverter.class.getName());

    private SPObservationID _obsId;
    private DatasetRecord[] _records;
    private int _recIndex;

    EventConverter(SPObservationID obsId, DatasetRecord[] records) {
        _obsId = obsId;
        _records = records;
    }

    private void _logMissingInfo(ParamSet grilloDset, String paramName) {
        LOG.log(Level.WARNING, "Grillo dataset '" + grilloDset.getSequence() +
                "' missing param '" + paramName + "' for obs '" + _obsId + "'");
    }

    long getTimeStamp(ParamSet grilloEvent) {
        long timestamp = Pio.getLongValue(grilloEvent, "time", -1);
        if (timestamp == -1) _logMissingInfo(grilloEvent, "time");
        return timestamp;
    }

    String getMessage(ParamSet grilloEvent) {
        return Pio.getValue(grilloEvent, "message", "");
    }

    private static boolean _timesMatch(long datasetTime, long eventTime) {
        long startRange = datasetTime - 10000; // 10 secs
        long endRange   = datasetTime + 10000; // 10 secs
        return (eventTime >= startRange) && (eventTime <= endRange);
    }

    private DatasetRecord _findRecordForEventTime(long eventTime) {
        while (_recIndex < _records.length) {
            DatasetRecord rec = _records[_recIndex];

            long datasetTime = rec.exec.dataset().getTimestamp();

            if (_timesMatch(datasetTime, eventTime)) {
                ++_recIndex;
                return rec;
            }

            System.out.println("_recIndex    = " + _recIndex);
            System.out.println("dataset time = " + datasetTime);
            System.out.println("event time   = " + eventTime);

            if (datasetTime > eventTime) {
                // stop looking, next dataset should belong to a future event
                return null;
            }

            ++_recIndex;
        }

        return null;
    }

    ObsExecEvent toObsExecEvent(ParamSet grilloEvent) {
        String name = grilloEvent.getName();
        if (name == null) {
            _logMissingInfo(grilloEvent, "name");
            return null;
        }

        long time = getTimeStamp(grilloEvent);

        if ("Start Observation".equals(name)) {
            return new SlewEvent(time,_obsId);

        } else if ("Start Sequence".equals(name)) {
            return new StartSequenceEvent(time, _obsId);

        } else if ("Abort Observation".equals(name)) {
            String msg = getMessage(grilloEvent);
            return new AbortObserveEvent(time, _obsId, msg);

        } else if ("Pause Sequence".equals(name)) {
            String msg = getMessage(grilloEvent);
            return new PauseObserveEvent(time, _obsId, msg);

        } else if ("End Sequence".equals(name)) {
            return new EndSequenceEvent(time, _obsId);

        } else if ("Dataset Complete".equals(name)) {
            DatasetRecord rec = _findRecordForEventTime(time);
            if (rec == null) {
                LOG.log(Level.WARNING, "In obs '" + _obsId +
                        "' could not find dataset record matching 'Dataset Complete' event at time: " +
                        time);
                return null;
            }
            return new StartDatasetEvent(time, rec.exec.dataset());
        }

        LOG.log(Level.WARNING, "Unknown event type, skipping: '" + name + "'");
        return null;
    }


    static List<ObsExecEvent> getEventList(SPObservationID obsId, DatasetRecord[] records, Container obsCont) {
        final ParamSet history = obsCont.lookupParamSet(new PioPath("Observation/history"));
        if (history == null) return Collections.emptyList();

        final List<ParamSet> historyItems = history.getParamSets();
        if (historyItems == null) return Collections.emptyList();

        final EventConverter ec = new EventConverter(obsId, records);
        final List<ObsExecEvent> res = new ArrayList<>();
        for (ParamSet grilloEvent : historyItems) {
            final ObsExecEvent evt = ec.toObsExecEvent(grilloEvent);
            if (evt != null) res.add(evt);
        }
        return res;
    }
}
