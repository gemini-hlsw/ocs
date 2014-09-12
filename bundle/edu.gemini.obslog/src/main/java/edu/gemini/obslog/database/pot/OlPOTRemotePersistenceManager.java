package edu.gemini.obslog.database.pot;

import edu.gemini.obslog.database.OlPersistenceException;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.spdb.IDBDatabaseService;

import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: OlPOTRemotePersistenceManager.java,v 1.4 2005/12/11 15:54:15 gillies Exp $
//

public class OlPOTRemotePersistenceManager extends OlPOTPersistenceManager implements OlPersistenceManager {
    private static final Logger LOG = Logger.getLogger(OlPOTRemotePersistenceManager.class.getName());

     /**
     * Default amount of time waiting to locate the SPDB before giving up (in ms);
     */
    public static final long DEFAULT_TIMEOUT = 100000;

    private IDBDatabaseService _database;

    /**
     * Provide the manager with the proper local or remote
     * database.  Either local or remote can be used at one time, but
     * not both at the same time.
     * <p/>
     * Note: to set the location of the local database before calling _getDatabase, the
     * user should set the ot.spdb.dir property which is used by the
     * <code>{@link edu.gemini.pot.client.SPDBAccess#getLocalDatabase}</code> method.
     * <p/>
     * All classes should get the database from the DBManager.
     */
    private IDBDatabaseService setupDatabase(long timeout) {

        LOG.info("Starting remote database fetch.");

        IDBDatabaseService db = SPDB.get();

        LOG.info("Created a remote database reference with DBDatabaseSmartRef.");
        return db;
    }

    /**
       * Return the IDBDatabase in use by this session
       * @return IDBDatabaseService instance or null if nothing has been set.
       * @throws edu.gemini.obslog.database.OlPersistenceException if locating the database fails
       */
      public IDBDatabaseService getDatabase() throws OlPersistenceException {
          synchronized (this) {
              if (_database != null) return _database;
          }

          IDBDatabaseService db = setupDatabase(DEFAULT_TIMEOUT);
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
        return "POT Remote Persistence Manager";
    }

}

