//
// $Id: Copier.java 522 2006-08-10 14:30:36Z shane $
//

package edu.gemini.auxfile.workflow;

import edu.gemini.auxfile.api.AuxFileListener;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles copying auxiliary files around the summit when they are posted to
 * the database.  It implements the AuxFileListener interface to receive update
 * events from the backend server.
 */
public final class Workflow implements AuxFileListener {
    private static final Logger LOG = Logger.getLogger(Workflow.class.getName());
    private static final long RETRY_PERIOD = 60 * 60 * 1000;

    private class RetryTask extends TimerTask {
        private Worker _worker;

        RetryTask(Worker worker) {
            _worker = worker;
        }

        public void run() {
            Collection<AuxFileTask> tasks = new ArrayList<AuxFileTask>(_state.getTasks());
            for (AuxFileTask task : tasks) {
                _worker.addTask(task);
            }
        }
    }

    /**
     * Defines the actions of a worker thread that loads the available tasks
     * from a file and executes the copy of each corresponding file.
     */
    private class Worker implements Runnable {
        private boolean _stop;

        private List<AuxFileTask> _pendingTasks = new ArrayList<AuxFileTask>();

        synchronized void addTask(AuxFileTask task) {
        	for (Iterator<AuxFileTask> it = _pendingTasks.iterator(); it.hasNext(); ) {
        		AuxFileTask pendingTask = it.next();
                if (pendingTask.getFile().equals(task.getFile()) && pendingTask.getClass() == task.getClass())
                	it.remove();
            }
            _pendingTasks.add(task);
            notify();
        }

        private synchronized AuxFileTask _getNextTask() {
            while (_pendingTasks.size() == 0) {
                try {
                    LOG.log(Level.INFO, "auxfile-copier Worker waiting for next task");
                    wait();
                    LOG.log(Level.INFO, "auxfile-copier Worker processing next task");
                } catch (InterruptedException ex) {
                    LOG.log(Level.INFO, "auxfile-copier Worker interrupted while waiting for next task");
                    return null;
                }
            }
            return _pendingTasks.get(0);
        }

        private synchronized void _clearCurrentTask() {
            _pendingTasks.remove(0);
        }

        private void _executeTask(AuxFileTask _task) {
            try {
            	_task.execute(Workflow.this);
            } catch (Exception ex) {
                String msg =
                	"Problem executing auxfile task of type " + _task.getClass().getSimpleName() +
                	" for file: " + _task.getFile() +
                    " for program " + _task.getProgId().stringValue();
                LOG.log(Level.SEVERE, msg, ex);
            } finally {
                _clearCurrentTask();
            }
        }

        private synchronized boolean isStopped() {
            return _stop;
        }

        synchronized void stop() {
            _stop = true;
        }

        public void run() {
            // loop getting tasks and executing them until stopped
            try {
                LOG.log(Level.INFO, "Start auxfile-copier Worker");
                while (!isStopped()) {
                    AuxFileTask task = _getNextTask();
                    if ((task == null) || isStopped()) {
                        LOG.log(Level.INFO, "auxfile-copier stopping");
                        return;
                    }
                    _executeTask(task);
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Stopping auxfile-copier Worker", ex);
            } finally {
                LOG.log(Level.INFO, "Stopped auxfile-copier Worker");
            }
        }
    }

    private final CopyTaskState _state;
    private final Mailer _mailer;

    private Thread _workerThread;
    private Worker _worker;
    private Timer _retryTimer;
    private RetryTask _retryTask;

    public Workflow(CopyTaskState state, Mailer mailer) {
        _state  = state;
        _mailer = mailer;
    }

    public synchronized void start() {
        if (_worker != null) return;

        // Start the worker thread.
        _worker = new Worker();
        _workerThread = new Thread(_worker, "Worlflow.Worker");
        _workerThread.start();

        _retryTimer = new Timer("Worlflow Retry Timer", true);
        _retryTask = new RetryTask(_worker);
        _retryTimer.scheduleAtFixedRate(_retryTask, 60000, RETRY_PERIOD);
    }

    public synchronized void stop() {
        if (_worker == null) return;

        _retryTask.cancel();
        _retryTask = null;
        _retryTimer.cancel();
        _retryTimer = null;

        // Stop the worker thread
        _worker.stop();
        _workerThread.interrupt();

        _worker = null;
        _workerThread = null;
    }

    public void filesDeleted(SPProgramID progId, Collection<String> filenames) {
        // ignore
    }

    public void fileFetched(SPProgramID progId, File file) {
        // ignore
    }

    public void descriptionUpdated(SPProgramID progId, String description, Collection<File> files) {
        // ignore
    }

    public void lastEmailedUpdated(SPProgramID progId, Option<Instant> lastEmailed, Collection<File> files) {
        // ignore
    }

    public void checkedUpdated(SPProgramID progId, boolean newChecked, Collection<File> files) {
        synchronized (this) {
            if (_worker != null) {
            	for (File file: files) {
                	LOG.info("Checked status updated: " + file + " => " + newChecked);
            		CheckTask task = new CheckTask(progId, file, newChecked);
                	_worker.addTask(task);
            	}
            }
        }
    }

    public void fileStored(SPProgramID progId, File file) {
        LOG.info("AuxFile stored: " + file);

        // Add the task for this file.
        CopyTask task = new CopyTask(_state, progId, file, file.lastModified());
        _state.addTask(task);

        // Let the worker know that there is a new file to copy.
        synchronized (this) {
            if (_worker != null) _worker.addTask(task);
        }
    }

	public Mailer getMailer() {
		return _mailer;
	}
}
