//
// $Id: DatasetRecordListenerTracker.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.osgi;

import edu.gemini.datasetrecord.DatasetRecordListener;
import edu.gemini.datasetrecord.impl.DsetRecordServiceImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks {@link edu.gemini.datasetrecord.DatasetRecordListener} services,
 * (un)registering them with the
 * {@link edu.gemini.datasetrecord.DatasetRecordService} as they come and go.
 */
final class DatasetRecordListenerTracker implements ServiceTrackerCustomizer {
    private static final Logger LOG = Logger.getLogger(DatasetRecordListenerTracker.class.getName());

    private BundleContext _ctx;
    private DsetRecordServiceImpl _srv;
    private ServiceTracker _tracker;

    DatasetRecordListenerTracker(BundleContext ctx, DsetRecordServiceImpl srv) {
        _ctx = ctx;
        _srv = srv;
    }

    synchronized void start() {
        if (_tracker != null) return;
        _tracker = new ServiceTracker(_ctx, _getServiceFilter(_ctx), this);
        _tracker.open();
    }

    synchronized void stop() {
        if (_tracker == null) return;
        _tracker.close();
        _tracker = null;
        _ctx = null;
    }

    private static Filter _getServiceFilter(BundleContext ctx) {
        StringBuilder buf = new StringBuilder();
        buf.append("(&");
        buf.append(" (objectClass=").append(DatasetRecordListener.class.getName()).append(')');
        buf.append(')');

        try {
            return ctx.createFilter(buf.toString());
        } catch (InvalidSyntaxException ex) {
            // this shouldn't be a checked exception
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public Object addingService(ServiceReference ref) {
        LOG.info("Found a DatasetRecordListener");
        DatasetRecordListener ls = (DatasetRecordListener) _ctx.getService(ref);
        _srv.addListener(ls);
        return ls;
    }

    public void modifiedService(ServiceReference ref, Object object) {
        // don't think we care
    }

    public void removedService(ServiceReference ref, Object object) {
        LOG.info("Removing a DatasetRecordListener");
        _ctx.ungetService(ref);

        DatasetRecordListener ls = (DatasetRecordListener) object;
        if (ls == null) return;  // not sure it can be null ...
        _srv.removeListener(ls);
    }
}
