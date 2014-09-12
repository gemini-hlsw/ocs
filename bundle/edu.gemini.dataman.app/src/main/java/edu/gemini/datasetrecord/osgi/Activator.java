//
// $Id: Activator.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.osgi;

import edu.gemini.datasetrecord.impl.DsetRecordServiceImpl;
import edu.gemini.util.security.principal.StaffPrincipal;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Bundle activator for the dataset trigger client.  Handles watching for
 * listeners and databases and making sure all listeners receive events from
 * the database as they happen.
 */
public final class Activator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private DsetRecordServiceImpl _service;
    private DatasetRecordListenerTracker _listenerTracker;
    private DatabaseTracker _dbTracker;

    // Run as superuser
    private final Set<Principal> user = Collections.<Principal>singleton(StaffPrincipal.Gemini());

    public Activator() {
        _service = new DsetRecordServiceImpl(user);
    }

    public synchronized void start(BundleContext ctx) throws Exception {
        LOG.info("Start dataset-record bundle");

        // Register as a DatasetRecordService
        _service.start();

        // Start watching for DsetTriggerListener
        _listenerTracker = new DatasetRecordListenerTracker(ctx, _service);
        _listenerTracker.start();

        // Start watching for IDBDatabaseService
        _dbTracker = new DatabaseTracker(ctx, _service);
        _dbTracker.start();
    }

    public synchronized void stop(BundleContext bundleContext) throws Exception {
        LOG.info("Stop dataset-record bundle");

        _dbTracker.stop();
        _dbTracker = null;

        _listenerTracker.stop();
        _listenerTracker = null;

        _service.stop();
    }
}
