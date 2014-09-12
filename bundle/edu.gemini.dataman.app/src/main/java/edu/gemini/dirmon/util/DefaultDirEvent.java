//
// $Id: DefaultDirEvent.java 190 2005-10-10 20:25:13Z shane $
//

package edu.gemini.dirmon.util;

import edu.gemini.dirmon.DirEvent;
import edu.gemini.dirmon.MonitoredDir;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.ArrayList;


/**
 * A simple implementation of the DirEvent interface.
 */
public class DefaultDirEvent extends EventObject implements DirEvent {
    private MonitoredDir _dir;
    private long _lastModified;
    private Collection<File> _newFiles;
    private Collection<File> _updFiles;
    private Collection<File> _delFiles;

    public DefaultDirEvent(MonitoredDir src, long lastModified,
                           Collection<File> newFiles, Collection<File> updFiles, Collection<File> delFiles) {
        super(src);
        _dir = src;
        _lastModified = lastModified;

        if (newFiles == null) {
            _newFiles = Collections.emptyList();
        } else {
            _newFiles = Collections.unmodifiableCollection(new ArrayList<File>(newFiles));
        }

        if (updFiles == null) {
            _updFiles = Collections.emptyList();
        } else {
            _updFiles = Collections.unmodifiableCollection(new ArrayList<File>(updFiles));
        }

        if (delFiles == null) {
            _delFiles = Collections.emptyList();
        } else {
            _delFiles = Collections.unmodifiableCollection(new ArrayList<File>(delFiles));
        }
    }

    public MonitoredDir getMonitoredDir() {
        // not sure why this doesn't work
        //return (MonitoredDir) getSource();
        return _dir;
    }

    public long getLastModified() {
        return _lastModified;
    }

    public Collection<File> getNewFiles() {
        return _newFiles;
    }

    public Collection<File> getModifiedFiles() {
        return _updFiles;
    }

    public Collection<File> getDeletedFiles() {
        return _delFiles;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("DirEvent [loc=");
        buf.append(_dir.getDirLocation());
        buf.append(", deleted=[").append(_delFiles);
        buf.append("], modified=[").append(_updFiles);
        buf.append("], new=[").append(_newFiles);
        buf.append("]]");
        return buf.toString();
    }
}
