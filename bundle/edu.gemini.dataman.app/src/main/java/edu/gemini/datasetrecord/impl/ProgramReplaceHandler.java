package edu.gemini.datasetrecord.impl;

import edu.gemini.datasetrecord.impl.trigger.DsetRecordChange;
import edu.gemini.pot.sp.ISPObsQaLog;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.spdb.ProgramEvent;
import edu.gemini.pot.spdb.ProgramEventListener;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.obslog.ObsLog;

import java.util.*;

/**
 * A DsetRecordServiceImpl helper that deals with program replace events.  When
 * a QA state is edited and that change is stored in the database, a program
 * replace event is fired.  This class handles that event to find any updated
 * QA states and have the DsetRecordServiceImpl forward those along as
 * appropriate.
 */
final class ProgramReplaceHandler implements ProgramEventListener<ISPProgram> {

    // A "function" called when a QA state update is detected.
    interface QaStateUpdatedFunction {
        void updatedQaStates(Collection<DsetRecordChange> updates);
    }

    private static class UpdatedObs {
        final ISPObservation oldObs; // may be null if it is a newly created observation
        final ISPObservation newObs;

        UpdatedObs(ISPObservation oldObs, ISPObservation newObs) {
            this.oldObs = oldObs;
            this.newObs = newObs;
        }

        private Map<DatasetLabel, DatasetRecord> mapRecords(ISPObservation obs) {
            if (obs == null) return Collections.emptyMap();

            final ObsLog log = ObsLog.getIfExists(obs);
            if (log == null) return Collections.emptyMap();

            final Map<DatasetLabel, DatasetRecord> m = new TreeMap<DatasetLabel, DatasetRecord>();
            for (DatasetRecord r : log.getAllDatasetRecords()) {
                m.put(r.getLabel(), r);
            }
            return m;
        }

        List<DsetRecordChange> getQaStateChanges() {
            final ObsLog newLog = ObsLog.getIfExists(newObs);
            if (newLog == null) return Collections.emptyList();

            final List<DatasetRecord> newRecs = newLog.getAllDatasetRecords();
            if (newRecs.size() == 0) return Collections.emptyList();

            final List<DsetRecordChange> changes = new ArrayList<DsetRecordChange>();
            final Map<DatasetLabel, DatasetRecord> oldMap = mapRecords(oldObs);
            for (DatasetRecord newRec : newRecs) {
                final DatasetRecord oldRec = oldMap.get(newRec.getLabel());
                if ((oldRec == null) || (oldRec.qa != newRec.qa)) {
                    changes.add(new DsetRecordChange(oldRec, newRec));
                }
            }
            return changes;
        }
    }

    // Gets a list of only those observation pairs who have seen their QA log
    // updated (or are brand new observations with no past qa log).
    private static Collection<UpdatedObs> getUpdatedObservations(ISPProgram oldProg, ISPProgram newProg) {
        final Map<SPNodeKey, ISPObservation> oldObsMap = new HashMap<SPNodeKey, ISPObservation>();
        for (ISPObservation oldObs : oldProg.getAllObservations()) {
            oldObsMap.put(oldObs.getNodeKey(), oldObs);
        }

        final List<UpdatedObs> res = new ArrayList<UpdatedObs>();
        for (ISPObservation newObs : newProg.getAllObservations()) {
            final ISPObsQaLog newQaLog = newObs.getObsQaLog();
            if (newQaLog == null) continue;

            final SPNodeKey key = newQaLog.getNodeKey();
            if (!newProg.getVersions(key).equals(oldProg.getVersions(key))) {
                res.add(new UpdatedObs(oldObsMap.get(newObs.getNodeKey()), newObs));
            }
        }
        return res;
    }


    private final QaStateUpdatedFunction func;

    ProgramReplaceHandler(QaStateUpdatedFunction func) {
        this.func = func;
    }

    @Override public void programAdded(ProgramEvent<ISPProgram> pme) {
        // Ignore
    }
    @Override public void programRemoved(ProgramEvent<ISPProgram> pme) {
        // Ignore
    }

    @Override public void programReplaced(ProgramEvent<ISPProgram> pme) {
        final List<DsetRecordChange> changes = new ArrayList<DsetRecordChange>();
        for (UpdatedObs uo : getUpdatedObservations(pme.getOldProgram(), pme.getNewProgram())) {
            changes.addAll(uo.getQaStateChanges());
        }
        if (changes.size() > 0) func.updatedQaStates(changes);
    }
}
