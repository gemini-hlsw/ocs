package edu.gemini.obslog.osgi;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.spdb.IDBDatabaseService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.logging.Logger;

public class DatabaseTracker extends ServiceTracker {

	private static final Logger LOGGER = Logger.getLogger(DatabaseTracker.class.getName());

	public DatabaseTracker(BundleContext context) {
		super(context, IDBDatabaseService.class.getName(), null);
	}

	@Override
	public Object addingService(ServiceReference ref) {
		IDBDatabaseService db = (IDBDatabaseService) context.getService(ref);
		SPDB.init(db);
		LOGGER.info("Adding " + db.getUuid());
		return db;

	}

	@Override
	public void removedService(ServiceReference ref, Object service) {
		IDBDatabaseService db = (IDBDatabaseService) service;
        LOGGER.info("Removing " + db.getUuid());
        SPDB.clear();
		context.ungetService(ref);
	}

}

