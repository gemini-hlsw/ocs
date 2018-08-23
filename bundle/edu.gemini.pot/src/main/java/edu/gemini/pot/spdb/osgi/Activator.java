package edu.gemini.pot.spdb.osgi;

import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBQueryRunner;
import edu.gemini.util.BundleProperties;
import edu.gemini.util.osgi.*;
import org.osgi.framework.*;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Activator implements BundleActivator {
    private static final Logger LOGGER = Logger.getLogger(Activator.class.getName());

    private enum Mode {
        api, local;
        static Mode DEFAULT = local;
    }

    /**
     * Bundle property that specifies the SPDB mode.
     * @see Mode
     */
    public static final String BUNDLE_PROP_MODE = "edu.gemini.spdb.mode";

    /**
     * Bundle property that specifies the location of the database directory.
     * Legal values are a valid file path.  Default is local bundle storage.
     */
    public static final String BUNDLE_PROP_DIR = "edu.gemini.spdb.dir";

    /**
     * Bundle property specifying the ICTD database user.
     */
    public static final String ICTD_USER_PROP = "edu.gemini.ictd.user";

    /**
     * Bundle property specifying the ICTD database password.
     */
    public static final String ICTD_PASS_PROP = "edu.gemini.ictd.password";


    // Mutable state
    private DatabaseLoader loader;

    public void start(BundleContext context) throws Exception {

        // Get the mode and dbDir. Both will be non-null.
        BundleProperties props = new BundleProperties(context);
        final Mode mode = props.getEnum(BUNDLE_PROP_MODE, Mode.DEFAULT, Mode.class);

        // Start stuff up, depending on Mode
        if ((mode == Mode.local) && (loader == null)) {
            final File dbDir;
            final String dirString = props.getString(BUNDLE_PROP_DIR, null);
            if (dirString != null) {
                dbDir = new File(dirString);
            } else {
                dbDir = ExternalStorage$.MODULE$.getExternalDataFile(context, "spdb");
            }
            loader = new DatabaseLoader(context, dbDir);
        }

    }

    public void stop(BundleContext context) throws Exception {
        try {
            if (loader != null) loader.stop();
        } finally {
            loader = null;
        }
    }

    private static final class DatabaseLoader implements Runnable {
        private enum State {
            loading, ready, stopped;
        }

        private final BundleContext ctx;
        private final File dbDir;

        private State state;
        private IDBDatabaseService db;
        private ServiceRegistration<IDBDatabaseService> dbReg;
        private ServiceRegistration<SecureServiceFactory<IDBQueryRunner>> qrReg;

        DatabaseLoader(BundleContext ctx, File dir) {
            this.ctx   = ctx;
            this.dbDir = dir;
            this.state = State.loading;

            Thread t = new Thread(this, "DatabaseLoader");
            t.setDaemon(true);
            t.start();
        }

        synchronized void setReady(IDBDatabaseService db) {
            if (state == State.stopped) return;
            state = State.ready;

            this.db = db;

            LOGGER.info("Exporting database to OSGi.");
            final Dictionary<String, Object> properties1 = new Hashtable<>();
            dbReg = ctx.registerService(IDBDatabaseService.class, db, properties1);

            LOGGER.info("Exporting query runner to OSGi and TRPC.");
            final Dictionary<String, Object> properties2 = new Hashtable<>();
            properties2.put("trpc", ""); // publish just the QueryRunner as TRPC service
            // A factory to make a new query runner for each TRPC user
            final SecureServiceFactoryForJava<IDBQueryRunner> factory = new SecureServiceFactoryForJava<IDBQueryRunner>() {
                public IDBQueryRunner getService(Set<Principal> ps) {
                    return DatabaseLoader.this.db.getQueryRunner(ps);
                }
            };
            qrReg = SecureServiceFactory$.MODULE$.<IDBQueryRunner>registerSecureServiceForJava(
                ctx, factory, IDBQueryRunner.class, properties2
            );

        }

        synchronized void stop() {
            try {
                switch (state) {
                    case stopped:
                    case loading:
                        // don't have a way to interrupt the loading ...
                        break;
                    case ready:
                        LOGGER.info("Removing query runner OSGi service.");
                        qrReg.unregister();
                        LOGGER.info("Removing database OSGi service.");
                        dbReg.unregister();
                        LOGGER.info("Stopping database.");
                        db.getDBAdmin().stopDb();
                        LOGGER.info("Done.");
                        break;
                }
            } finally {
                dbReg = null;
                qrReg = null;
                db    = null;
                state = State.stopped;
            }
        }

        public void run() {
            LOGGER.info("Starting local database on " + dbDir.getAbsolutePath());
            try {
                setReady(DBLocalDatabase.create(dbDir));
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Could not start ODB on " + dbDir, ex);
                try {
                    ctx.getBundle().stop();
                } catch (Exception bex) {
                    LOGGER.log(Level.WARNING, "Could not stop bundle.", bex);
                }
            }
        }
    }
}
