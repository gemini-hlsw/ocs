// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DBLocalDatabase.java 47016 2012-07-27 14:23:50Z swalker $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.core.OcsVersionUtil;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Version;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Collections;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class provides the implementation of the <code>IDBDatabase</code>
 * interface.  It is used to implement the (remote) Science Program database
 * service, or may be instantiated and used "locally" by a client.
 */
public final class DBLocalDatabase implements IDBDatabaseService {

    private static final Logger LOG = Logger.getLogger(DBLocalDatabase.class.getName());

    public static IDBDatabaseService create(final File dbRootDir) throws IOException {
        final File dbDir = getVersionedDatabaseDir(dbRootDir);
        initDbDir(dbDir);
        return new DBLocalDatabase(loadUuid(dbRootDir), new FileManager(dbDir));
    }

    public static IDBDatabaseService createTransient() {
        try {
            return new DBLocalDatabase(UUID.randomUUID(), DoNothingPersister.INSTANCE);
        } catch (IOException ex) {
            // not loading anything from disk so this cannot happen
            LOG.log(Level.SEVERE, "IO exception with transient db", ex);
            throw new RuntimeException(ex);
        }
    }

    public static File getVersionedDatabaseDir(File dbRootDir) {
        return OcsVersionUtil.getVersionDir(dbRootDir, Version.current);
    }

    private static void initDbDir(final File dbDir) throws IOException {
        if (dbDir.exists()) {
            if (!dbDir.isDirectory()) throw new IOException("Not a directory: " + dbDir.getAbsolutePath());
        } else {
            if (!dbDir.mkdirs()) throw new IOException("Could not create directory: " + dbDir.getAbsolutePath());
        }
    }

    private static UUID loadUuid(File dbRootDir) {
        // Set the UUID, creating it the first time and storing it into the
        // database directory.
        final UuidIo io = new UuidIo(dbRootDir);
        if (io.exists()) {
            return io.load();
        } else {
            UUID uuid = UUID.randomUUID();
            io.store(uuid);
            return uuid;
        }
    }

    private final DatabaseManager _dataMan;
    private final DBAdmin _admin;
    private final TriggerRegistrar _triggerRegistrar;
    private final UUID uuid;

    private DBLocalDatabase(UUID uuid, IDBPersister persister) throws IOException {

        LOG.log(Level.FINE, "Set ODB UUID to " + uuid);
        this.uuid    = uuid;
        _dataMan     = new DatabaseManager(persister, uuid);
        _admin       = new DBAdmin(_dataMan);

        // Handle trigger registrations.
        _triggerRegistrar = new TriggerRegistrar(_dataMan.getProgramManager());
    }

    private static final class UuidIo {
        private final File uuidFile;

        UuidIo(File dbRootDir) {
            uuidFile = new File(dbRootDir, "uuid");
        }

        boolean exists() {
            return uuidFile.exists();
        }

        UUID load() {
            Scanner s = null;
            try {
                s = new Scanner(uuidFile, "UTF-8").useDelimiter("\\Z");
                return UUID.fromString(s.next());
            } catch (Exception ex) {
                final String msg = "Couldn't load uuid file: " + uuidFile;
                LOG.log(Level.SEVERE, msg, ex);
                throw new RuntimeException(msg, ex);
            } finally {
                if (s != null) s.close();
            }
        }

