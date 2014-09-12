//
// $Id: OsgiDatamanContext.java 697 2006-12-15 17:55:42Z shane $
//

package edu.gemini.dataman.osgi;

import edu.gemini.dataman.context.DatamanConfig;
import edu.gemini.dataman.context.DatamanContext;
import edu.gemini.dataman.context.DatamanState;
import edu.gemini.dataman.gsa.GsaVigilante;
import edu.gemini.dataman.listener.FileListener;
import edu.gemini.dataman.listener.RecordListener;
import edu.gemini.dataman.qacheck.QaCheckMailer;
import edu.gemini.dataman.sync.DatasetSync;
import edu.gemini.dataman.util.DatasetCommandProcessor;
import edu.gemini.datasetfile.DatasetFileListener;
import edu.gemini.datasetfile.DatasetFileService;
import edu.gemini.datasetrecord.DatasetRecordListener;
import edu.gemini.datasetrecord.DatasetRecordService;
import edu.gemini.pot.spdb.IDBDatabaseService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The controller is responsible for keeping track of services that the
 * Dataman app depends upon.  It also registers listeners for the
 * {@link DatasetFileService} and {@link DatasetRecordService} and removes
 * them as needed.
 */
final class OsgiDatamanContext implements DatamanContext {
    private static final Logger LOG = Logger.getLogger(OsgiDatamanContext.class.getName());

    private BundleContext _ctx;
    private final DatamanConfig _conf;
    private final DatamanState _state;
    private boolean _started;

    // DatasetFileService
    private FileListener _fileListener; // our listener
    private ServiceRegistration _fileListenerReg; // our listener registration
    private ServiceTracker _fileTracker; // service tracker for the file service
    private DatasetFileService _fileService; // file service itself

    // DatasetRecordService
    private RecordListener _recordListener;
    private ServiceRegistration _recordListenerReg;
    private ServiceTracker _recordTracker; // service tracker for record service
    private DatasetRecordService _recordService;  // record service itself

    // IDBDatabaseService
    private ServiceTracker _dbTracker;
    private Set<IDBDatabaseService> _dbServiceSet = new HashSet<IDBDatabaseService>();

    // DatasetSync
    private Future<Boolean> _syncFuture;

    // GsaVigilante
    private GsaVigilante _vigilante;

    // QA Check state checker/mailer
    private QaCheckMailer _mailer;

    // Who are we doing this for?
    private final Set<Principal> _user;

    OsgiDatamanContext(BundleContext ctx, Set<Principal> user) throws OsgiConfigException, IOException {
        _ctx = ctx;
        _conf = new OsgiDatamanConfig(ctx);
        _state = new OsgiDatamanState(ctx);

        _fileListener = new FileListener(this);
        _recordListener = new RecordListener(this);

        _vigilante = new GsaVigilante(this, user);
        _mailer    = new QaCheckMailer(this, user);

        _user = user;
    }

    BundleContext getBundleContext() {
        return _ctx;
    }

    synchronized void start() {
        if (_started) return;
        _started = true;

        _fileTracker = new ServiceTracker(_ctx, DatasetFileService.class.getName(),
            new ServiceTrackerCustomizer() {
                public Object addingService(ServiceReference ref) {
                    DatasetFileService srv;
                    srv = (DatasetFileService) _ctx.getService(ref);
                    setDatasetFileService(srv);
                    return srv;
                }

                public void modifiedService(ServiceReference ref, Object object) {
                }

                public void removedService(ServiceReference ref, Object object) {
                    setDatasetFileService(null);
                    _ctx.ungetService(ref);
                }
            });
        _fileTracker.open();

        _recordTracker = new ServiceTracker(_ctx, DatasetRecordService.class.getName(),
            new ServiceTrackerCustomizer() {
                public Object addingService(ServiceReference ref) {
                    DatasetRecordService srv;
                    srv = (DatasetRecordService) _ctx.getService(ref);
                    setDatasetRecordService(srv);
                    return srv;
                }

                public void modifiedService(ServiceReference ref, Object object) {
                }

                public void removedService(ServiceReference ref, Object object) {
                    setDatasetRecordService(null);
                    _ctx.ungetService(ref);
                }
            });
        _recordTracker.open();

        _dbTracker = new ServiceTracker(_ctx, IDBDatabaseService.class.getName(),
            new ServiceTrackerCustomizer() {

                public Object addingService(ServiceReference ref) {
                    IDBDatabaseService db;
                    db = (IDBDatabaseService) _ctx.getService(ref);
                    addDatabase(db);
                    return db;
                }

                public void modifiedService(ServiceReference ref, Object object) {
                }

                public void removedService(ServiceReference ref, Object object) {
                    removeDatabase((IDBDatabaseService) object);
                    _ctx.ungetService(ref);
                }
            });
        _dbTracker.open();
    }

