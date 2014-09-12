// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DatabaseManager.java 47005 2012-07-26 22:35:47Z swalker $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.util.POTUtil;

import java.io.IOException;
import java.util.UUID;


/**
 * The database manager is an implementation class that creates and
 * provides access to the other "manager" implementation objects such
 * as the file, program, and storage managers.
 */
final class DatabaseManager {
    private final IDBPersister _persister;
    private final ProgramManager<ISPProgram> _progMan;
    private final ProgramManager<ISPNightlyRecord> _planMan;
    private final StorageManager<ISPProgram> _progStoreMan;
    private final StorageManager<ISPNightlyRecord> _planStoreMan;

    //private DBAdmin _admin;
    private final ISPFactory _fact;

    final FunctorLogger functorLogger;

    /**
     * Constructs with the database directory to use.
     *
     * @throws IOException if the database directory is not readable and
     * writable (or creatable if not existent)
     */
    DatabaseManager(IDBPersister persister, UUID uuid) throws IOException {
        this.functorLogger = new FunctorLogger();

        // Create the file manager and load the programs in the database.
        _persister = persister;

        // Give the programs to the program manager.  It will keep track of
        // them and provide access to them.
        _progMan = new ProgramManager<ISPProgram>(_persister.reloadPrograms());
        _planMan = new ProgramManager<ISPNightlyRecord>(_persister.reloadPlans());

        // Create the storage manager to keep the program files up-to-date
        // as they change and to store/remove programs as necessary.
        _progStoreMan = new StorageManager<ISPProgram>(_progMan, _persister);
        _planStoreMan = new StorageManager<ISPNightlyRecord>(_planMan, _persister);

        _fact = POTUtil.createFactory(uuid);
    }

    /**
     * Gets the factory used to create new program nodes.
     */
    public ISPFactory getFactory() {
        return _fact;
    }


    /**
     * Obtains a reference to the <code>FileManager</code>.
     */
    IDBPersister getPersister() {
        return _persister;
    }

    /**
     * Obtains a reference to the <code>ProgramManager</code>.
     */
    ProgramManager<ISPProgram> getProgramManager() {
        return _progMan;
    }

    /**
     * Obtains a reference to the <code>NightlyPlanManager</code>
     */
    ProgramManager<ISPNightlyRecord> getNightlyPlanManager() {
        return _planMan;
    }

    /**
     * Obtains a reference to the <code>StorageManager</code>.
     */
    StorageManager<ISPProgram> getProgramStorageManager() {
        return _progStoreMan;
    }

    /**
     * Obtains a reference to the <code>StorageManager</code>.
     */
    StorageManager<ISPNightlyRecord> getPlanStorageManager() {
        return _planStoreMan;
    }

    /**
     * Shuts down the database, storing any outstanding modifications.
     */
    void shutdown() {
        _progStoreMan.shutdown();
        _planStoreMan.shutdown();
        _progMan.shutdown();
        _planMan.shutdown();
        functorLogger.cancel();
    }

    /**
     * Returns the total storage size, on disk, or zero for transient databases.
     */
    long getTotalStorage() {
        return _persister.getTotalStorage();
    }
}