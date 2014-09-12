//
// $Id: DirListenerTracker.java 206 2005-10-16 23:10:39Z shane $
//

package edu.gemini.dirmon.impl.osgi;

import edu.gemini.dirmon.DirListener;
import edu.gemini.dirmon.DirLocation;
import edu.gemini.dirmon.MonitoredDir;
import edu.gemini.dirmon.impl.*;
import edu.gemini.dirmon.util.DefaultDirLocation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Dictionary;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 */
final class DirListenerTracker implements ServiceTrackerCustomizer {
    private static final Logger LOG = Logger.getLogger(DirListenerTracker.class.getName());

    // DirWrapper is returned from the addingService method and used to keep
    // up with the monitored dir impl associated with a DirListener.  However,
    // DirListener can change the location they want to monitor. If that
    // happens the MonitoredDirImpl must change as well.  Yet the object
    // returned by the addingService method cannot be swapped for another.
    private static class DirWrapper {
        private MonitoredDirImpl _dir;

        DirWrapper(MonitoredDirImpl dir) {
            _dir = dir;
        }

        MonitoredDirImpl getDir() {
            return _dir;
        }

        void setDir(MonitoredDirImpl dir) {
            _dir = dir;
        }
    }

    private static class MonitoredDirReg {
        MonitoredDirImpl dir;
        ServiceRegistration reg;

        MonitoredDirReg(MonitoredDirImpl dir, ServiceRegistration reg) {
            this.dir = dir;
            this.reg = reg;
        }
    }

    private BundleContext _context;
    private String _addr;
    private ServiceTracker _tracker;
    private Map<DirLocation, MonitoredDirReg>  _dirs = new HashMap<DirLocation, MonitoredDirReg>();

    private MonitoredDirConfig _config;

    DirListenerTracker(BundleContext ctx, String addr) {
        _context = ctx;
        _addr    = addr;
        _config  = new OsgiMonitoredDirConfig(ctx);
    }

    synchronized void start() {
        if (_tracker != null) return;
        _tracker = new ServiceTracker(_context, _getServiceFilter(_context), this);
        _tracker.open();
    }

    synchronized void stop() {
        if (_tracker == null) return;
        _tracker.close();
        _tracker = null;

        for (MonitoredDirReg mdr : _dirs.values()) {
            mdr.dir.stop();
            mdr.dir.discard();
            mdr.reg.unregister();
        }
        _dirs.clear();
    }

    private static Filter _getServiceFilter(BundleContext ctx) {
        StringBuilder buf = new StringBuilder();
        buf.append("(&");
        buf.append(" (objectClass=").append(DirListener.class.getName()).append(')');
//        buf.append(" (").append(DirLocation.HOST_PROP).append('=').append(addr).append(')');
        buf.append(" (").append(DirLocation.DIR_PATH_PROP).append("=*)");
        buf.append(')');

        try {
            return ctx.createFilter(buf.toString());
        } catch (Exception ex) {
            LOG.severe("Error in filter string: " + buf);
            throw new RuntimeException(ex);
        }
    }


    private MonitoredDirImpl _lookupImpl(DirLocation loc) {
        MonitoredDirReg mdr = _dirs.get(loc);
        if (mdr == null) return null;
        return mdr.dir;
    }

    private ServiceRegistration _registerMonitoredDir(MonitoredDir dir) {
        String className = MonitoredDir.class.getName();
        DirLocation loc;

//        try {
            loc = dir.getDirLocation();
//        } catch (RemoteException ex) {
//            LOG.log(Level.SEVERE, "remote exception from local method call", ex);
//            throw new RuntimeException(ex);
//        }

        Dictionary props;
        if (loc instanceof DefaultDirLocation) {
            props = ((DefaultDirLocation) loc).toDictionary();
        } else {
            props = (new DefaultDirLocation(loc)).toDictionary();
        }

        if (LOG.isLoggable(Level.FINE)) {
            StringBuilder buf = new StringBuilder();
            buf.append("Register monitored dir as service.  ");
            buf.append(DirLocation.DIR_PATH_PROP).append("='");
            buf.append(props.get(DirLocation.DIR_PATH_PROP)).append("'");
            LOG.log(Level.FINE, buf.toString());
        }


        ServiceRegistration reg;
        reg = _context.registerService(className, dir, props);

        return reg;
    }