        void store(UUID uuid) {
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(uuidFile, "UTF-8");
                pw.write(uuid.toString());
                pw.flush();
            } catch (Exception ex) {
                final String msg = "Couldn't write the uuid file: " + uuidFile;
                LOG.log(Level.SEVERE, msg, ex);
                throw new RuntimeException(msg, ex);
            } finally {
                if (pw != null) pw.close();
            }
        }
    }

    @Override public UUID getUuid() {
        return uuid;
    }

    /**
     * Checkpoints the given program.
     * See <code>{@link IDBDatabaseService#checkpoint(ISPProgram)}</code>.
     */
    public void checkpoint(ISPProgram prog) {
        _dataMan.getProgramStorageManager().checkpoint(prog);
    }

    /**
     * Checkpoints all outstanding modifications.
     * See <code>{@link IDBDatabaseService#checkpoint()}</code>.
     */
    public void checkpoint() {
        _dataMan.getProgramStorageManager().checkpoint();
        _dataMan.getPlanStorageManager().checkpoint();
    }

    public SPNodeKey lookupProgramKeyByID(SPProgramID programID) {
        return _dataMan.getProgramManager().lookupProgramKey(programID);
    }

    public ISPObservation lookupObservationByID(SPObservationID obsID) {

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("DBDatabase.lookupObservationByID(" + obsID + ")");
        }

        ISPProgram prog = lookupProgramByID(obsID.getProgramID());
        if (prog == null) return null;

        for (ISPObservation obs : prog.getAllObservations()) {
            if (obs.getObservationNumber() == obsID.getObservationNumber()) {
                return obs;
            }
        }
        return null;
    }

    /**
     * Fetches the named program.
     * See <code>{@link IDBDatabaseService#lookupProgram}</code>.
     */
    public ISPProgram lookupProgram(SPNodeKey programKey) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("DBDatabase.lookupProgram(" + programKey + ")");
        }
        return _dataMan.getProgramManager().lookupProgram(programKey);
    }

    public ISPProgram lookupProgramByID(SPProgramID progID) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("DBDatabase.lookupProgramByID(" + progID + ")");
        }
        return _dataMan.getProgramManager().lookupProgramByID(progID);
    }

    public ISPProgram put(ISPProgram program) throws DBIDClashException {
        LOG.fine("DBDatabase.put(program)");
        return _dataMan.getProgramManager().putProgram(program);
    }

    public ISPNightlyRecord put(ISPNightlyRecord record) throws DBIDClashException {
        LOG.fine("DBDatabase.put(nightly record)");
        return _dataMan.getNightlyPlanManager().putProgram(record);
    }

    public boolean remove(ISPProgram program) {
        LOG.fine("DBDatabase.remove(program)");

        if (_dataMan.getProgramManager().removeProgram(program.getProgramKey())) {
            _dataMan.getPersister().remove(program.getProgramKey());
            return true;
        } else {
            return false;
        }
    }

    public boolean remove(ISPNightlyRecord nightlyRecord) {
        LOG.fine("DBDatabase.removeNightlyRecord(nightlyRecord)");

        if (_dataMan.getNightlyPlanManager().removeProgram(nightlyRecord.getProgramKey())) {
            _dataMan.getPersister().remove(nightlyRecord.getProgramKey());
            return true;
        } else {
            return false;
        }
    }

    public ISPProgram removeProgram(SPNodeKey programKey) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("DBDatabase.removeProgram(" + programKey + ")");
        }

        ProgramManager<ISPProgram> pm = _dataMan.getProgramManager();
        ISPProgram prog = pm.lookupProgram(programKey);
        if (prog == null) return null;
        pm.removeProgram(prog.getNodeKey());
        return prog;
    }

    public boolean removeNightlyRecord(SPNodeKey nightlyRecordKey) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("DBDatabase.removeNightlyRecord(" + nightlyRecordKey + ")");
        }

        ProgramManager<ISPNightlyRecord> pm = _dataMan.getNightlyPlanManager();
        ISPNightlyRecord nightlyRecord = pm.lookupProgram(nightlyRecordKey);
        if (nightlyRecord == null) return false;
        pm.removeProgram(nightlyRecord.getProgramKey());
        return true;
    }

    public void addProgramEventListener(ProgramEventListener<ISPProgram> pel) {
        _dataMan.getProgramManager().addListener(pel);
    }

    public void removeProgramEventListener(ProgramEventListener<ISPProgram> pel) {
        _dataMan.getProgramManager().removeListener(pel);
    }

    public void addNightlyRecordEventListener(ProgramEventListener<ISPNightlyRecord> rel) {
        _dataMan.getNightlyPlanManager().addListener(rel);
    }

    public void removeNightlyRecordEventListener(ProgramEventListener<ISPNightlyRecord> rel) {
        _dataMan.getNightlyPlanManager().removeListener(rel);
    }

    /**
     * Gets the administration object.
     * See <code>{@link IDBDatabaseService#getDBAdmin}</code>.
     */
    public IDBAdmin getDBAdmin() { return _admin; }

    /**
     * Fetches the named nightly plan.
     * See <code>{@link IDBDatabaseService#lookupNightlyPlan}</code>.
     */
    public ISPNightlyRecord lookupNightlyPlan(SPNodeKey nightlyPlanKey) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("DBDatabase.lookupNightlyPlan(" + nightlyPlanKey + ")");
        }
        return _dataMan.getNightlyPlanManager().lookupProgram(nightlyPlanKey);
    }

    public ISPNightlyRecord lookupNightlyRecordByID(SPProgramID planID) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("DBDatabase.lookupNightlyRecordByID(" + planID + ")");
        }
        return _dataMan.getNightlyPlanManager().lookupProgramByID(planID);
    }

    @Override public long fileSize(SPNodeKey key) {
        return _dataMan.getPersister().size(key);
    }

    /**
     * Gets the factory used to create new program nodes.
     * See <code>{@link IDBDatabaseService#getFactory}</code>.
     */
    public ISPFactory getFactory() {
        LOG.fine("DBDatabase.getFactory()");
        return _dataMan.getFactory();
    }

    public <T extends IDBFunctor> T execute(final T functor, final ISPNode node, final Set<Principal> ps) throws SPNodeNotLocalException {
        LOG.fine("DBDatabase.execute()");

        WithPriority.exec(functor.getPriority(), new Runnable() {
            public void run() {
                FunctorLogger.Handback hb = _dataMan.functorLogger.logStart(functor);
                try {
                    functor.execute(DBLocalDatabase.this, node, ps);
                } catch (Exception ex) {
                    functor.setException(ex);
                }
                _dataMan.functorLogger.logEnd(functor, hb);

            }
        });

        return functor;
    }

    public IDBQueryRunner getQueryRunner(Set<Principal> principals) {
        LOG.fine("DBDatabase.getQueryRunner()");
        return new QueryRunner(this, _dataMan, principals);
    }

    public IDBQueryRunner getQueryRunner() {
        return getQueryRunner(Collections.<Principal>emptySet());
    }


    public void registerTrigger(IDBTriggerCondition condition, IDBTriggerAction action) {
        _triggerRegistrar.register(condition, action);
    }

    public void unregisterTrigger(IDBTriggerCondition condition, IDBTriggerAction action) {
        _triggerRegistrar.unregister(condition, action);
    }

}


