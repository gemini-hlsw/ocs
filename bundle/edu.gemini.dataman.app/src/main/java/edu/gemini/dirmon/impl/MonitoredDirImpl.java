//
// $Id: MonitoredDirImpl.java 244 2006-01-03 18:47:49Z shane $
//
package edu.gemini.dirmon.impl;

import edu.gemini.dirmon.DirEvent;
import edu.gemini.dirmon.DirListener;
import edu.gemini.dirmon.DirLocation;
import edu.gemini.dirmon.MonitoredDir;
import edu.gemini.dirmon.util.DefaultDirLocation;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A collection of Files belonging to a directory, along with listener support
 * for being notified of updates, additions, and deletions.
 */
public final class MonitoredDirImpl implements MonitoredDir {
    private static final Logger LOG = Logger.getLogger(MonitoredDirImpl.class.getName());

    private static final List<File> EMPTY_FILE_LIST = new ArrayList<File>(0);

    private static void _logScan(Level level, String type, DirLocation loc, long start, DirEvent evt) {
        if (!LOG.isLoggable(level)) return;

        StringBuilder buf = new StringBuilder();
        buf.append("----------- ").append(type).append(" scan of ");
        buf.append(loc).append(": ");

        long end = System.currentTimeMillis();
        long total = end - start;
        buf.append(total).append(" ms\n");
        buf.append(evt.toString());

        LOG.log(level, buf.toString());
    }

    private class FullScanTask extends TimerTask {
        public void run() {
            long start = System.currentTimeMillis();

            DirScanResults res = _dirState.doFullScan();
            if (res != null) {
                DirEvent evt = new DirEventImpl(MonitoredDirImpl.this, res);
                _logScan(Level.FINER, "full", getDirLocation(), start, evt);
                _fireEvent(evt);
            }
        }
    }

    private class ActiveScanTask extends TimerTask {
        private List<File> _activeCopy = new ArrayList<File>();

        public void run() {
            ActiveFiles af = getActiveFiles();
            if (af == null) return;

            long start = System.currentTimeMillis();

            _activeCopy.clear();
            af.copyActiveFilesTo(_activeCopy);
            DirScanResults res = _dirState.doPartialScan(_activeCopy);
            if (res != null) {
                DirEvent evt = new DirEventImpl(MonitoredDirImpl.this, res);
                _logScan(Level.FINER, "active", getDirLocation(), start, evt);
                _fireEvent(evt);
            }
        }
    }

    private class Scanner implements Runnable {
        private Timer _timer;

        synchronized void start() {
            if (_timer == null) {
                _timer = new Timer(true);
                Thread initThread = new Thread(this);
                initThread.start();
            }
        }

        synchronized void stop() {
            if (_timer != null) {
                _timer.cancel();
                _timer = null;
            }
        }

        public void run() {
            _dirState.doFullScan();
            synchronized (this) {
                if (_timer == null) return;
                markInitialized(_dirState.getLatestModTime());
                long period = _config.getFullDirPollPeriod();
                _timer.schedule(new FullScanTask(), period, period);
                period = _config.getActiveSetPollPeriod();
                _timer.schedule(new ActiveScanTask(), period, period);
            }
        }
    }

    private DirState _dirState;
    private ActiveFiles _activeFiles;
    private Scanner _scanner;

    private DirLocation _loc;
    private final MonitoredDirConfig _config;

    private final MonitoredDir _stub;

    private List<DirListener> _listeners = new ArrayList<DirListener>();
    private boolean _initialized;

//    private ExecutorService _pool = Executors.newCachedThreadPool();
    private ExecutorService _pool = Executors.newFixedThreadPool(5);

    public MonitoredDirImpl(DirLocation loc) throws IOException {
        this(loc, MonitoredDirConfig.DEFAULT);
    }

    public MonitoredDirImpl(DirLocation loc, MonitoredDirConfig config) throws IOException {
        _stub   = this; //(MonitoredDir) UnicastRemoteObject.exportObject(this, 0);
        _config = config;
        _setDirLocation(loc);
    }

    public synchronized void start() {
        if (_scanner == null) {
            _scanner = new Scanner();
            _scanner.start();
        }
    }

    public synchronized void stop() {
        if (_scanner != null) {
            _scanner.stop();
            _scanner = null;
        }
    }

    public synchronized DirLocation getDirLocation() {
        return _loc;
    }

    private synchronized ActiveFiles getActiveFiles() {
        return _activeFiles;
    }

