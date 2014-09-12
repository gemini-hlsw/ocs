//
// $Id: ObsRecordUpdateTask.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.impl.store;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetRecord;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 */
final class ObsRecordUpdateTask implements Callable<Map<DatasetLabel, DatasetRecord>> {
    private static final Logger LOG = Logger.getLogger(ObsRecordUpdateTask.class.getName());

    enum Status {
        pending, active, done
    }

    private final SPObservationID _obsId;
    private final Collection<IDBDatabaseService> _dbs;

    private final Collection<DsetRecordUpdateRequest> _requests = new ArrayList<DsetRecordUpdateRequest>();
    private final Lock _lock = new ReentrantLock();
    private final Condition _completion = _lock.newCondition();
    private Status _status = Status.pending;

    private final long _creationTime = System.currentTimeMillis();
    private long _lastMod = _creationTime;

    private Map<DatasetLabel, DatasetRecord> _results;
    private Exception _problem;
    private final Set<Principal> _user;

    ObsRecordUpdateTask(SPObservationID obsId, Collection<IDBDatabaseService> dbs, Set<Principal> user) {
        _obsId = obsId;
        _dbs   = dbs;
        _user  = user;
    }

    public SPObservationID getObservationId() {
        return _obsId;
    }

    synchronized boolean addPendingRequest(DsetRecordUpdateRequest update) {
        if (_status != Status.pending) return false;
        _lastMod = System.currentTimeMillis();
        _requests.add(update);
        return true;
    }

    synchronized Collection<DatasetLabel> getDatasetLabels() {
        List<DatasetLabel> res = new ArrayList<DatasetLabel>();

        for (DsetRecordUpdateRequest req : _requests) {
            res.add(req.getLabel());
        }
        Collections.sort(res);

        return res;
    }

    synchronized Status getStatus() {
        return _status;
    }

    synchronized long getCreationTime() {
        return _creationTime;
    }

    synchronized long getLastModificationTime() {
        return _lastMod;
    }

    private synchronized void setStatus(Status status) {
        _lastMod = System.currentTimeMillis();
        _status = status;
    }

    DatasetRecord get(DatasetLabel label) throws InterruptedException {
        _lock.lockInterruptibly();
        try {
            while (getStatus() != Status.done) {
                _completion.await();
            }
        } finally {
            _lock.unlock();
        }

        synchronized (this) {
            if (_problem != null) {
                if (!(_problem instanceof RuntimeException)) {
                    _problem = new RuntimeException(_problem);
                }
                throw (RuntimeException) _problem;
            }
            if (_results == null) return null;
            return _results.get(label);
        }
    }

    public Map<DatasetLabel, DatasetRecord> call() throws Exception {
        LOG.fine("** Updating observation " + _obsId);
        setStatus(Status.active);

        // do it
        ObsRecordUpdateFunctor f = new ObsRecordUpdateFunctor(_obsId, _requests);
        for (IDBDatabaseService db : _dbs) {
            try {
                LOG.fine("Sending ObsRecordUpdateFunctor " + _obsId + " to db: " + db);
                long t1 = System.currentTimeMillis();
                f = db.getQueryRunner(_user).execute(f, null);
                long t2 = System.currentTimeMillis();
                LOG.fine("Executed ObsRecordUpdateFunctor " + _obsId + " in " + (t2-t1) + " ms");

                _results = f.getResults();
                break;
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Problem executing ObsRecordUpdateFunctor", ex);
                _problem = ex;
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "Throwable executing ObsRecordUpdateFunctor", t);
                _problem = new RuntimeException(t);
            }
        }

        // Notify completion.
        setStatus(Status.done);
        _lock.lockInterruptibly();
        try {
            LOG.fine("Signalling all to continue " + _obsId);
            _completion.signalAll();
        } finally {
            _lock.unlock();
        }

        LOG.fine("Returning results " + _obsId);

        if ((_results == null) && (_problem != null)) throw _problem;
        return _results;
    }
}
