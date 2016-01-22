package edu.gemini.obslog.osgi;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.spdb.IDBDatabaseService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.logging.Logger;

class DatabaseTracker extends ServiceTracker<IDBDatabaseService, IDBDatabaseService> {

    private static final Logger LOGGER = Logger.getLogger(DatabaseTracker.class.getName());

    DatabaseTracker(BundleContext context) {
        super(context, IDBDatabaseService.class.getName(), null);
    }

    @Override
    public IDBDatabaseService addingService(ServiceReference<IDBDatabaseService> ref) {
        IDBDatabaseService db = context.getService(ref);
        SPDB.init(db);
        LOGGER.info("Adding " + db.getUuid());
        return db;

    }

    @Override
    public void removedService(ServiceReference<IDBDatabaseService> ref, IDBDatabaseService db) {
        LOGGER.info("Removing " + db.getUuid());
        SPDB.clear();
        context.ungetService(ref);
    }

}

