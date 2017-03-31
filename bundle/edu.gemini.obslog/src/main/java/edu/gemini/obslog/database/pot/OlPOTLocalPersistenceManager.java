package edu.gemini.obslog.database.pot;

import edu.gemini.obslog.database.OlPersistenceException;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.spdb.IDBDatabaseService;

import java.util.logging.Logger;

public class OlPOTLocalPersistenceManager extends OlPOTPersistenceManager implements OlPersistenceManager {
    private static final Logger LOG = Logger.getLogger(OlPOTLocalPersistenceManager.class.getName());

    private IDBDatabaseService _database;

    /**
     * Return the IDBDatabase in use by this session
     *
     * @return IDBDatabaseService instance or null if nothing has been set.
     * @throws edu.gemini.obslog.database.OlPersistenceException
     *          if locating the database fails
     */
    public IDBDatabaseService getDatabase() throws OlPersistenceException {
        synchronized (this) {
            if (_database != null) return _database;
        }

        IDBDatabaseService db = SPDB.get();
        if (db == null) {
            // Logging in the  concreate class
            return null;
        }

        synchronized (this) {
            _database = db;
            return _database;
        }
    }

    /**
     * Return info on this persistence manager.
     *
     * @return info string
     */
    public String info() {
        return "POT Local Persistence Manager";
    }

}

