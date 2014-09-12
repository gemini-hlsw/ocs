package edu.gemini.smartgcal.servlet.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import java.util.logging.Logger;

/**
 * An activator that tracks the HttpService so that it may register the
 * smartgcal config servlet.
 */
public class Activator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private ServiceTracker httpTracker;

    @Override
    public void start(BundleContext ctx) throws Exception {
        LOG.info("Start GCal Config Service");
        httpTracker = new HttpTracker(ctx);
        httpTracker.open();
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
        LOG.info("Stop GCal Config Service");
        httpTracker.close();
        httpTracker = null;
    }
}
