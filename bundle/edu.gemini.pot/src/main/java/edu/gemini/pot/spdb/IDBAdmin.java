// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: IDBAdmin.java 46832 2012-07-19 00:28:38Z rnorris $
//

package edu.gemini.pot.spdb;





/**
 * This is the database administration interface.  It contains routines
 * that control or tune the database but that are not customarily used by
 * ordinary clients.
 */
public interface IDBAdmin {

    /**
     * Gets the storage interval (in ms), which determines how often
     * modified programs are saved to disk.
     */
    long getStorageInterval() ;

    /**
     * Sets the storage interval (in ms), which determines how often
     * modified programs are saved to disk.  If the interval is set too
     * low, performance can be impacted since programs being actively
     * modified must be locked to store them.  If the interval is set too
     * high, then the potential for lost changes is greater in the event
     * of a crash.
     *
     * @param interval period in ms between checking for and storing
     *                 modifications
     * @throws IllegalArgumentException if <code>interval</code> is 0 or less
     */
    void setStorageInterval(long interval) throws IllegalArgumentException;

    /**
     * Shuts down the database in an orderly manner.  If this method is not
     * used to stop the database server then some unsaved modifications could
     * be lost.
     */
    void shutdown() ;

    void stopDb() ;
    
    /**
     * This method returns a DBStatus object containing all the information related
     * to the ODB status, updated when the call is executed.
     *
     * @return a DBStatus Object
     * @
     */
    DBStatus getStatus() ;

    /**
     * Returns the total storage size, on disk, or zero for transient databases.
     * @return total disk storage in bytes, or zero for transient databases
     */
    long getTotalStorage();

}