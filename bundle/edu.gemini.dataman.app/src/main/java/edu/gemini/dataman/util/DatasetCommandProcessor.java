//
// $Id: DatasetCommandProcessor.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.dataman.util;

import edu.gemini.spModel.dataset.DatasetLabel;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.*;

/**
 * The DatasetCommandProcessor handles concurrent execution of
 * {@link DatasetCommand}s, queueing commands associated with the same
 * {@link DatasetLabel} for sequential execution.
 *
 * <p>The DatasetCommandProcessor is a singleton referenced via the
 * {@link #INSTANCE} field.  However, it may be stopped and started without
 * knowledge of the client as underlying services that the Dataman app
 * depends upon come and go.
 */
public final class DatasetCommandProcessor {
    private static final Logger LOG = Logger.getLogger(DatasetCommandProcessor.class.getName());

    public static final DatasetCommandProcessor INSTANCE =
                                                 new DatasetCommandProcessor();

    private static final int MAX_THREADS = 10;
    // lower the thread count to 1 to work around a problem with simultaneous
    // access to ObservationCB
//    private static final int MAX_THREADS = 1;

    // Queue of commands for the same dataset.
    private static class CommandQueue implements Callable<DatasetLabel> {
        private DatasetLabel _label;
        private Queue<DatasetCommand> _cmdQ = new LinkedList<DatasetCommand>();

        public CommandQueue(DatasetCommand firstCommand) {
            _label = firstCommand.getLabel();
            _cmdQ.offer(firstCommand);
        }

        public DatasetLabel getLabel() {
            return _label;
        }

        // Returns <code>true</code> if the command was added;
        // <code>false</code> otherwise.  If not added, this implies that the
        // queue is complete and will be removed from the map of commands.
        synchronized boolean add(DatasetCommand cmd) {
            // Don't add to an empty queue.  If the queue is empty, it means
            // that its call() method will be terminating soon and a new queue
            // will be created for cmd.
            if (_cmdQ.size() == 0) return false;
            _cmdQ.offer(cmd);
            return true;
        }

        synchronized boolean isFinished() {
            return _cmdQ.size() == 0;
        }

        private synchronized DatasetCommand _getFirstCommand() {
            return _cmdQ.peek();
        }

        private synchronized DatasetCommand _getNext() {
            _cmdQ.remove();
            return _cmdQ.peek();
        }

        public DatasetLabel call() {
            DatasetCommand cmd = _getFirstCommand();
            while (cmd != null) {
                try {
                    cmd.call();
                } catch (Throwable ex) {
                    String msg = "DatasetCommand exception for " + _label;
                    LOG.log(Level.WARNING, msg, ex);
                }
                cmd = _getNext();
            }

            synchronized (this) {
                notifyAll(); // in case anybody is waiting
            }

            return _label;
        }

        public synchronized boolean waitUntilFinished(long timeout) throws InterruptedException {
            long now  = System.currentTimeMillis();
            long then = now + timeout;
            while ((now < then) && !isFinished()) {
                wait(then - now);
                now = System.currentTimeMillis();
            }
            return now < then;
        }
    }

    // Runnable responsible for removing CommandQueues from the map when they
    // run out of DatasetCommands.
    private class MapCleaner implements Runnable {
        private boolean _stop;

        synchronized void stop() {
            _stop = true;
        }

        synchronized boolean isStopped() {
            return _stop;
        }

        public void run() {
            do {
                try {
                    Future<DatasetLabel> fut = _pool.take();
                    _cleanup(fut.get());
                } catch (ExecutionException ex) {
                    // means the CommandQueue job itself failed
                    LOG.log(Level.SEVERE, "Fatal DatasetCommandProcessor problem", ex);
                    break;
                } catch (InterruptedException ex) {
                    LOG.info("Interrupted PoolCleaner, stopped cleanup");
                }
            } while (!isStopped());
        }
    }

    private ExecutorCompletionService<DatasetLabel> _pool;
    private ExecutorService _exec;
    private Map<DatasetLabel, CommandQueue> _cmdMap =
                                    new HashMap<DatasetLabel, CommandQueue>();

