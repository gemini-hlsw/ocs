//
// $Id: DirScanResults.java 190 2005-10-10 20:25:13Z shane $
//

package edu.gemini.dirmon.impl;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Holds the results of a single scan of the directory.
 */
final class DirScanResults implements Serializable {

    private long _lastModTime;
    private Set<File> _newFiles = new TreeSet<File>();
    private Set<File> _updFiles = new TreeSet<File>();
    private Set<File> _delFiles = new TreeSet<File>();

    DirScanResults(long lastModTime, Set<File> newFiles, Set<File> updFiles, Set<File> delFiles) {
        _lastModTime = lastModTime;
        if (newFiles == null) {
            _newFiles = Collections.emptySet();
        } else {
            _newFiles = Collections.unmodifiableSet(newFiles);
        }
        if (updFiles == null) {
            _updFiles = Collections.emptySet();
        } else {
            _updFiles = Collections.unmodifiableSet(updFiles);
        }
        if (delFiles == null) {
            _delFiles = Collections.emptySet();
        } else {
            _delFiles = Collections.unmodifiableSet(delFiles);
        }
    }

    long getLastModified() {
        return _lastModTime;
    }

    Set<File> getNewFiles() {
        return _newFiles;
    }

    Set<File> getModifiedFiles() {
        return _updFiles;
    }

    Set<File> getDeletedFiles() {
        return _delFiles;
    }

    boolean isEmpty() {
        return (_newFiles.size() == 0) && (_updFiles.size() == 0) &&
               (_delFiles.size() == 0);
    }
}