    synchronized void stop() {
        if (!_started) return;
        _started = false;

        _shutdown();
        _fileTracker.close();
        _fileTracker = null;
        _recordTracker.close();
        _recordTracker = null;
        _dbTracker.close();
        _dbTracker = null;
    }

    private synchronized boolean _allServicesPresent() {
        return (_fileService != null) && (_recordService != null) &&
                (_dbServiceSet.size() > 0);
    }

    public synchronized DatasetFileService getDatasetFileService() {
        return _fileService;
    }

    synchronized void setDatasetFileService(DatasetFileService service) {
        _fileService = service;
        if (service == null) {
            _shutdown();
        } else if (_allServicesPresent()) {
            _startup();
        }
    }

    public synchronized DatasetRecordService getDatasetRecordService() {
        return _recordService;
    }

    synchronized void setDatasetRecordService(DatasetRecordService service) {
        _recordService = service;
        if (service == null) {
            _shutdown();
        } else if (_allServicesPresent()) {
            _startup();
        }
    }

    public synchronized Set<IDBDatabaseService> getDatabases() {
        return new HashSet<IDBDatabaseService>(_dbServiceSet);
    }

    synchronized void addDatabase(IDBDatabaseService db) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("OsgiDatamanContext.addDatabase(" + db + ")");
        }
        _dbServiceSet.add(db);
        if (_allServicesPresent()) {
            _startup();
        }
    }

    synchronized void removeDatabase(IDBDatabaseService db) {
        _dbServiceSet.remove(db);
        if (!_allServicesPresent()) {
            _shutdown();
        }
    }

    private synchronized void _startup() {
        boolean printMessage = false;

        DatasetCommandProcessor.INSTANCE.start();
        if (_fileListenerReg == null) {
            printMessage = true;
            LOG.fine("Register DatasetFileListener");
            String srvName = DatasetFileListener.class.getName();
            _fileListenerReg = _ctx.registerService(srvName, _fileListener, null);
        }

        if (_recordListenerReg == null) {
            printMessage = true;
            LOG.fine("Register DatasetRecordListener");
            String srvName = DatasetRecordListener.class.getName();
            _recordListenerReg = _ctx.registerService(srvName, _recordListener, null);
        }
        if (_syncFuture == null) {
            printMessage = true;
            _syncFuture = DatasetSync.schedule(this, _user);
        }
        _vigilante.start();
        _mailer.start();

        if (printMessage) LOG.info("Dataman online.");
    }

    private synchronized void _shutdown() {
        boolean printMessage = false;

        _mailer.stop();
        _vigilante.stop();
        if (_fileListenerReg != null) {
            printMessage = true;
            _fileListenerReg.unregister();
            _fileListenerReg = null;
        }
        if (_recordListenerReg != null) {
            printMessage = true;
            _recordListenerReg.unregister();
            _recordListenerReg = null;
        }
        if (_syncFuture != null) {
            printMessage = true;
            _syncFuture.cancel(true);
            _syncFuture = null;
        }
        DatasetCommandProcessor.INSTANCE.stop();
        if (printMessage) LOG.info("Dataman offline.");
    }

    public DatamanState getState() {
        return _state;
    }

    public DatamanConfig getConfig() {
        return _conf;
    }
}
