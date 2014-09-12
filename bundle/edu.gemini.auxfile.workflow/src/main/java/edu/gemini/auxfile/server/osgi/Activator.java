package edu.gemini.auxfile.server.osgi;

import edu.gemini.auxfile.api.AuxFileListener;
import edu.gemini.auxfile.server.AuxFileServer;
import edu.gemini.auxfile.server.file.BackendFileSystemImpl;
import edu.gemini.auxfile.server.file.FileManager;
import edu.gemini.auxfile.server.file.TransferDirCleaner;
import edu.gemini.auxfile.server.notify.NotifyingBackend;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.util.Hashtable;
import java.util.logging.Logger;

public final class Activator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private static final String ROOT_DIR_PROP = "edu.gemini.auxfile.root";

    private ServiceTracker<AuxFileListener, AuxFileListener> listenerTracker;
    private ServiceRegistration<AuxFileServer> serviceReg;

    public void start(BundleContext ctx) throws Exception {
        LOG.info("Start Auxfile Server");

        final String dirStr = ctx.getProperty(ROOT_DIR_PROP);
        if (dirStr == null) {
            final String msg = "Missing '" + ROOT_DIR_PROP + "' property";
            LOG.warning(msg);
            throw new RuntimeException(msg);
        }

        final File rootDir = new File(dirStr);
        FileManager.init(rootDir);

        final AuxFileServer rawServer = new BackendFileSystemImpl();
        final NotifyingBackend server = new NotifyingBackend(rawServer);

        TransferDirCleaner.start();

        listenerTracker = new ListenerTracker(ctx, server);
        listenerTracker.open();

        LOG.info("Register " + AuxFileServer.class);
        final Hashtable<String, String> props = new Hashtable<String,String>();
        props.put("trpc", "");
        serviceReg = ctx.registerService(AuxFileServer.class, server, props);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        LOG.info("Stop Auxfile Server");
        serviceReg.unregister();

        listenerTracker.close();
        listenerTracker = null;

        TransferDirCleaner.stop();
    }
}
