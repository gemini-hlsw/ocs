// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: StorageManager.java 46971 2012-07-25 16:59:17Z swalker $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.ISPRootNode;
import edu.gemini.spModel.core.SPProgramID;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>StorageManager</code>
 */
final class StorageManager<N extends ISPRootNode> implements ProgramEventListener<N> {
    private static final Logger LOG = Logger.getLogger(StorageManager.class.getName());

    /**
     * The default storage interval, which controls how often dirty programs
     * are saved to the database.  By default the value is 10 seconds.
     */
    public static final long DEFAULT_STORAGE_INTERVAL = 1000 * 10;


    /**
     * The StorageWorker is a helper class that contains a thread that
     * periodically wakes up and stores any modified programs.
     */
    private class StorageWorker {
        private long _storageInterval = DEFAULT_STORAGE_INTERVAL;
        private Thread _storageThread;
        private StorageRunnable _storageRunnable;

        //
        // Runnable that does the storage work.
        //
        private class StorageRunnable implements Runnable {
            volatile boolean done = false;

            public void run() {
                while (!done) {
                    _storeDirtyPrograms();
                    try {
                        Thread.sleep(_storageInterval);
                    } catch (InterruptedException ex) {
                        LOG.log(Level.FINE, "ODB StorageRunnable interrupted.");
                    }
                }
            }
        }

        /**
         * Start the storage worker, if it isn't already running.
         */
        public synchronized void start() {
            if (_storageThread != null) {
                return;
            }
            _storageRunnable = new StorageRunnable();
            _storageThread = new Thread(_storageRunnable, "ODB StorageRunnable");
            _storageThread.setPriority(Thread.NORM_PRIORITY - 1);
            _storageThread.start();
        }


        /**
         * Stop the storage worker, if it is running.
         */
        public synchronized void stop() {
            if (_storageThread == null) {
                return;
            }

            _storageRunnable.done = true;
            _storageThread.interrupt();
            _storageThread = null;
        }

        /**
         * Gets the time period (in milliseconds) that the storage worker
         * will sleep between periods of writing dirty programs (if any).
         */
        public synchronized long getStorageInterval() {
            return _storageInterval;
        }

        /**
         * Sets the time period (in milliseconds) that the storage worker
         * will sleep between periods of writing dirty programs.
         *
         * @throws IllegalArgumentException if interval is 0 or less
         */
        public synchronized void setStorageInterval(long interval) throws IllegalArgumentException {
            if (_storageInterval < 1) {
                throw new IllegalArgumentException("storage interval must be > 0");
            }

            _storageInterval = interval;
            if (_storageThread != null) {
                _storageThread.interrupt();
            }
        }
    }


    private final ProgramManager<N> _progMan;
    private final IDBPersister _persister;
    private final DirtyProgramListener<N> _dirty;
    private final StorageWorker _storeWorker;


    /**
     * Creates the <code>StorageManager</code> with the <code>FileManager</code>
     * used to store modified/added programs.
     */
    StorageManager(ProgramManager<N> pm, IDBPersister persister)  {
        _progMan   = pm;
        _persister = persister;
        _dirty     = new DirtyProgramListener<N>();

        pm.addListener(this);

        // Add the dirty listener to all the existing programs.
        for (N prog : pm.getPrograms()) prog.addCompositeChangeListener(_dirty);

        // Start the thread that periodically looks for modifications.
        _storeWorker = new StorageWorker();
        _storeWorker.start();
    }

    /**
     * Shuts down the storage manager, first saving any modified programs.
     * The storage manager will no longer function after a call to shutdown.
     */
    void shutdown() {
        // Stop the storage thread.
        _storeWorker.stop();

        // Do some cleanup, removing listeners.
        _progMan.removeListener(this);

        for (N prog : _progMan.getPrograms()) prog.removeCompositeChangeListener(_dirty);

        // Write out any last modifications.
        _storeDirtyPrograms();
    }

    /**
     * Gets the storage interval (period in ms), which controls how often dirty
     * programs are saved to disk.
     */
    long getStorageInterval() {
        return _storeWorker.getStorageInterval();
    }

    /**
     * Sets the storage interval (period in ms), which controls how often dirty
     * programs are saved to disk.
     */
    void setStorageInterval(long periodMS) throws IllegalArgumentException {
        _storeWorker.setStorageInterval(periodMS);
    }

    /**
     * Stores the given program and adds it to the set of programs being
     * monitored for changes.   Implements the
     * <code>{@link ProgramEventListener#programAdded}</code> method.
     */
    public void programAdded(ProgramEvent<N> pme) {
        final N prog = pme.getNewProgram();
        try {
            _persister.store(prog);
            prog.addCompositeChangeListener(_dirty);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Program adding program", ex);
        }
    }

    public void programReplaced(ProgramEvent<N> pme) {
        programRemoved(pme);
        programAdded(pme);
    }

    /**
     * Removes the file associated with the given program and quits monitoring
     * it for changes.  Implements the
     * <code>{@link ProgramEventListener#programRemoved}</code> method.
     */
    public void programRemoved(ProgramEvent<N> pme) {
        N prog = pme.getOldProgram();
        prog.removeCompositeChangeListener(_dirty);
        _dirty.removeProgram(prog);
        _persister.remove(prog.getNodeKey());
    }

    /**
     * Stores all the modified programs, if any.
     */
    private void _storeDirtyPrograms() {
        for (N n : _dirty.getDirtyPrograms()) {
            try {
                _persister.store(n);
            } catch (Exception ex) {
                log(n, ex);
            }
        }
    }

    /**
     * Checkpoints the given program.  Any outstanding modifications to the
     * program are stored.
     */
    void checkpoint(N prog) {
        _dirty.removeProgram(prog);

        try {
            _persister.store(prog);
        } catch (Exception ex) {
            log(prog, ex);
        }
    }

    private static void log(ISPRootNode prog, Exception ex) {
        LOG.log(Level.SEVERE, "Couldn't store program " + getId(prog), ex);
    }

    private static String getId(ISPRootNode mab) {
        final SPProgramID progId = mab.getProgramID();
        return (progId == null) ? mab.getProgramKey().toString() : progId.toString();
    }

    /** Checkpoints all the outstanding modifications. */
    void checkpoint() { _storeDirtyPrograms(); }
}
