// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DBAdmin.java 46971 2012-07-25 16:59:17Z swalker $
//

package edu.gemini.pot.spdb;

import java.io.Serializable;



/**
 * This class provides the implemention of the <code>IDBAdmin</code>
 * interface.
 */
public class DBAdmin implements IDBAdmin {
    /**
     * The file to which state is saved.
     */
    private static final String STATE_FILE = "admin.ser";

    private static class State implements Serializable {
        long storageInterval;

        State(long storageInterval) {
            this.storageInterval = storageInterval;
        }
    }

    private DatabaseManager _dataMan;
    private State _state;

    /**
     * Constructs with the program and storage managers to administer.
     */
    DBAdmin(DatabaseManager dm) {
        _dataMan = dm;
        _state = new State(_dataMan.getProgramStorageManager().getStorageInterval());
        _setStorageInterval(_state.storageInterval);
    }

    /**
     * Checkpoints the state.
     */
    private void _checkpoint() {
//        try {
//            _dataMan.getFileManager().storeObject(_state, STATE_FILE);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }

    /**
     * Gets the storage interval.
     */
    public synchronized long getStorageInterval() {
        return _state.storageInterval;
        //return _dataMan.getStorageManager().getStorageInterval();
    }

    /**
     * Sets the storage interval w/o modifying state and checkpointing.
     */
    private void _setStorageInterval(long interval) throws IllegalArgumentException {
        _dataMan.getPlanStorageManager().setStorageInterval(interval);
        _dataMan.getProgramStorageManager().setStorageInterval(interval);
    }

    /**
     * Sets the storage interval.
     */
    public synchronized void setStorageInterval(long interval) throws IllegalArgumentException {
        _setStorageInterval(interval);
        _state.storageInterval = interval;
        _checkpoint();
    }

    /**
     * Shuts down the database, storing any outstanding modifications.
     */
    public void shutdown() {
        stopDb();
    }

    public void stopDb() {
        _dataMan.shutdown();
    }

    /**
     * Get Status information for the database
     *
     * @return DBStatus object, updated with the most recent data
     */
    public DBStatus getStatus()  {
        return DBStatus.getStatus(this);
    }

    /**
     * Returns the total storage size, on disk, or zero for transient databases.
     */
    public long getTotalStorage() {
        return _dataMan.getTotalStorage();
    }

}
