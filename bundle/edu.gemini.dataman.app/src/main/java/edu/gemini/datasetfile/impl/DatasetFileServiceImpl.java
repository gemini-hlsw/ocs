//
// $Id: DatasetFileServiceImpl.java 452 2006-06-27 21:25:47Z shane $
//

package edu.gemini.datasetfile.impl;

import edu.gemini.datasetfile.*;
import edu.gemini.dirmon.DirEvent;
import edu.gemini.dirmon.DirListener;
import edu.gemini.dirmon.MonitoredDir;
import edu.gemini.fits.HeaderItem;
import edu.gemini.fits.Hedit;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetQaState;

import java.io.File;
import java.io.IOException;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public final class DatasetFileServiceImpl implements DatasetFileService, DirListener {
    private static final Logger LOG = Logger.getLogger(DatasetFileServiceImpl.class.getName());

    private static final int CACHE_SIZE = 1000;

    private File _dir;
    private DatasetFileState _state;
    private MonitoredDir _monitoredDir;

    private final ExecutorService _pool = Executors.newCachedThreadPool();
    private final List<DatasetFileListener> _fileListeners;

    private final Map<String, DatasetFile> _lruCache;
    private final Map<String, DatasetLabel> _labelMap;


    public DatasetFileServiceImpl(File dir, DatasetFileState state) {
        if (dir == null) throw new NullPointerException("dir = null");
        if (state == null) throw new NullPointerException("state = null");
        _dir = dir;
        _state = state;
        _fileListeners = new ArrayList<DatasetFileListener>();

        // An LRU cache filename -> DatasetFile.
        int cap = (int) Math.round(CACHE_SIZE / 0.75) + 1;
        _lruCache = Collections.synchronizedMap(
            new LinkedHashMap<String, DatasetFile>(cap, (float)0.75, true) {
                protected boolean removeEldestEntry(Map.Entry<String, DatasetFile> me) {
                    return size() > CACHE_SIZE;
                }
            });

        _labelMap = Collections.synchronizedMap(new TreeMap<String, DatasetLabel>());
    }

    public File getDir() {
        return _dir;
    }

    public DatasetFile fetch(String filename)
            throws IOException, DatasetFileException, InterruptedException {

        DatasetFile res = _lruCache.get(filename);
        if (res != null) return res;

        File fitsFile = new File(_dir, filename);
        if (!fitsFile.exists()) return null;

        res = DatasetFileImpl.parse(fitsFile);
        if (res != null) {
            _lruCache.put(filename, res);
            _labelMap.put(filename, res.getDataset().getLabel());
        }
        return res;
    }

    public DatasetFile updateQaState(String filename, DatasetQaState newQaState)
            throws IOException, DatasetFileException, InterruptedException {
        return update(filename, new DatasetFileUpdateImpl(newQaState));
    }

    public DatasetFile updateRelease(String filename, Date releaseDate, Boolean headerPrivate)
            throws IOException, DatasetFileException, InterruptedException {
        return update(filename, new DatasetFileUpdateImpl(null, releaseDate, headerPrivate));
    }

    public DatasetFile update(String filename, DatasetFileUpdate updateTmpl)
            throws IOException, DatasetFileException, InterruptedException {

        DatasetFileImpl dset = (DatasetFileImpl) fetch(filename);
        if (dset == null) return null;

        Collection<HeaderItem> updates = new ArrayList<HeaderItem>(3);
        DatasetQaState newQaState = updateTmpl.getQaState();
        if ((newQaState != null) && (newQaState != dset.getQaState())) {
            LOG.info(String.format("Updating QA State of %s(%s) from %s to %s", dset.getLabel(), dset.getDatasetFileName(), dset.getQaState(), newQaState));
            updates.add(QaStateConverter.toRawGemQaItem(newQaState));
            updates.add(QaStateConverter.toRawPiReqItem(newQaState));
        }
        Date newRelease = updateTmpl.getRelease();
        if ((newRelease != null) && (!newRelease.equals(dset.getRelease()))) {
            updates.add(TimeConverter.toReleaseItem(newRelease));
        }
        Boolean newHeaderPrivate = updateTmpl.isHeaderPrivate();
        if ((newHeaderPrivate != null) && (newHeaderPrivate != dset.isHeaderPrivate())) {
            updates.add(ProprietaryMetadataConverter.toItem(newHeaderPrivate));
        }

        if (updates.size() == 0) return dset;

        Hedit hedit = new Hedit(dset.getFile());

        hedit.updatePrimary(updates);
        DatasetFile res = dset.apply(updateTmpl);
        _lruCache.put(filename, res);

        _expectUpdates(dset.getFile().getName());
        return res;
    }

    public void addFileListener(DatasetFileListener listener) {
        synchronized (_fileListeners) {
            _fileListeners.add(listener);
        }
    }

    public void removeFileListener(DatasetFileListener listener) {
        synchronized (_fileListeners) {
            _fileListeners.remove(listener);
        }
    }

    public int getFileListenerCount() {
        synchronized (_fileListeners) {
            return _fileListeners.size();
        }
    }

    private void _fireDatasetModified(DatasetFile dsetFile) {
        final DatasetFileEvent evt = new DatasetFileEventImpl(this, dsetFile);
        List<DatasetFileListener> listeners;
        synchronized (_fileListeners) {
            listeners = new ArrayList<DatasetFileListener>(_fileListeners);
        }
        for (final DatasetFileListener l : listeners) {
            _pool.submit(new Runnable() {
                public void run() {
                    l.datasetModified(evt);
                }
            });
        }
    }

    private void _fireDatasetAdded(DatasetFile dsetFile) {
        final DatasetFileEvent evt = new DatasetFileEventImpl(this, dsetFile);
        List<DatasetFileListener> listeners;
        synchronized (_fileListeners) {
            listeners = new ArrayList<DatasetFileListener>(_fileListeners);
        }
        for (final DatasetFileListener l : listeners) {
            _pool.submit(new Runnable() {
                public void run() {
                    l.datasetAdded(evt);
                }
            });
        }
    }

    private void _fireBadDataset(File file, DatasetLabel label, String message) {
        final DatasetFileEvent evt = new DatasetFileEventImpl(this, file, label, message);
        List<DatasetFileListener> listeners;
        synchronized (_fileListeners) {
            listeners = new ArrayList<DatasetFileListener>(_fileListeners);
        }
        for (final DatasetFileListener l : listeners) {
            _pool.submit(new Runnable() {
                public void run() {
                    l.datasetModified(evt);
                }
            });
        }
    }

    private void _fireDatasetDeleted(File file, DatasetLabel label) {

        String msg = file.getName() + " deleted";
        final DatasetFileEvent evt = new DatasetFileEventImpl(this, file, label, msg);
        List<DatasetFileListener> listeners;
        synchronized (_fileListeners) {
            listeners = new ArrayList<DatasetFileListener>(_fileListeners);
        }
        for (final DatasetFileListener l : listeners) {
            _pool.submit(new Runnable() {
                public void run() {
                    l.datasetDeleted(evt);
                }
            });
        }
    }


    public void init(MonitoredDir dir, long lastModified) {
        long since = _state.getLastModified();
        if ((since != -1) && (since < lastModified)) {
            Collection<File> files = dir.getModified(since, lastModified);
            for (File f : files) {
                _handleFile(f, false);
            }
        }
        _state.increaseLastModified(lastModified);
        _setMonitoredDir(dir);
    }

    private synchronized void _setMonitoredDir(MonitoredDir monDir) {
        _monitoredDir = monDir;
    }

    private synchronized MonitoredDir _getMonitoredDir() {
        return _monitoredDir;
    }

    private void _expectUpdates(String filename) {
        MonitoredDir monDir = _getMonitoredDir();
        if (monDir == null) return;

//        try {
            monDir.expectUpdates(filename);
//        } catch (RemoteException ex) {
            // this was only an optimization so just log a warning
//            LOG.log(Level.WARNING, "Remote exception calling monitored directory", ex);
//        }
    }

    private void _expectUpdates(Collection<String> files) {
        MonitoredDir monDir = _getMonitoredDir();
        if (monDir == null) return;

//        try {
            monDir.expectUpdates(files);
//        } catch (RemoteException ex) {
            // this was only an optimization so just log a warning
//            LOG.log(Level.WARNING, "Remote exception calling monitored directory", ex);
//        }
    }

    private Collection<String> _guessNextDatasets(Collection<File> newFiles) {
        String lastName = "";
        for (File f : newFiles) {
            String curName = f.getName();
            if (curName.compareTo(lastName) > 0) {
                lastName = curName;
            }
        }
        DatasetFileName dfn = new DatasetFileName(lastName);
        if (!dfn.isValid()) return null;

        Collection<String> res = new ArrayList<String>(5);
        for (int i=0; i<5; ++i) {
            dfn = dfn.getNext();
            res.add(dfn.toString());
        }
        return res;
    }

    private static final long ONE_WEEK = 1000 * 60 * 60 * 24 * 7;

    public void dirModified(DirEvent evt) {
        long lastMod = _state.getLastModified();
        lastMod = lastMod - ONE_WEEK;

        Collection<File> files = evt.getNewFiles();
        Collection<String> next = _guessNextDatasets(files);
        if (next != null) _expectUpdates(next);
        for (File f : files) {
            // filter out "new" files that are really old
            if (f.lastModified() < lastMod) continue;

            _handleFile(f, true);
        }

        files = evt.getModifiedFiles();
        for (File f : files) _handleFile(f, false);

        files = evt.getDeletedFiles();
        for (File f : files) _handleRemovedFile(f);

        _state.increaseLastModified(evt.getLastModified());
    }

    private void _handleFile(File f, boolean isNew) {
        String fname = f.getName();
        if (!fname.endsWith(".fits")) return;
        try {

            DatasetFile dsetfile = DatasetFileImpl.parse(f);
            _lruCache.put(fname, dsetfile);
            _labelMap.put(fname, dsetfile.getDataset().getLabel());
            if (isNew) {
                _fireDatasetAdded(dsetfile);
            } else {
                _fireDatasetModified(dsetfile);
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (DatasetFileException ex) {
            LOG.info(ex.getMessage());
            DatasetLabel label = _labelMap.get(f.getName());
            _fireBadDataset(f, label, ex.getMessage());
        } catch (InterruptedException ex) {
            LOG.log(Level.WARNING, "Interrupted while locking " + f, ex);
        }
    }

    private void _handleRemovedFile(File f) {
        String fname = f.getName();
        if (!fname.endsWith(".fits")) return;
        _lruCache.remove(fname);
        DatasetLabel label = _labelMap.remove(fname);
        if (label == null) return;
        _fireDatasetDeleted(f, label);
    }
}
