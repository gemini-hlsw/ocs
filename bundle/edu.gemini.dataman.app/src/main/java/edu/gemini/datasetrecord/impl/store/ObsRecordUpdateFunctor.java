//
// $Id: ObsRecordUpdateFunctor.java 694 2006-12-11 19:44:27Z shane $
//

package edu.gemini.datasetrecord.impl.store;

import edu.gemini.datasetrecord.DatasetRecordTemplate;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBFunctor;
import edu.gemini.pot.spdb.IDBParallelFunctor;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.dataset.*;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obsrecord.ObsExecRecord;

import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Functor used to update an ObsRecord's DatasetRecords.
 */
final class ObsRecordUpdateFunctor extends DBAbstractFunctor implements IDBParallelFunctor {
    private static final Logger LOG = Logger.getLogger(ObsRecordUpdateFunctor.class.getName());

    private SPObservationID _obsId;
    private Collection<DsetRecordUpdateRequest> _requests;
    private Map<DatasetLabel, DatasetRecord> _results;
    private boolean _modified;

    public ObsRecordUpdateFunctor(SPObservationID obsId, Collection<DsetRecordUpdateRequest> requests) {
        if (obsId == null) throw new NullPointerException("obsId == null");
        if (requests == null) throw new NullPointerException("requests == null");
        _obsId    = obsId;
        _requests = requests;
    }

    public boolean isModified() {
        return _modified;
    }

    public Map<DatasetLabel, DatasetRecord> getResults() {
        return _results;
    }

    public void execute(IDBDatabaseService db, ISPNode ispNode, Set<Principal> principals) {
        try {
            _updateDataObj(db);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Unexpected exception updating dataset: " + _obsId, t);

        } finally {
            // no need to serialize and send these back to the client
            _requests = null;
        }
    }

    private void _updateDataObj(final IDBDatabaseService db) {

        ObsLog.update(db, _obsId, new ObsLog.UpdateOp() {
            @Override public void apply(ISPObservation obs, ObsLog log) {

                final SortedSet<DatasetLabel> created = new TreeSet<DatasetLabel>();
                final Map<DatasetLabel, DatasetRecord> results = new TreeMap<DatasetLabel, DatasetRecord>();

                final ObsExecRecord obsRec = log.execLogDataObject.getRecord();
                final DsetRecordFactory factory = new DsetRecordFactory(obs, obsRec);
                for (DsetRecordUpdateRequest request : _requests) {
                    final DatasetLabel label = request.getLabel();
                    DatasetExecRecord execRec = obsRec.getDatasetExecRecord(label);

                    if (execRec == null) {
                        execRec = factory.createRecord(request);
                        if (execRec == null) continue;  // can fail to create record
                        _modified = true;
                        created.add(label);
                    }

                    // Update the dataset record.
                    final DatasetRecord initial = new DatasetRecord(log.qaLogDataObject.get(label), execRec);
                    final DatasetRecord updated = _updateRecord(request.getRequest(), request.getPrecond(), initial);
                    if (!updated.equals(initial)) {
                        _modified = true;
                        results.put(label, updated);

                        if (!updated.qa.equals(initial.qa)) {
                            log.qaLogDataObject.set(updated.qa);
                        }
                        if (!updated.exec.equals(initial.exec)) {
                            obsRec.putDatasetExecRecord(updated.exec, null);
                        }
                    }
                }

                if (!_modified) return;

                // Add completed steps for all datasets whose record had to be added.
                if (created.size() > 0) {
                    final ConfigSequence seq = factory.getConfigSequence();
                    log.execLogDataObject.setCompletedSteps(seq, created);
                }

                _results = results;
                _logUpdate(_obsId, created, results);
            }
        });
    }

