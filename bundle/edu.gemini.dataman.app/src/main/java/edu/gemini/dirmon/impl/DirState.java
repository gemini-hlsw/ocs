//
// $Id: DirState.java 190 2005-10-10 20:25:13Z shane $
//

package edu.gemini.dirmon.impl;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

/**
 * Recorded file state of the directory (files that are present and the last
 * known modification times).
 */
final class DirState {

    static final class FileRecord {
        private final File _file;
        private long _lastUpdate;

        FileRecord(File f) {
            _file       = f;
            _lastUpdate = f.lastModified();
        }

        File getFile() {
            return _file;
        }

        synchronized long getLastUpdate() {
            return _lastUpdate;
        }

        synchronized long setLastUpdate() {
            _lastUpdate = _file.lastModified();
            return _lastUpdate;
        }
    }


    private Map<String, FileRecord> _fileMap = new TreeMap<String, FileRecord>();
    private long _latestModTime;

    private final File _dir;
    private final CompletionPolicy _compPolicy;
    private final FileFilter _filter;


    DirState(File dir, CompletionPolicy policy, FileFilter filter) {
        _dir        = dir;
        _compPolicy = policy;
        _filter     = filter;
        _latestModTime = _dir.lastModified();
    }

    synchronized long getLatestModTime() {
        return _latestModTime;
    }

    synchronized List<File> getAllFiles() {
        List<File> res = new ArrayList<File>(_fileMap.size());
        for (FileRecord rec : _fileMap.values()) {
            File f = rec.getFile();
            res.add(f);
        }
        return res;
    }

    synchronized List<File> getAllFiles(long startTime, long endTime) {
        List<File> res = new ArrayList<File>();
        for (FileRecord rec : _fileMap.values()) {
            File f = rec.getFile();
            long modTime = f.lastModified();
            if ((modTime > startTime) && (modTime <= endTime)) res.add(f);
        }
        return res;
    }

    private synchronized FileRecord _lookupRecord(File f) {
        return _fileMap.get(f.getName());
    }

    // Updates the state in the _fileMap, and returns a new DirScanResults
    // based upon the given one, and the state of the _fileMap with the lock
    // held.  The idea is to avoid double notification of events if more than
    // one scan is happening at the same time in different threads.
    private synchronized DirScanResults _updateState(Set<File> newFiles,
                                                     Set<File> updFiles,
                                                     Set<File> delFiles) {
        long latestScanMod = -1;

        // Add new files.
        if (newFiles != null) {
            for (Iterator<File> it=newFiles.iterator(); it.hasNext(); ) {
                File f = it.next();

                if (_fileMap.containsKey(f.getName())) {
                    it.remove();
                    continue; // already added by a different scan
                }

                FileRecord rec = new FileRecord(f);
                long updTime = rec.getLastUpdate();
                if (latestScanMod < updTime) latestScanMod = updTime;
                _fileMap.put(f.getName(), rec);
            }
        }

        // Update updated files.
        if (updFiles != null) {
            for (Iterator<File> it=updFiles.iterator(); it.hasNext(); ) {
                File f = it.next();

                FileRecord rec = _fileMap.get(f.getName());
                long oldUpdateTime = rec.getLastUpdate();
                long updTime = rec.setLastUpdate();
                if (updTime == oldUpdateTime) {
                    it.remove();
                    continue; // already updated by a different scan
                }
                if (latestScanMod < updTime) latestScanMod = updTime;
            }
        }

        // Delete deleted files.
        if (delFiles != null) {
            for (Iterator<File> it=delFiles.iterator(); it.hasNext(); ) {
                File f = it.next();

                if (_fileMap.remove(f.getName()) == null) {
                    it.remove(); // already updated by a different scan
                }
            }
        }

        if (_latestModTime < latestScanMod) _latestModTime = latestScanMod;
        return new DirScanResults(_latestModTime, newFiles, updFiles, delFiles);
    }

    DirScanResults doFullScan() {

        Set<File> newFiles = null;
        Set<File> updFiles = null;

        File[] allFiles = _dir.listFiles(_filter);
        if (allFiles != null) {
            for (File f : allFiles) {
                FileRecord rec = _lookupRecord(f);
                if (rec == null) {
                    // This is a new file.  If it is complete, then add it to
                    // the newFiles set.  If not, then it will be found again
                    // during the next scan.
                    if (_compPolicy.isComplete(f)) {
                        if (newFiles == null) newFiles = new TreeSet<File>();
                        newFiles.add(f);
                    }
                } else {
                    // This file has been seen before.  See if it has been
                    // updated and is complete.  If updated and not complete,
                    // we'll find it in a future scan.
                    long modTime = f.lastModified();
                    if ((modTime > rec.getLastUpdate()) && _compPolicy.isComplete(f)) {
                        if (updFiles == null) updFiles = new TreeSet<File>();
                        updFiles.add(f);
                    }
                }
            }
        }

        // Check for deleted files.
        Set<File> delFiles = null;
        List<FileRecord> recList;
        synchronized (this) {
            recList = new ArrayList<FileRecord>(_fileMap.values());
        }
        for (FileRecord rec : recList) {
            File f = rec.getFile();
            if (!f.exists()) {
                if (delFiles == null) delFiles = new TreeSet<File>();
                delFiles.add(f);
            }
        }

        // Remove any new or updated fiels that were deleted during the scan.
        if (delFiles != null) {
            if (newFiles != null) newFiles.removeAll(delFiles);
            if (updFiles != null) updFiles.removeAll(delFiles);
        }

        // Update the state to match what we found.
        if ((newFiles == null) && (updFiles == null) && (delFiles == null)) {
            return null;
        }
        return _updateState(newFiles, updFiles, delFiles);
    }

    DirScanResults doPartialScan(Collection<File> files) {
        if ((files == null) || (files.size() == 0)) {
            return null;
        }

        Set<File> newFiles = null;
        Set<File> updFiles = null;
        Set<File> delFiles = null;

        for (File f : files) {
            FileRecord rec = _lookupRecord(f);
            if (rec == null) {
                if (f.exists() && _compPolicy.isComplete(f)) {
                    if (newFiles == null) newFiles = new TreeSet<File>();
                    newFiles.add(f);
                }
            } else {
                if (!f.exists()) {
                    if (delFiles == null) delFiles = new TreeSet<File>();
                    delFiles.add(f);
                } else {
                    long modTime = f.lastModified();
                    if ((modTime > rec.getLastUpdate()) && _compPolicy.isComplete(f)) {
                        if (updFiles == null) updFiles = new TreeSet<File>();
                        updFiles.add(f);
                    }
                }
            }
        }

        // Update the state to match what we found.
        if ((newFiles == null) && (updFiles == null) && (delFiles == null)) {
            return null;
        }
        return _updateState(newFiles, updFiles, delFiles);
    }
}
