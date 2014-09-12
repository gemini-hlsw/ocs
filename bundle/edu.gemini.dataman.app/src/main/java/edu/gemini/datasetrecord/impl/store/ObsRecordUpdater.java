//
// $Id: ObsRecordUpdater.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.impl.store;

import edu.gemini.datasetrecord.DatasetRecordTemplate;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetRecord;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The ObsRecordUpdater is charged with keeping ObsRecords up to date with
 * dataset changes.  It ensures that multiple updates for the same observation
 * are either grouped into one request or else queued so that simultaneous
 * updates aren't attempted.  That's important because only one update at a
 * time can actually succeed due to version-ing of the data object.
 *
 * <p>Requests to update a dataset record are delayed slightly in order to
 * group multiple requests when possible.  A delay of {@link #MAX_START_DELAY}
 * is the maximum that an update request should be delayed.  However, if new
 * requests to update datasets in the same observation are spaced by more than
 * {@link #MAX_IDLE_DELAY}, the request will be sent sooner.
 */
public final class ObsRecordUpdater {
    private static final Logger LOG = Logger.getLogger(ObsRecordUpdater.class.getName());

    private static final long MAX_START_DELAY = 10000;
    private static final long MAX_IDLE_DELAY  =   500;

    private final Set<Principal> _user;

    public ObsRecordUpdater(Set<Principal> user) {
        this._user = user;
    }

    /**
     * Pending and active tasks for a particular observation.
     */
    private static class UpdateTasks {
        ObsRecordUpdateTask pendingTask;
        ObsRecordUpdateTask activeTask;
    }

    /**
     * A adapter that allows the _startTask method to be called from a Timer.
     */
    private class TaskStarter extends TimerTask {
        private SPObservationID _obsId;

        TaskStarter(SPObservationID obsId) {
            _obsId = obsId;
        }

        public void run() {
            _startPendingTask(_obsId);
        }
    }

    private Timer _taskStarterTimer;
    private ExecutorService _taskPool;

    private final Collection<IDBDatabaseService>  _dbs = new HashSet<IDBDatabaseService>();
    private Map<SPObservationID, UpdateTasks> _taskMap = new HashMap<SPObservationID, UpdateTasks>();

    public synchronized void start() {
        _taskStarterTimer = new Timer("ObsRecordUpdater", true);
        _taskPool = Executors.newCachedThreadPool();
    }

    public synchronized void stop() {
        _taskStarterTimer.cancel();
        _taskPool.shutdown();
    }

    public void addDatabase(IDBDatabaseService db) {
        synchronized (_dbs) {
            _dbs.add(db);
        }
    }

    public void removeDatabase(IDBDatabaseService db) {
        synchronized (_dbs) {
            _dbs.remove(db);
        }
    }

    private Collection<IDBDatabaseService> _getDbs() {
        Collection<IDBDatabaseService> res;
        synchronized (_dbs) {
            res = new ArrayList<IDBDatabaseService>(_dbs);
        }
        return res;
    }

    private synchronized ObsRecordUpdateTask _addRequest(DsetRecordUpdateRequest req)
            throws InterruptedException {

        StringBuilder buf = new StringBuilder();
        if (LOG.isLoggable(Level.FINE)) {
            buf.append("** Update request for ");
            buf.append(req.getLabel());
            buf.append('\n');
        }

        SPObservationID obsId = req.getLabel().getObservationId();
        UpdateTasks ti = _taskMap.get(obsId);

        if (ti == null) {
            ti = new UpdateTasks();
            _taskMap.put(obsId, ti);
            if (LOG.isLoggable(Level.FINE)) {
                buf.append("\tNothing recorded for obs ");
                buf.append(obsId);
                buf.append(", added a new UpdateTasks object.\n");
            }
        }

        if (ti.pendingTask == null) {
            ti.pendingTask = new ObsRecordUpdateTask(obsId, _getDbs(), _user);
            if (LOG.isLoggable(Level.FINE)) {
                buf.append("\tAdded a new pending task for obs ");
                buf.append(obsId);
                buf.append('\n');
            }
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                buf.append("\tEnqueued a new request for obs ");
                buf.append(obsId);
                buf.append('\n');
            }
        }

        ti.pendingTask.addPendingRequest(req);

        if (LOG.isLoggable(Level.FINE)) LOG.fine(buf.toString());
        return ti.pendingTask;
    }

    /**
     * This task is submitted to the task pool when it is time to do the update
     * of the remote observation.  It simply calls the
     * {@link ObsRecordUpdateFunctor} that it wraps and then checks whether any
     * other tasks have been en-queued for the same observation.
     */
    private class TaskWrapper implements Callable<Map<DatasetLabel, DatasetRecord>> {
        private ObsRecordUpdateTask _task;

        TaskWrapper(ObsRecordUpdateTask task) {
            _task = task;
        }

        public Map<DatasetLabel, DatasetRecord> call() throws Exception {
            // Here we will do the task but catch any exception that may
            // come up (such as a database communication error).  That way
            // we can cleanup this task and process any pending tasks whether
            // there is a problem or not.
            Exception problem = null;
            Map<DatasetLabel, DatasetRecord> res = null;
            try {
                res = _task.call();
            } catch (Exception ex) {
                problem = ex;
            }

            // Cleanup.
            SPObservationID obsId = _task.getObservationId();
            _finishActiveTask(obsId);
            _startPendingTask(obsId);

            // Now that the cleanup is done, rethrow the problem.
            if (problem != null) throw problem;
            return res;
        }
    }

    private synchronized void _finishActiveTask(SPObservationID obsId) {
        StringBuilder buf = new StringBuilder();
        buf.append("*** Finish update task for obs ");
        buf.append(obsId);
        buf.append('\n');

        UpdateTasks ti = _taskMap.get(obsId);
        if (ti == null) {
            // this is an error condition
            buf.append("\tNo active or pending tasks for obs!\n");
            LOG.severe(buf.toString());
            // done for now with this observation, no need to check again
            return;
        }

        ti.activeTask = null;
    }

    private synchronized void _startPendingTask(SPObservationID obsId) {
        StringBuilder buf = new StringBuilder();
        buf.append("*** Checking update tasks for obs ");
        buf.append(obsId);
        buf.append('\n');

        UpdateTasks ti = _taskMap.get(obsId);
        if (ti == null) {
            buf.append("\tNo active or pending tasks for obs.\n");
            LOG.fine(buf.toString());
            // done for now with this observation, no need to check again
            return;
        }
        if (ti.activeTask != null) {
            buf.append("\tThere is an active task for obs.\n");
            LOG.fine(buf.toString());
            // return, when the active task finishes it will call _startTask
            return;
        }
        if (ti.pendingTask == null) {
            buf.append("\tThere are no active or pending tasks, cleaning up.\n");
            LOG.fine(buf.toString());
            _taskMap.remove(obsId);
            return;
        }

        long now         = System.currentTimeMillis();
        long lastMod     = ti.pendingTask.getLastModificationTime();
        long creation    = ti.pendingTask.getCreationTime();

        long elapsedTime = now - creation;
        long idleTime    = now - lastMod;

        if (LOG.isLoggable(Level.FINE)) {
            buf.append("\telapsedTime = ");
            buf.append(elapsedTime);
            buf.append('\n');
            buf.append("\tidleTime    = ");
            buf.append(idleTime);
            buf.append('\n');
        }

        if ((elapsedTime >= MAX_START_DELAY) || (idleTime >= MAX_IDLE_DELAY)) {
            if (LOG.isLoggable(Level.FINE)) {
                buf.append("\tSubmitting an update job: ");
                Collection<DatasetLabel> labels = ti.pendingTask.getDatasetLabels();
                buf.append(labels);
                buf.append('\n');
            }
            ti.activeTask  = ti.pendingTask;
            ti.pendingTask = null;
            _taskPool.submit(new TaskWrapper(ti.activeTask));
        } else {
            long t1 = MAX_START_DELAY - elapsedTime;
            long t2 = MAX_IDLE_DELAY - idleTime;
            long min = Math.min(t1, t2);
            if (LOG.isLoggable(Level.FINE)) {
                buf.append("\tNot ready to submit job, wait for ");
                buf.append(min);
                buf.append(" ms\n");
            }
            TaskStarter ts = new TaskStarter(obsId);
            _taskStarterTimer.schedule(ts, min);
        }

        LOG.fine(buf.toString());
    }

    public DatasetRecord update(DatasetLabel label,
                                DatasetRecordTemplate update,
                                DatasetRecordTemplate precond) throws InterruptedException {
        return _update(new DsetRecordUpdateRequest(label, update, precond));
    }

    public DatasetRecord update(Dataset dataset,
                                DatasetRecordTemplate update,
                                DatasetRecordTemplate precond) throws InterruptedException {
        return _update(new DsetRecordUpdateRequest(dataset, update, precond));
    }

    private DatasetRecord _update(DsetRecordUpdateRequest req) throws InterruptedException {

        ObsRecordUpdateTask task = _addRequest(req);

        DatasetLabel label = req.getLabel();
        SPObservationID obsId = label.getObservationId();

        TaskStarter ts = new TaskStarter(obsId);
        _taskStarterTimer.schedule(ts, MAX_IDLE_DELAY);

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Waiting for update to dataset: " + label);
        }
        DatasetRecord res = task.get(label);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Received update: " + res);
        }
        return res;
    }
}
