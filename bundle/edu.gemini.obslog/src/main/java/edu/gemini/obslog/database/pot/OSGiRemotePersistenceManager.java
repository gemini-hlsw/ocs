package edu.gemini.obslog.database.pot;

import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.pot.spdb.IDBDatabaseService;

/**
 * Gemini Observatory/AURA
 * $Id$
 */
public class OSGiRemotePersistenceManager extends OlPOTPersistenceManager implements OlPersistenceManager {
    private IDBDatabaseService _database;

    public synchronized IDBDatabaseService getDatabase() {
        return _database;
    }

    public synchronized void setDatabase(IDBDatabaseService db) {
        _database = db;
    }

    /**
     * Abstract method to allow subclasses to print some ID info.
     *
     * @return info string
     */
    public String info() {
        return "OSGi provided database service";
    }
}
