//
// $Id: MonitoredDirConfig.java 229 2005-11-02 21:41:52Z shane $
//

package edu.gemini.dirmon.impl;

import java.io.FileFilter;

/**
 * Configuration properties for MonitoredDir.
 */
public interface MonitoredDirConfig {
    long DEFAULT_FULL_DIR_POLL_PERIOD_MS   = 60 * 1000;
    long DEFAULT_ACTIVE_SET_POLL_PERIOD_MS =  5 * 1000;
    int  DEFAULT_ACTIVE_SET_SIZE           = 100;

    /**
     * A default MonitoredDirConfig.
     */
    MonitoredDirConfig DEFAULT = new MonitoredDirConfig() {
        private CompletionPolicy _policy = new IdleTimeCompletionPolicy();

        public long getFullDirPollPeriod() {
            return DEFAULT_FULL_DIR_POLL_PERIOD_MS;
        }

        public long getActiveSetPollPeriod() {
            return DEFAULT_ACTIVE_SET_POLL_PERIOD_MS;
        }

        public int getActiveSetSize() {
            return DEFAULT_ACTIVE_SET_SIZE;
        }

        public CompletionPolicy getCompletionPolicy() {
            return _policy;
        }

        public FileFilter getFileFilter() {
            return null;
        }
    };

    /**
     * Gets the time betweeen full scans of the directory.  This involves a
     * complete listing of all the files, which typically will take longer than
     * a check of the "active set" subset of files in the directory.
     *
     * @return time (in ms) to wait between full scans of the directory
     */
    long getFullDirPollPeriod();

    /**
     * Gets the time between scans of the active set of files associated with a
     * MonitoredDir.  The active set is checked more frequently than full
     * directory scans because they are the files that are believed to be
     * changing.
     *
     * @return time (in ms) to wait between scans of the "active set" of files
     */
    long getActiveSetPollPeriod();

    /**
     * Gets the size of the "active set" of files that are believed to be the
     * most likely to be modified, created, or deleted in the near future.  The
     * larger the active set, the more timely notifications are likely to be,
     * but the amount of I/O required to check the files.
     */
    int getActiveSetSize();

    /**
     * Gets the policy to use to determine whether a file is judged "complete",
     * or no longer being written to.  This is awful, and file locks are used
     * by this implementation, but they are not required by the OS.
     */
    CompletionPolicy getCompletionPolicy();

    /**
     * Gets the file filter to use, if any, when considering files in the
     * monitored directory.
     *
     * @return FileFilter to apply or <code>null</code> if all files should
     * be considered
     */
    FileFilter getFileFilter();
}