    private synchronized void _setDirLocation(DirLocation loc) throws java.io.IOException {
        if (loc == null) throw new NullPointerException("loc is null");

        if (_scanner != null) {
            throw new IllegalStateException("Currently monitoring a directory.");
        }

        // Record an immutable version of the location.
        if (loc instanceof DefaultDirLocation) {
            _loc = loc;
        } else {
            _loc = new DefaultDirLocation(loc);
        }

        File dir = new File(_loc.getDirPath());
        if (!dir.exists()) {
            String msg = "Directory '" + dir.getName() + "' does not exist.";
            LOG.severe(msg);
            throw new IOException(msg);
        }

        if (!(dir.isDirectory() && dir.canRead())) {
            String msg = "Directory '" + dir.getName() + "' is not a readable dir.";
            LOG.severe(msg);
            throw new IOException(msg);
        }

        if (!(dir.isAbsolute())) {
            dir = dir.getAbsoluteFile();
        }

        CompletionPolicy compPolicy = _config.getCompletionPolicy();
        if (compPolicy == null) compPolicy = CompletionPolicy.IMMEDIATE;

        _dirState    = new DirState(dir, compPolicy, _config.getFileFilter());
        _activeFiles = new ActiveFiles(dir, _config.getActiveSetSize());
    }

    public MonitoredDir getStub() {
        return _stub;
    }

    /**
     * Adds a file event listener.  The listener will receive any subsequent
     * {@link DirEvent directory events}.
     */
    public void addListener(final DirListener listener) {
        boolean isInitialized;
        synchronized (this) {
            _listeners.add(listener);
            isInitialized = _initialized;
        }
        if (isInitialized) {
            // The init() fired from markInitialized didn't (or won't) include
            // this listener, so sent one specifically to this listener.
            _pool.submit(new Runnable() {
                public void run() {
/*                    try {*/
                        listener.init(_stub, getLastModified());
/*                    } catch (RemoteException ex) {*/
/*                        LOG.log(Level.SEVERE, "Could not deliever init notification", ex);*/
                        // if the remote client is dead, then the listener will
                        // eventually be removed
/*                    }*/
                }
            });
        }
    }

    /**
     * Removes a directory event listener, if present.  May be called from
     * within the event handler.
     */
    public synchronized void removeListener(DirListener listener) {
        _listeners.remove(listener);
    }

    /**
     * Removes all the listeners.
     */
    public synchronized void removeAllListeners() {
        _listeners.clear();
    }

    /**
     * Gets the number of listeners (number of clients) of this class.
     */
    public synchronized int getListenerCount() {
        return _listeners.size();
    }

    private void _fireEvent(final DirEvent event) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(event.toString());
        }

        List<DirListener> listeners;
        synchronized (this) {
            listeners = new ArrayList<DirListener>(_listeners);
        }
        for (final DirListener dl : listeners) {
            _pool.submit(new Runnable() {
                public void run() {
/*                    try {*/
                        dl.dirModified(event);
/*                    } catch (RemoteException ex) {*/
/*                        LOG.log(Level.SEVERE, "Could not deliever directory event", ex);*/
                        // if the remote client is dead, then the listener will
                        // eventually be removed
/*                    } catch (Throwable t) {*/
/*                        LOG.log(Level.SEVERE, "Unexpected exception firing event: " + event, t);*/
/*                    }*/
                }
            });
        }
    }

    void markInitialized(final long lastModified) {
        List<DirListener> listeners;
        synchronized (this) {
            _initialized = true;
            listeners = new ArrayList<DirListener>(_listeners);
        }
        for (final DirListener dl : listeners) {
            _pool.submit(new Runnable() {
                public void run() {
                    try {
                        dl.init(_stub, lastModified);
//                    } catch (RemoteException ex) {
//                        LOG.log(Level.SEVERE, "Could not deliever init notification", ex);
                        // if the remote client is dead, then the listener will
                        // eventually be removed
                    } catch (Throwable t) {
                        LOG.log(Level.SEVERE, "Unexpected exception initializing listener", t);
                    }
                }
            });
        }
    }

    public synchronized long getLastModified() {
        if (_dirState == null) return -1;
        return _dirState.getLatestModTime();
    }

    public synchronized Collection<File> getCurrentFiles() {
        if (_dirState == null) return EMPTY_FILE_LIST;
        return _dirState.getAllFiles();
    }

    public synchronized Collection<File> getModified(long sinceAfter, long through) {
        if (_dirState == null) return EMPTY_FILE_LIST;
        return _dirState.getAllFiles(sinceAfter, through);
    }

    public synchronized void expectUpdates(String filename) {
        if (_activeFiles != null) {
            _activeFiles.addFile(filename);
        }
    }

    public synchronized void expectUpdates(Collection<String> filenames) {
        if (_activeFiles == null) return;

        for (String f : filenames) {
            _activeFiles.addFile(f);
        }
    }

    public void discard() {
        removeAllListeners();
//        try {
//            UnicastRemoteObject.unexportObject(this, true);
//        } catch (NoSuchObjectException ex) {
//            LOG.log(Level.WARNING, "Missing monitored dir impl", ex);
//        }
    }
}
