package edu.gemini.horizons.server.osgi;

import java.util.Hashtable;
import java.util.logging.Logger;

import edu.gemini.horizons.api.IQueryExecutor;
import edu.gemini.horizons.server.backend.CgiQueryExecutor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private ServiceRegistration<IQueryExecutor> reg;

    public void start(BundleContext ctx) throws Exception {
        Activator.LOG.info("Start Horizons Server");
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("trpc", "");
        reg = ctx.registerService(IQueryExecutor.class, CgiQueryExecutor.instance, props);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        Activator.LOG.info("Stop Horizons Server");
        if (reg != null) {
            reg.unregister();
            reg = null;
        }
    }
}
