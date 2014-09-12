package edu.gemini.datasetrecord.impl;


import edu.gemini.datasetrecord.DatasetRecordEvent;
import edu.gemini.datasetrecord.DatasetRecordListener;
import edu.gemini.datasetrecord.DatasetRecordService;
import edu.gemini.datasetrecord.DatasetRecordTemplate;
import edu.gemini.datasetrecord.impl.store.ObsRecordUpdater;
import edu.gemini.datasetrecord.impl.trigger.DsetRecordChange;
import edu.gemini.datasetrecord.impl.trigger.ObsRecordTriggerClient;
import edu.gemini.datasetrecord.impl.trigger.ObsRecordTriggerHandler;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPNodeNotLocalException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.dataflow.GsaAspect;
import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.dataset.DatasetExecRecord;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.gemini.obscomp.SPProgram;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client side implementation object that receives events when a
 * {@link edu.gemini.datasetrecord.impl.trigger.ObsRecordTriggerAction} is executed.  Handles propagating events to
 * local {@link edu.gemini.datasetrecord.DatasetRecordListener}s.
 */
public final class DsetRecordServiceImpl implements DatasetRecordService, ObsRecordTriggerHandler {
    private static final Logger LOG = Logger.getLogger(DsetRecordServiceImpl.class.getName());

    // Thread involved in sending the event to local listeners.  Done in a
    // separate thread in order to allow the {@link ObsRecordTriggerAction} to
    // terminate as rapidly as possible.  That's important because the
    // DsetTriggerAction is running remotely in the ODB.
    private class NotificationWorker implements Runnable {
        private BlockingQueue<Collection<DsetRecordChange>> _updateQueue =
                     new LinkedBlockingQueue<Collection<DsetRecordChange>>();
        private volatile boolean _stop;

        void stop() {
            _stop = true;
        }

        void addUpdate(Collection<DsetRecordChange> update) {
            try {
                _updateQueue.put(update);
            } catch (InterruptedException ex) {
                LOG.log(Level.FINE, "Interrupted while doing a put", ex);
            }
        }

        public void run() {
            while (!_stop) {
                try {
                    Collection<DsetRecordChange> update;
                    update = _updateQueue.poll(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                    _fireLocalEvents(update);
                } catch (InterruptedException ex) {
                    LOG.log(Level.FINE, "Interrupted while waiting for update", ex);
                }
            }
        }
    }

    private final Collection<DatasetRecordListener> _listeners;
    private ObsRecordTriggerClient _triggerClient;
    private ObsRecordUpdater _recordUpdater;
    private final Collection<IDBDatabaseService> _dbs;
    private final ProgramReplaceHandler _progReplaceHandler;
    private final Set<Principal> _user;

    private NotificationWorker _worker;
    private Thread _workerThread;

    public DsetRecordServiceImpl(Set<Principal> user) {
        _listeners = new HashSet<DatasetRecordListener>(3);
        _triggerClient = new ObsRecordTriggerClient(this);
        _recordUpdater = new ObsRecordUpdater(user);
        _dbs = new HashSet<IDBDatabaseService>();
        _progReplaceHandler = new ProgramReplaceHandler(new ProgramReplaceHandler.QaStateUpdatedFunction() {
            @Override public void updatedQaStates(Collection<DsetRecordChange> updates) {
                obsRecTrigger(updates);
            }
        });
        _user = user;
    }

    private Collection<IDBDatabaseService> _getDatabases() {
        Collection<IDBDatabaseService> res;
        synchronized (_dbs) {
            res = new ArrayList<IDBDatabaseService>(_dbs);
        }
        return res;
    }