    private MonitoredDirImpl _getImpl(DirLocation loc) throws IOException {
        MonitoredDirImpl dir = _lookupImpl(loc);
        if (dir == null) {
            dir = new MonitoredDirImpl(loc, _config);
            dir.start();

            ServiceRegistration reg = _registerMonitoredDir(dir);
            _dirs.put(loc, new MonitoredDirReg(dir, reg));
        }
        return dir;
    }

    private void _forgetImpl(MonitoredDirImpl dir) {
        MonitoredDirReg mdr = _dirs.remove(dir.getDirLocation());
        if (mdr != null) {
            mdr.reg.unregister();
        }
        dir.stop();
        dir.discard();
    }

    public void _removeListener(DirListener ls, MonitoredDirImpl dir) {
        // Stop getting updates on the old directory.
        dir.removeListener(ls);
        if (dir.getListenerCount() == 0) {
            _forgetImpl(dir);
        }
    }

    private static DirLocation _getLocation(ServiceReference ref) {
        String dirPath;
        dirPath = (String) ref.getProperty(DirLocation.DIR_PATH_PROP);

        String addr;
        addr = (String) ref.getProperty(DirLocation.HOST_PROP);

        return new DefaultDirLocation(dirPath, addr);
    }

    public synchronized Object addingService(ServiceReference ref) {

        // Figure out where the DirListener wants to watch.
        DirLocation loc = _getLocation(ref);
        LOG.info("dirmon-server found DirListener for: " + loc);
        String host = loc.getHostAddress();
        if ((host != null) && !_addr.equals(host)) {
            return null;
        }

        // Get a MonitoredDirImpl for this location, creating it if necessary.
        MonitoredDirImpl dir;
        try {
            dir = _getImpl(loc);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Could not monitor: " + loc, ex);
            return new DirWrapper(null);
        }

        // Add the listener.
        DirListener ls = (DirListener) _context.getService(ref);
        dir.addListener(ls);

        // Return the service object.
        return new DirWrapper(dir);
    }

    public synchronized void modifiedService(ServiceReference ref, Object o) {
        if (o == null) return; // for some other host

        // Get the monitored directory.
        DirWrapper wrapper = (DirWrapper) o;
        MonitoredDirImpl oldDir = wrapper.getDir();

        // Get the directory they are interested in now.
        DirLocation newLoc = _getLocation(ref);

        if (oldDir != null) {
            // Get the directory they were interested in watching.
            DirLocation oldLoc = oldDir.getDirLocation();

            // If the same, there is nothing to do here.
            if (oldLoc.equals(newLoc)) return;

            LOG.info("DirListener stopped watching: " + oldLoc);
        }
        LOG.info("DirListener now watching: " + newLoc);

        // Get the directory listener.
        DirListener ls = (DirListener) _context.getService(ref);

        // Remove it and cleanup the old monitored dir.
        _removeListener(ls, oldDir);


        // See if there is an monitored dir for this location.
        MonitoredDirImpl newDir;
        try {
            newDir = _getImpl(newLoc);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Could not monitor: " + newLoc, ex);
            wrapper.setDir(null);
            return;
        }

        // Add the listener.
        newDir.addListener(ls);

        // Update the wrapper.
        wrapper.setDir(newDir);
    }

    public synchronized void removedService(ServiceReference ref, Object o) {
        if (o == null) return; // for some other host

        // Find the listener.
        DirListener ls = (DirListener) _context.getService(ref);

        // Let the service go.
        _context.ungetService(ref);

        // Get the monitored directory, if any.
        DirWrapper wrapper = (DirWrapper) o;
        MonitoredDirImpl dir = wrapper.getDir();
        if (dir == null) return;  // nothing more to cleanup

        DirLocation loc = _getLocation(ref);
        LOG.info("dirmon-server remove DirListener for: " + loc);

        _removeListener(ls, dir);

    }
}