    private MapCleaner _mapCleaner;
    private Thread _mapCleanerThread;

    private DatasetCommandProcessor() {
    }

    /**
     * Starts the processor, enabling it to receive and execute commands.
     */
    public synchronized void start() {
        if (_pool != null) return; // already running

        _exec = Executors.newFixedThreadPool(MAX_THREADS);
        _pool = new ExecutorCompletionService<DatasetLabel>(_exec);
        _mapCleaner = new MapCleaner();
        _mapCleanerThread = new Thread(_mapCleaner, "DatasetCommandProcessor.MapCleaner");
        _mapCleanerThread.setPriority(Thread.NORM_PRIORITY - 1);
        _mapCleanerThread.start();
    }

    /**
     * Shuts down the processor, meaning that no new or existing commands will
     * be executed by this processor again until and unless it is restarted.
     */
    public synchronized void stop() {
        if (_pool == null) return; // not running

        _mapCleaner.stop();
        _mapCleanerThread.interrupt();
        _mapCleaner = null;
        _mapCleanerThread = null;

        _exec.shutdownNow();
        _exec = null;

        _pool = null;

        _cmdMap.clear();
    }

    /**
     * @return <code>true</code> if the processor has been shutdown;
     * <code>false</code> otherwise
     */
    public synchronized boolean isStarted() {
        return _pool != null;
    }

    /**
     * Adds the given command to the processor.  Commands associated with the
     * same dataset are executed sequentially in the order of arrival.
     * Commands associated with different datasets may be executed concurrently.
     *
     * <p>It is not an error to add a command to the processor even if it isn't
     * running.  However, if not running the command will be ignored.
     *
     * @return <code>true</code> if the command is added to the processor;
     * <code>false</code> if the command is not added because the processor
     * has been stopped
     */
    public synchronized boolean add(DatasetCommand command) {
        if (_pool == null) return false;

        CommandQueue q = _cmdMap.get(command.getLabel());
        if ((q == null) || !q.add(command)) {
            q = new CommandQueue(command);
            _cmdMap.put(command.getLabel(), q);
            _pool.submit(q);
        }
        return true;
    }

    /**
     * Determines whether there are any outstanding commands for dataset
     * associated the given <code>label</code>.
     * @param label label of the dataset to check
     *
     * @return <code>true</code> if there is outstanding processing for this
     * dataset; <code>false</code> if there is no ongoing processing for this
     * dataset at the time of this call
     */
    public synchronized boolean isProcessing(DatasetLabel label) {
        CommandQueue q = _cmdMap.get(label);
        return (q != null) && !q.isFinished();
    }

    /**
     * Waits until the commands associated with the given dataset
     * <code>label</code> have finished, giving up after <code>timeout</code>
     * milliseconds have passed.
     *
     * <p>Note that a new command for the same dataset may be added immediately
     * after this method makes the determination that the commands have
     * completed, or the timeout could be reached and immediately thereafter all
     * commands could complete.   This method is intended more for test cases
     * which run in a controlled environment.
     *
     * @param label dataset label whose command processing status is sought
     * @param timeout time in milliseconds to wait before giving up
     *
     * @return <code>true</code> if the command processor had no active or
     * pending commands for <code>label</code> at some point during the
     * <code>timeout</code> milliseconds; <code>false</code> if the processor
     * was still executing at least one command for this dataset and
     * <code>timeout</code> milliseconds have passed
     */
    public boolean waitUntilFinished(DatasetLabel label, long timeout) throws InterruptedException {
        CommandQueue q;
        synchronized (this) {
            q = _cmdMap.get(label);
        }
        if (q == null) return true;
        return q.waitUntilFinished(timeout);
    }

    private synchronized void _cleanup(DatasetLabel label) {
        // Ordinarily this means the command is finished so the queue should be
        // removed.  Timing could be such that a queue finished up just before
        // another command for the same dataset label arrived.  In this case,
        // a new queue would have replaced the old one.  We don't want to remove
        // it in this case.
        CommandQueue q = _cmdMap.remove(label);
        if ((q != null) && !q.isFinished()) _cmdMap.put(label, q);
    }
}
