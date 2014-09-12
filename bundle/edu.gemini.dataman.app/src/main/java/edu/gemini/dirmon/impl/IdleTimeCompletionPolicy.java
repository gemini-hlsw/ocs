//
// $Id: IdleTimeCompletionPolicy.java 120 2005-09-12 21:39:09Z shane $
//

package edu.gemini.dirmon.impl;

import java.io.File;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 */
public class IdleTimeCompletionPolicy implements CompletionPolicy {
    private static final Logger LOG = Logger.getLogger(IdleTimeCompletionPolicy.class.getName());

    // Maximum time that a writer of a file will remain idle between updating
    // the file's content.
    public static final long DEFAULT_MAX_WRITE_IDLE_TIME = 2000;

    private long _maxWriteIdleTime = DEFAULT_MAX_WRITE_IDLE_TIME;

    public IdleTimeCompletionPolicy() {
    }

    public IdleTimeCompletionPolicy(long maxWriteIdleTime) {
        _maxWriteIdleTime = maxWriteIdleTime;
    }

    public synchronized long getMaxWriteIdleTime() {
        return _maxWriteIdleTime;
    }

    public synchronized void setMaxWriteIdleTime(long maxWriteIdleTime) {
        _maxWriteIdleTime = maxWriteIdleTime;
    }

    public boolean isComplete(File f) {
        long modTime  = f.lastModified();
        long curTime  = System.currentTimeMillis();
        long idleTime = curTime - modTime;

        return isComplete(f, idleTime);
    }

    protected boolean isComplete(File f, long idleTime) {
        // If the file has been modified more recently than MAX_WRITE_IDLE_TIME
        // milliseconds ago, then return false -- not complete.
        if (idleTime < getMaxWriteIdleTime()) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(f.getName() + " is only " + idleTime + " ms old.");
            }
            return false;
        }

        return true;
    }
}
