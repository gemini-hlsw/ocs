/* Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file LICENSE for complete details.
 *
 * $Id: SPDB.java 46996 2012-07-26 14:21:12Z swalker $
 */
package edu.gemini.pot.client;

import edu.gemini.pot.spdb.IDBDatabaseService;

import java.io.File;
import java.util.logging.Logger;


/**
 * Static accessors for a single local database.
 */
public final class SPDB {

    private static final Logger LOG = Logger.getLogger(SPDB.class.getName());
    private static IDBDatabaseService db;

    /**
     * Filesystem path of is either the value
     * of system property <code>ot.spdb.dir</code> (if present) or the platform-specific equivalent of
     * <code>~/.jsky/spdb</code> otherwise.
     */
    public static final File DB_DEFAULT;

    static {
        String dirName = System.getProperty("ot.spdb.dir");
        if (dirName != null) {
            DB_DEFAULT = new File(dirName);
        } else {
            String home = System.getProperty("user.home");
            if (home == null) home = ""; // ?
            DB_DEFAULT = new File(home + File.separator + ".jsky" + File.separator + "spdb");
        }
    }

    /**
     * Returns the current database, which must have been previously registered
     * with a call to {@link #init}
     *
     * @throws IllegalStateException if database not set by previous call to
     * {@link #init}
     */
    public static synchronized IDBDatabaseService get() {
        if (db == null) {
            LOG.severe("Database requested without prior initializaton.");
            throw new IllegalStateException("Database requested without prior initialization.");
        }
        return db;
    }

    /**
     * Initializes the database to the given instance if no database has yet been initialized.
     * @throws IllegalStateException if the database has already been initialized
     */
    public static synchronized void init(IDBDatabaseService db) {
        if (SPDB.db != null) throw new IllegalStateException("Database already set");
        SPDB.db = db;
    }

    public static synchronized void clear() {
        SPDB.db = null;
    }

    /**
     * Creates and returns a database at <code>path</code> if no database has yet been initialized.
     *
     * @throws IllegalStateException if the database has already been initialized
     * @throws RuntimeException      wrapping internal problems if initialization fails
     */
    /*
    public static synchronized IDBDatabaseService init(File path) {
        try {
            if (db != null)
                throw new IllegalStateException("A database has already been initialized.");
            LOG.info("Opening SPDB at " + path);
            db = DBLocalDatabase.create(path);
            return db;
        } catch (IOException dbioe) {
            throw new RuntimeException(dbioe);
        }
    }
    */

}
