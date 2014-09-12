//
// $Id: DatasetSync.java 131 2005-09-14 18:22:24Z shane $
//

package edu.gemini.dataman.sync;

import edu.gemini.dataman.context.DatamanContext;

import java.security.Principal;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * A service used to synchronized the state of the datasets in the working
 * directory with their corresponding datasets in the ODB.
 */
public final class DatasetSync {
    private static final Logger LOG = Logger.getLogger(DatasetSync.class.getName());

    // Wraps a dataset sync in order to cancel the running task if needed.
    private static final class DatasetSyncFuture implements Future<Boolean> {
        private DatasetSyncTask _task;
        private Future<Boolean> _delegate;
        public DatasetSyncFuture(DatasetSyncTask task, Future<Boolean> delegate) {
            _task     = task;
            _delegate = delegate;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            LOG.info("Cancelling DatasetSync operation.");
            _task.cancel();
            return _delegate.cancel(mayInterruptIfRunning);
        }

        public boolean isCancelled() {
            return _delegate.isCancelled();
        }

        public boolean isDone() {
            return _delegate.isDone();
        }

        public Boolean get() throws InterruptedException, ExecutionException {
            return _delegate.get();
        }

        public Boolean get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            return _delegate.get(l, timeUnit);
        }
    }

    private DatasetSync() {
        // defeat instantiation
    }

    private static ExecutorService _pool = Executors.newSingleThreadExecutor();

    /**
     * Starts a dataset sync task.  If called multiple times, multiple sync
     * tasks will be queued up one after the other.  Use the Future returned
     * in order to interact with the running task.  The Boolean returned by
     * the Future is <code>true</code> if the sync task ran to completion
     * (regardless of whether individual datasets could not be synced due to
     * some problem).  In other words, it is <code>true</code> if the services
     * required to perform the sync remained present during the entire sync
     * operation.
     *
     * @return a Future that can be used to interact with the running sync task
     */
    public static Future<Boolean> schedule(DatamanContext ctx, Set<Principal> user) {
        LOG.info("Scheduling DatasetSync operation.");
        DatasetSyncTask task = new DatasetSyncTask(ctx, user);
        return new DatasetSyncFuture(task, _pool.submit(task));
    }

}
