package edu.gemini.spdb.reports.osgi;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spdb.reports.impl.BatchReportsTask;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

public class DatabaseTracker extends ServiceTracker {

    private final File batchRoot;
    private HttpTracker httpTracker;
    private final Set<Principal> user;

	public DatabaseTracker(BundleContext context, File batchRoot, Set<Principal> user) {
		super(context, IDBDatabaseService.class.getName(), null);
        this.batchRoot = batchRoot;
        this.user = user;
    }

	@Override
	public Object addingService(ServiceReference ref) {
		IDBDatabaseService db = (IDBDatabaseService) context.getService(ref);
        httpTracker = new HttpTracker(context, batchRoot, db, user);
        httpTracker.open();
		return db;
	}

	@Override
	public void removedService(ServiceReference ref, Object service) {
        httpTracker.close();
		context.ungetService(ref);
	}

}

