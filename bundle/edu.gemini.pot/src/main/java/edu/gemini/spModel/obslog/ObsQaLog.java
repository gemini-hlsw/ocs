package edu.gemini.spModel.obslog;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsQaLog;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.dataset.*;
import edu.gemini.spModel.gemini.init.SimpleNodeInitializer;
import edu.gemini.spModel.obsrecord.ObsQaRecord;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

import java.util.*;

/**
 * Holds the user-settable information in the observing log.  This wraps an
 * immutable ObsQaRecord giving it mutability, standard data object trappings,
 * and listener support.
 */
public final class ObsQaLog extends AbstractDataObject {

    public static final SPComponentType SP_TYPE = SPComponentType.OBS_QA_LOG;

    public static final ISPNodeInitializer<ISPObsQaLog, ObsQaLog> NI =
        new SimpleNodeInitializer<>(SP_TYPE, () -> new ObsQaLog());

    public static final class Event extends EventObject {
        public final DatasetQaRecord oldRec;
        public final DatasetQaRecord newRec;

        private Event(ObsQaLog source, DatasetQaRecord oldRec, DatasetQaRecord newRec) {
            super(source);
            this.oldRec = oldRec;
            this.newRec = newRec;
        }

        public boolean isQaStateUpdated() {
            return oldRec.qaState != newRec.qaState;
        }

        public boolean isCommentUpdated() {
            return !oldRec.comment.equals(newRec.comment);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Event that = (Event) o;
            if (!newRec.equals(that.newRec)) return false;
            return oldRec.equals(that.oldRec);

        }

        @Override
        public int hashCode() {
            int result = oldRec.hashCode();
            result = 31 * result + newRec.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Event{" + "oldRec=" + oldRec + ", newRec=" + newRec + '}';
        }
    }

    public interface Listener extends EventListener {
        void datasetQaUpdate(Event event);
    }

    private ObsQaRecord obsQaRecord;
    private transient List<Listener> listeners;

    public ObsQaLog() {
        super(SP_TYPE);
        obsQaRecord = new ObsQaRecord();
    }

    public ObsQaLog(ObsQaRecord obsQaRecord) {
        super(SP_TYPE);
        this.obsQaRecord = obsQaRecord;
    }

    @Override
    public Object clone() {
        final ObsQaLog that = (ObsQaLog) super.clone();
        that.listeners = null;
        return that;
    }

    private synchronized List<Listener> getListeners() {
        if (listeners == null) listeners = new ArrayList<>();
        return listeners;
    }

    private synchronized List<Listener> getListenersCopy() {
        return new ArrayList<>(getListeners());
    }

    public synchronized void addDatasetQaRecordListener(Listener l) {
        getListeners().add(l);
    }

    public synchronized void removeDatasetQaRecordListener(Listener l) {
        getListeners().remove(l);
    }

    public synchronized ObsQaRecord getRecord() {
        return obsQaRecord;
    }

    public synchronized DatasetQaRecord get(DatasetLabel label) {
        return obsQaRecord.apply(label);
    }

    public void set(DatasetQaRecord qa) {
        final Event e;
        synchronized (this) {
            final DatasetQaRecord oldRec = get(qa.label);
            if (oldRec.equals(qa)) {
                e = null;
            } else {
                obsQaRecord = obsQaRecord.updated(qa);
                e = new Event(this, oldRec, qa);
            }
        }
        if (e != null)
            for (Listener l : getListenersCopy()) l.datasetQaUpdate(e);
    }

    public String getComment(DatasetLabel label) {
        return get(label).comment;
    }

    public void setComment(DatasetLabel label, String comment) {
        set(get(label).withComment(comment));
    }

    public DatasetQaState getQaState(DatasetLabel label) {
        return get(label).qaState;
    }

    public void setQaState(DatasetLabel label, DatasetQaState qa) {
        set(get(label).withQaState(qa));
    }

    @Override
    public synchronized ParamSet getParamSet(PioFactory factory) {
        final ParamSet paramSet = super.getParamSet(factory);
        paramSet.addParamSet(obsQaRecord.paramSet(factory));
        return paramSet;
    }

    @Override
    public synchronized void setParamSet(ParamSet paramSet) {
        final ParamSet ps = paramSet.getParamSet(ObsQaRecord.PARAM_SET());
        if (ps == null) {
            obsQaRecord = new ObsQaRecord();
        } else {
            obsQaRecord = ObsQaRecord.fromParamSet(ps);
        }
    }

    public DatasetQaStateSums sumDatasetQaStates(ObsExecLog exec) {
        return sumDatasetQaStates(exec.getRecord().getDatasetLabels());
    }

    public synchronized DatasetQaStateSums sumDatasetQaStates(Collection<DatasetLabel> labels) {
        final DatasetQaState[] qaStates = DatasetQaState.values();
        final int sz = qaStates.length;
        final int[] counts = new int[sz];

        for (DatasetLabel label : labels) {
            counts[getQaState(label).ordinal()] += 1;
        }

        final DatasetQaStateSum[] sums = new DatasetQaStateSum[sz];
        for (int i = 0; i < sz; ++i) {
            final DatasetQaState state = qaStates[i];
            sums[i] = new DatasetQaStateSum(state, counts[i]);
        }

        return new DatasetQaStateSums(sums);
    }
}
