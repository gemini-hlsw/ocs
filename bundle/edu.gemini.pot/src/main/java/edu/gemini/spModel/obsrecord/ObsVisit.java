package edu.gemini.spModel.obsrecord;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.event.ObsExecEvent;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.time.ObsTimes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * An ObsVisit describes the activities related to a single session of
 * observing activity related to a single observation.  It is a single unit of
 * time accounting associated with the observation.  An observation may have
 * multiple visits over the course of its life, if observed on multiple nights
 * or multiple times on the same night.
 */
public final class ObsVisit implements Serializable {
    public static final ObsVisit[] EMPTY_ARRAY = new ObsVisit[0];

    /**
     * A comparator that may be used to sort {@link ObsVisit}s based upon
     * start time.
     */
    public static final Comparator<ObsVisit> START_TIME_COMPARATOR = new Comparator<ObsVisit>() {
        public int compare(ObsVisit o1, ObsVisit o2) {
            long t1 = o1.getStartTime();
            long t2 = o2.getStartTime();
            return (t1 == t2) ? 0 : (t1 < t2 ? -1 : 1);
        }
    };

    // The events that occurred during the visit.  This should always start
    // with a StartVisitEvent and typically end with one too.
    private ObsExecEvent[] _events;

    // The unique configurations that produced datasets over the course of the
    // visit, if any.
    private UniqueConfig[] _configs;

    // Time taken by the observation broken into classes
    private VisitTimes _times;

    ObsVisit(ObsExecEvent[] events, UniqueConfig[] configs, VisitTimes times) {
        if (events == null) throw new NullPointerException("events");
        if (events.length < 1) {
            throw new IllegalArgumentException("must be at least one event");
        }

        // just assign a ref since the ObsVisit should only be created by the
        // ObsRecord class, which doesn't hold on to the events after
        // creating the ObsVisit.
        _events = events;

        if (configs == null) configs = UniqueConfig.EMPTY_ARRAY;
        _configs = configs;

        // again, doesn't hold on to the reference after creating the ObsVisit
        _times = times;
    }

    /**
     * Gets the observation id associated with this ObsVisit.
     */
    public SPObservationID getObsId() {
        return _events[0].getObsId();
    }

    /**
     * Gets (a copy of) the list of events that occured during this visit.
     */
    public ObsExecEvent[] getEvents() {
        ObsExecEvent[] res = new ObsExecEvent[_events.length];
        System.arraycopy(_events, 0, res, 0, _events.length);
        return res;
    }

    /**
     * Gets (a copy of) the unique configurations during this visit that
     * produced datasets, if any.  Will return an empty array if there were no
     * datasets produced during the visit.
     */
    public UniqueConfig[] getUniqueConfigs() {
        if (_configs.length == 0) return UniqueConfig.EMPTY_ARRAY;

        UniqueConfig[] res = new UniqueConfig[_configs.length];
        System.arraycopy(_configs, 0, res, 0, _configs.length);
        return res;
    }

    /**
     * Gets the start time of the visit.
     */
    public long getStartTime() {
        return _events[0].getTimestamp();
    }

    /**
     * Gets the end time of the visit.
     */
    public long getEndTime() {
        return _events[_events.length - 1].getTimestamp();
    }

    /**
     * Gets the total time of the visit.
     */
    public long getTotalTime() {
        return getEndTime() - getStartTime();
    }

    public ObsTimeCharges getTimeCharges(ChargeClass mainChargeClass) {
        return _times.getTimeCharges(mainChargeClass);
    }

    public ObsTimes getObsTimes(ChargeClass mainChargeClass) {
        return new ObsTimes(getTotalTime(), getTimeCharges(mainChargeClass));
    }


    /**
     * Gets all the dataset labels that are associated with this visit.
     */
    public DatasetLabel[] getAllDatasetLabels() {
        List<DatasetLabel> labels = new ArrayList<DatasetLabel>();

        for (UniqueConfig config : _configs) {
            DatasetLabel[] configLables = config.getDatasetLabels();
            for (DatasetLabel configLable : configLables) {
                labels.add(configLable);
            }
        }

        return labels.toArray(DatasetLabel.EMPTY_ARRAY);
    }
}
