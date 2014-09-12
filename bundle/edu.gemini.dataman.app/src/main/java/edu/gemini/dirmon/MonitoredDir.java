//
// $Id: MonitoredDir.java 192 2005-10-11 16:42:32Z shane $
//

package edu.gemini.dirmon;

import java.io.File;
import java.util.Collection;

/**
 * A monitored directory.  Each monitored directory instance exists because
 * one or more {@link DirListener}s are watching it.
 */
public interface MonitoredDir {
    /**
     * Describes the directory being monitored.
     */
    DirLocation getDirLocation() ;

    /**
     * Gets the last time that the directory was modified.  In other words,
     * the last time that a file was added, removed, or updated.
     */
    long getLastModified() ;

    /**
     * Gets the set of files that currently exist in the directory.
     */
    Collection<File> getCurrentFiles() ;

    /**
     * Gets the set of files that were last modified or created in the
     * given time range.  The idea of this method is to provide a convenient
     * mechanism whereby a client can get all modifications that have occured
     * during a time span (presumably while it was not listening for changes).
     * For example, it can be called from the {@link DirListener#init} method
     * to get all the updates that occured while the client was down or away.
     *
     * @param sinceAfter start of the time range (exclusive)
     * @param through end of the time range (inclusive)
     */
    Collection<File> getModified(long sinceAfter, long through) ;

    /**
     * Provides a hint to the MonitoredDirectory that the given file might
     * be updated soon.  The implementation can use this hint to provide more
     * timely update information should the file in fact be added to the
     * directory or updated.
     *
     * <p>There is no requirement to call this method for proper behavior.  It
     * is simply an optimization.
     *
     * @param filename the file that is expected to be added, updated, or
     * deleted
     */
    void expectUpdates(String filename) ;

    /**
     * Provides a hint to the MonitoredDirectory that the given files might
     * be updated soon.  The implementation can use this hint to provide more
     * timely update information should any of the files in fact be added to
     * the directory or updated.
     *
     * <p>There is no requirement to call this method for proper behavior.  It
     * is simply an optimization.
     *
     * @param filenames the collection of files that are expected to be added,
     * updated, or deleted
     */
    void expectUpdates(Collection<String> filenames) ;
}
