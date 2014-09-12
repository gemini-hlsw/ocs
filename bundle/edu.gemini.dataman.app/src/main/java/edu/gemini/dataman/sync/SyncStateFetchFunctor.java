//
// $Id: SyncStateFetchFunctor.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.dataman.sync;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.*;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.dataset.DatasetExecRecord;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obslog.ObsQaLog;
import edu.gemini.spModel.obsrecord.ObsExecRecord;


import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
final class SyncStateFetchFunctor extends DBAbstractFunctor implements IDBParallelFunctor {
    private static final Logger LOG = Logger.getLogger(SyncStateFetchFunctor.class.getName());

    private Collection<SPProgramID> _progIds;

    private Collection<DatasetRecordSyncState> _syncStates;

    public SyncStateFetchFunctor(Collection<SPProgramID> progIds) {
        if (progIds == null) throw new NullPointerException();
        _progIds = new ArrayList<SPProgramID>(progIds);
    }

    @SuppressWarnings("unchecked")
    public synchronized Collection<DatasetRecordSyncState> getSyncStates() {
        if (_syncStates == null) return Collections.EMPTY_LIST;
        return Collections.unmodifiableCollection(_syncStates);
    }

    private synchronized Collection<DatasetRecordSyncState> _getSyncStates() {
        if (_syncStates == null) {
            _syncStates = new ArrayList<DatasetRecordSyncState>(128);
        }
        return _syncStates;
    }

    public void execute(IDBDatabaseService db, ISPNode ignore, Set<Principal> principals) {
        for (Iterator it=_progIds.iterator(); it.hasNext(); ) {
            SPProgramID id = (SPProgramID) it.next();
            ISPProgram prog = db.lookupProgramByID(id);
            if (prog == null) {
                // program deleted since the ids were fetched!
                continue;
            }
            _addSyncStates(prog);
        }
        _progIds = null; // no need to serialize these and send them back
    }

    private void _addSyncStates(ISPProgram prog) {
        List obsList = prog.getAllObservations();
        if ((obsList == null) || (obsList.size() == 0)) return;

        for (Iterator it=obsList.iterator(); it.hasNext(); ) {
            _addSyncStates((ISPObservation) it.next());
        }
    }

    private void _addSyncStates(ISPObservation obs) {
        final ISPObsQaLog ql = obs.getObsQaLog();
        if (ql == null) return;
        final ObsQaLog qaLog = (ObsQaLog) ql.getDataObject();

        final ISPObsExecLog el = obs.getObsExecLog();
        if (el == null) return;
        final ObsExecLog execLog = (ObsExecLog) el.getDataObject();

        final ObsExecRecord or = execLog.getRecord();
        final List<DatasetExecRecord> recs = or.getAllDatasetExecRecords();
        if (recs.size() == 0) return;

        final Collection<DatasetRecordSyncState> ss = _getSyncStates();
        for (DatasetExecRecord der : recs) {
            final DatasetLabel lab  = der.getLabel();
            final DatasetRecord rec = new DatasetRecord(qaLog.get(lab), der);
            ss.add(new DatasetRecordSyncState(rec));
        }
    }

    public static Collection<DatasetRecordSyncState> getSyncStates(
                                              IDBDatabaseService db,
                                              Collection<SPProgramID> progIds,
                                              Set<Principal> user) {
        SyncStateFetchFunctor func = new SyncStateFetchFunctor(progIds);
        try {
            func = db.getQueryRunner(user).execute(func, null);
        } catch (SPNodeNotLocalException ex) {
            LOG.log(Level.SEVERE, "Got an impossible node not local exception");
            throw new RuntimeException(ex);
        }
        return func.getSyncStates();
    }

    public void mergeResults(Collection<IDBFunctor> collection) {
        Collection<DatasetRecordSyncState> syncStates;
        syncStates = new ArrayList<DatasetRecordSyncState>();

        for (IDBFunctor fun : collection) {
            SyncStateFetchFunctor ssff = (SyncStateFetchFunctor) fun;
            if (ssff._syncStates != null) {
                syncStates.addAll(ssff._syncStates);
            }
        }

        _syncStates = syncStates;
    }
}
