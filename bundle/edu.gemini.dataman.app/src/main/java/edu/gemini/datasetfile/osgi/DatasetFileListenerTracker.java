//
// $Id: DatasetFileListenerTracker.java 150 2005-09-27 12:36:41Z shane $
//

package edu.gemini.datasetfile.osgi;

import edu.gemini.datasetfile.DatasetFileListener;
import edu.gemini.datasetfile.impl.DatasetFileServiceImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 *
 */
final class DatasetFileListenerTracker implements ServiceTrackerCustomizer {
//    private static final Logger LOG = Logger.getLogger(DatasetFileListenerTracker.class.getName());

    private BundleContext _context;
    private DatasetFileServiceImpl _service;
    private ServiceTracker _tracker;

//    private ServiceRegistration _dirMonReg;

    DatasetFileListenerTracker(BundleContext ctx, DatasetFileServiceImpl service) {
        _context = ctx;
        _service = service;
    }

    synchronized void start() {
        if (_tracker != null) return;
        _tracker = new ServiceTracker(_context, DatasetFileListener.class.getName(), this);
        _tracker.open();
    }

    synchronized void stop() {
        if (_tracker == null) return;
        _tracker.close();
        _tracker = null;
    }

    private synchronized void _addListener(DatasetFileListener ls) {
        _service.addFileListener(ls);
//        if (_dirMonReg != null) return; // already registered
//
//        // Register the DatasetFileServiceImpl as a directory monitor.
//        File workDir = _service.getDir();
//        Properties props = new Properties();
//        props.put(DirLocation.DIR_PATH_PROP, workDir.getPath());
//
//        String srvName = DirListener.class.getName();
//        _dirMonReg = _context.registerService(srvName, _service, props);
    }

    private synchronized void _removeListener(DatasetFileListener ls) {
        _service.removeFileListener(ls);
//        if (_dirMonReg == null) return; // not registered
//        if (_service.getFileListenerCount() > 0) return; // still active listeners
//        _dirMonReg.unregister();
//        _dirMonReg = null;
    }

    public Object addingService(ServiceReference ref) {
        DatasetFileListener ls = (DatasetFileListener) _context.getService(ref);
        _addListener(ls);
        return ls;
    }

    public void modifiedService(ServiceReference ref, Object object) {
    }

    public void removedService(ServiceReference ref, Object object) {
        _removeListener((DatasetFileListener) object);
        _context.ungetService(ref);
    }
}
