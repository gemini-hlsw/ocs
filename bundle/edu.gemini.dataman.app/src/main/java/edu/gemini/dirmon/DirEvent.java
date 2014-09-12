//
// $Id: DirEvent.java 52 2005-08-29 13:45:20Z shane $
//

package edu.gemini.dirmon;

import java.io.File;
import java.util.Collection;

/**
 * The DirEvent interface defines the events passed to a DirListener
 * implementation when a modification is made to the contents of a monitored
 * directory.
 */
public interface DirEvent {

    /**
     * Gets the last modified time of the directory at the time the event was
     * fired.
     */
    long getLastModified();

    /**
     * Gets the MonitoredDir to which the event applies (the event source).
     */
    MonitoredDir getMonitoredDir();

    /**
     * Gets the collection of new files that have been added to the directory,
     * if any.
     *
     * @return new files in the directory, or an empty collection if there
     * are none
     */
    Collection<File> getNewFiles();

    /**
     * Gets the collection of files that have been updated, if any.  These
     * existed already but have been modified.
     *
     * @return updated files in the directory, or an empty collection if there
     * are none
     */
    Collection<File> getModifiedFiles();

    /**
     * Gets the collection of files that have been removed from the directory,
     * if any.
     *
     * @return files that were deleted from the directory, or an empty
     * collection if there are none
     */
    Collection<File> getDeletedFiles();
}
