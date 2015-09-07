//
// $Id: WorkingStoreTracker.java 208 2005-10-16 23:32:18Z shane $
//

package edu.gemini.dataman.osgi;

import edu.gemini.dataman.raw.RawCopier;
import edu.gemini.dirmon.DirLocation;
import edu.gemini.dirmon.MonitoredDir;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Tracks the working storage {@link edu.gemini.dirmon.MonitoredDir} so that
 * {@link edu.gemini.dataman.raw.RawCopier} can tell it which datasets to
 * expect in the near future.
 */
final class WorkingStoreTracker implements ServiceTrackerCustomizer {
    private static final Logger LOG = Logger.getLogger(WorkingStoreTracker.class.getName());

    private BundleContext _context;
    private String _workDirPath;
    private RawCopier _rawCopier;
    private ServiceTracker _tracker;

    WorkingStoreTracker(BundleContext ctx, String workDirPath, RawCopier rawCopier) {
        _context     = ctx;
        _workDirPath = workDirPath;
        _rawCopier   = rawCopier;
    }

    synchronized void start() {
        if (_tracker != null) return;
        Filter filt = _getServiceFilter(_context, _workDirPath);
        _tracker = new ServiceTracker(_context, filt, this);
        _tracker.open();
    }

    synchronized void stop() {
        if (_tracker == null) return;
        _tracker.close();
        _tracker = null;

        _rawCopier.setWorkMonitoredDir(null);
    }

    private static Filter _getServiceFilter(BundleContext ctx, String workDirPath) {
        StringBuilder buf = new StringBuilder();
        buf.append("(&");
        buf.append(" (objectClass=").append(MonitoredDir.class.getName()).append(')');
        buf.append(" (").append(DirLocation.DIR_PATH_PROP).append('=');
        buf.append(workDirPath).append(')');
        buf.append(')');

        String filter = buf.toString();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Monitor working store: " + filter);
        }

        try {
            return ctx.createFilter(buf.toString());
        } catch (Exception ex) {
            LOG.severe("Error in filter string: " + buf);
            throw new RuntimeException(ex);
        }
    }

    public Object addingService(ServiceReference ref) {
        MonitoredDir md = (MonitoredDir) _context.getService(ref);
        if (LOG.isLoggable(Level.FINE)) {
//            try {
                LOG.log(Level.FINE, "Found working store monitored dir: " + md.getDirLocation());
//            } catch (RemoteException ex) {
//                LOG.log(Level.FINE, "Found working store monitored dir.");
//            }
        }
        _rawCopier.setWorkMonitoredDir(md);
        return md;
    }

    public void modifiedService(ServiceReference ref, Object object) {
    }

    public void removedService(ServiceReference ref, Object object) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Removed working store monitored dir.");
        }
        _context.ungetService(ref);
        _rawCopier.setWorkMonitoredDir(null);
    }
}
