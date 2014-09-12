//
// $Id: DatabaseTracker.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.osgi;

import edu.gemini.datasetrecord.DatasetRecordService;
import edu.gemini.datasetrecord.impl.DsetRecordServiceImpl;
import edu.gemini.pot.spdb.IDBDatabaseService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks {@link edu.gemini.pot.spdb.IDBDatabaseService}, setting/clearing the
 * database on the {@link DatasetRecordService} as they come and go.
 */
final class DatabaseTracker implements ServiceTrackerCustomizer {
    private static final Logger LOG = Logger.getLogger(DatabaseTracker.class.getName());

    private BundleContext _ctx;
    private DsetRecordServiceImpl _srv;
    private ServiceTracker _tracker;

    private ServiceRegistration _srvReg;

    DatabaseTracker(BundleContext ctx, DsetRecordServiceImpl service) {
        _ctx = ctx;
        _srv = service;
    }

    synchronized void start() {
        if (_tracker != null) return;

        String dbClassName = IDBDatabaseService.class.getName();
        _tracker = new ServiceTracker(_ctx, dbClassName, this);
        _tracker.open();
    }

    synchronized void stop() {
        if (_tracker == null) return;
        _tracker.close();
        _tracker = null;
        _ctx = null;
    }

    private synchronized void _addDatabase(IDBDatabaseService db) {
        _srv.addDatabase(db);
        if (_srvReg != null) return; // already registered
        String srvName = DatasetRecordService.class.getName();
        _srvReg = _ctx.registerService(srvName, _srv, null);
        LOG.info("DatasetRecordService online.");
    }

    private synchronized void _removeDatabase(IDBDatabaseService db) {
        _srv.removeDatabase(db);
        if (_srvReg == null) return; // not registered
        if (_srv.getDatabaseCount() > 0) return; // still active databases
        _srvReg.unregister();
        _srvReg = null;
        LOG.info("DatasetRecordService offline.");
    }

    public Object addingService(ServiceReference ref) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Found an SPDB: id=" + ref.getProperty("service.id"));
        }
        IDBDatabaseService db = (IDBDatabaseService) _ctx.getService(ref);


        _addDatabase(db);
        return db;
    }

    public void modifiedService(ServiceReference ref, Object object) {
        // ignore for now
    }

    public void removedService(ServiceReference ref, Object object) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.info("Removed an SPDB: id=" + ref.getProperty("service.id"));
        }
        _removeDatabase((IDBDatabaseService) object);
        _ctx.ungetService(ref);
    }
}
