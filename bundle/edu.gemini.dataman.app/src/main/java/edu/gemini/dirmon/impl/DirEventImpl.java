//
// $Id: DirEventImpl.java 205 2005-10-16 22:54:01Z shane $
//

package edu.gemini.dirmon.impl;

import edu.gemini.dirmon.DirEvent;
import edu.gemini.dirmon.MonitoredDir;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.EventObject;

/**
 *
 */
class DirEventImpl extends EventObject implements DirEvent, Serializable {

    private MonitoredDirImpl _src;
    private DirScanResults _scan;

    DirEventImpl(MonitoredDirImpl src, DirScanResults scan) {
        super(src);
        _src  = src;
        _scan = scan;
    }

    public long getLastModified() {
        return _scan.getLastModified();
    }

    public MonitoredDir getMonitoredDir() {
        return _src;
    }

    public Collection<File> getNewFiles() {
        return _scan.getNewFiles();
    }

    public Collection<File> getModifiedFiles() {
        return _scan.getModifiedFiles();
    }

    public Collection<File> getDeletedFiles() {
        return _scan.getDeletedFiles();
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("DirEvent [loc=");
        buf.append(_src.getDirLocation());
        buf.append(", deleted=[").append(getDeletedFiles());
        buf.append("], modified=[").append(getModifiedFiles());
        buf.append("], new=[").append(getNewFiles());
        buf.append("]]");
        return buf.toString();
    }
}