    private static void _logUpdate(SPObservationID obsId,
                                   Set<DatasetLabel> created,
                                   Map<DatasetLabel, DatasetRecord> updated) {
        if (!LOG.isLoggable(Level.FINE)) return;

        StringBuilder buf = new StringBuilder();
        buf.append("\nUpdated observing record for obs ");
        buf.append(obsId);
        buf.append("\n");
        if (created.size() > 0) {
            buf.append("\tCreated: ");
            buf.append(created);
            buf.append('\n');
        }

        Set<DatasetLabel> upd = new HashSet<DatasetLabel>(updated.keySet());
        upd.removeAll(created);
        buf.append("\tUpdated: ");
        buf.append(upd);

        LOG.fine(buf.toString());
    }

    private static DatasetRecord _updateRecord(DatasetRecordTemplate update,
                                               DatasetRecordTemplate precond,
                                               DatasetRecord rec) {

        // Check the preconditions.
        if (precond != null) {
            // Check the sync time.
            final Long preSyncTime = precond.getSyncTime();
            if ((preSyncTime != null) && !preSyncTime.equals(rec.exec.syncTime)) {
                return rec;
            }

            // Check the Qa State.
            final DatasetQaState qaState = precond.getQaState();
            if ((qaState != null) && !qaState.equals(rec.qa.qaState)) {
                return rec;
            }

            // Check the comment
            final String comment = precond.getComment();
            if ((comment != null) && !comment.equals(rec.qa.comment)) {
                return rec;
            }

            // Check the dataset state.
            final DatasetFileState fileState = precond.getDatasetFileState();
            if ((fileState != null) && !fileState.equals(rec.exec.fileState)) {
                return rec;
            }

            // Check the gsa state.
            final GsaState gsaState = precond.getGsaState();
            if ((gsaState != null) && !gsaState.equals(rec.exec.gsaState)) {
                return rec;
            }
        }

        final DatasetQaState updQaState     = update.getQaState();
        final DatasetQaState recQaState     = rec.qa.qaState;
        final DatasetFileState updFileState = update.getDatasetFileState();
        final DatasetFileState recFileState = rec.exec.fileState;

        // First, make sure this isn't an old out of order update.
        final DatasetExecRecord e0;
        final Long updateSyncTime = update.getSyncTime();
        final long recordSyncTime = rec.exec.syncTime;
        if ((updateSyncTime != null) && !updateSyncTime.equals(recordSyncTime)) {
            if (recordSyncTime > updateSyncTime) return rec; // old update
            e0 = rec.exec.withSyncTime(updateSyncTime);
        } else {
            e0 = rec.exec;
        }

        // Update Qa State.
        DatasetQaRecord q0 = rec.qa;
        if ((updQaState != null) && !updQaState.equals(recQaState)) {
            final boolean isNewUndefinedDataset =
                    (recFileState != DatasetFileState.OK) &&
                    (updFileState == DatasetFileState.OK) &&
                    (updQaState   == DatasetQaState.UNDEFINED);
            if (!isNewUndefinedDataset) {
                q0 = rec.qa.withQaState(updQaState);
            }
        }

        // Update the comment.
        final String comment = update.getComment();
        final DatasetQaRecord q1 = (comment == null) ? q0 : q0.withComment(comment);

        // Update the dataset file state.  Don't transition from TENTATIVE
        // to MISSING.  TENTATIVE implies MISSING.  It's like "EXPECTING".
        DatasetExecRecord e1 = e0;
        if ((updFileState != null) && !updFileState.equals(recFileState)) {
            if (!(DatasetFileState.MISSING.equals(updFileState) &&
                  DatasetFileState.TENTATIVE.equals(recFileState))) {
                e1 = e0.withFileState(updFileState);
            }
        }

        // Update the gsa state.
        final GsaState gsaState = update.getGsaState();
        final DatasetExecRecord e2 = (gsaState == null) ? e1 : e1.withGsaState(gsaState);
        return new DatasetRecord(q1, e2);
    }

    public void mergeResults(Collection<IDBFunctor> collection) {
        Map<DatasetLabel, DatasetRecord> results = null;
        boolean modified = false;
        for (IDBFunctor f : collection) {
            final ObsRecordUpdateFunctor up = (ObsRecordUpdateFunctor) f;
            if (up.isModified()) {
                results = up._results;
                break;
            }
        }
        _results  = results;
        _modified = modified;
    }
}