    public DatasetExecRecord fetch(DatasetLabel label) {
        Collection<IDBDatabaseService> dbs = _getDatabases();
        if (dbs.size() == 0) return null;

        DsetRecordFetchFunctor f = new DsetRecordFetchFunctor(label);
        for (IDBDatabaseService db : dbs) {
            try {
                f = db.getQueryRunner(_user).execute(f, null);

                // If we find a database that we can communicate with,
                // we assume it is the right database.  So any exceptions that
                // might have happened before are forgotten.
                return f.getRecord();
            } catch (SPNodeNotLocalException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return null;
    }

    public GsaAspect fetchGsaAspect(SPProgramID progId) throws InterruptedException {
        Collection<IDBDatabaseService> dbs = _getDatabases();
        if (dbs.size() == 0) return null;

        for (IDBDatabaseService db : dbs) {
            // If we find a database that we can communicate with,
            // we assume it is the right database.  So any exceptions that
            // might have happened before are forgotten.
            final ISPProgram prog = db.lookupProgramByID(progId);
            if (prog != null) {
                final SPProgram dataObj = (SPProgram) prog.getDataObject();
                if (dataObj != null) {
                    return dataObj.getGsaAspect();
                }
            }
        }
        return null;
    }

    public DatasetRecord update(DatasetLabel label, DatasetRecordTemplate update,
                                DatasetRecordTemplate precond)
            throws InterruptedException {

        return _recordUpdater.update(label, update, precond);
    }

    public DatasetRecord updateOrCreate(Dataset dataset, DatasetRecordTemplate update,
                                        DatasetRecordTemplate precond)
            throws InterruptedException {
        return _recordUpdater.update(dataset, update, precond);
    }

    public void addListener(DatasetRecordListener listener) {
        synchronized (_listeners) {
            _listeners.add(listener);
        }
    }

    public void removeListener(DatasetRecordListener listener) {
        synchronized (_listeners) {
            _listeners.remove(listener);
        }
    }

    private void _fireLocalEvents(Collection<DsetRecordChange> updates) {
        Set<DatasetRecordListener> lsSet;
        synchronized (_listeners) {
            lsSet = new HashSet<DatasetRecordListener>(_listeners);
        }

        for (DsetRecordChange dru : updates) {
            DatasetRecordEvent evt;
            evt = new DsetRecordEventImpl(this, dru.oldRecord, dru.newRecord);
            for (DatasetRecordListener ls : lsSet) {
                ls.datasetModified(evt);
            }
        }
    }

    private static void _logTrigger(Collection<DsetRecordChange> update) {
        StringBuilder buf = new StringBuilder();
        buf.append("Dataset record changes:\n");
        for (DsetRecordChange change : update) {
            buf.append('\t').append(change.getDifferences());
        }
        LOG.fine(buf.toString());
    }

    public synchronized void obsRecTrigger(Collection<DsetRecordChange> update) {
        if (LOG.isLoggable(Level.FINE)) _logTrigger(update);
        if (_worker == null) return;
        _worker.addUpdate(update);
    }

    public synchronized void start() {
        if (_worker != null) return; // already started
        _worker = new NotificationWorker();
        _workerThread = new Thread(_worker, "DatasetRecordServiceImplWorker");
        _workerThread.setDaemon(true);
        _workerThread.start();

        _triggerClient.setEnabled(true);
        _recordUpdater.start();
    }

    public synchronized void stop() {
        if (_worker == null) return; // already stopped
        _recordUpdater.stop();
        _triggerClient.setEnabled(false);
        _worker.stop();
        _worker = null;
        _workerThread.interrupt();
        _workerThread = null;
    }

    public synchronized void addDatabase(IDBDatabaseService database) {
        LOG.info("** new database: " + System.identityHashCode(database));
        _triggerClient.addDatabase(database);
        _recordUpdater.addDatabase(database);
        _dbs.add(database);
        database.removeProgramEventListener(_progReplaceHandler);
        database.addProgramEventListener(_progReplaceHandler);
    }

    public synchronized void removeDatabase(IDBDatabaseService database) {
        LOG.info("** remove database: " + System.identityHashCode(database));
        _triggerClient.removeDatabase(database);
        _recordUpdater.removeDatabase(database);
        _dbs.remove(database);
        database.removeProgramEventListener(_progReplaceHandler);
    }

    public synchronized int getDatabaseCount() {
        return _dbs.size();
    }
}
