package edu.gemini.catalog.osgi;

import jsky.catalog.skycat.SkycatConfigFile;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Bundle activator to set the default skycat.cfg file.
 */
public final class Activator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    public void start(BundleContext ctx) throws Exception {
        LOG.info("start edu.gemini.catalog");
        URL url = ctx.getBundle().getEntry("/jsky/catalog/osgi/skycat.cfg");
        SkycatConfigFile.setConfigFile(url);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        LOG.info("stop edu.gemini.catalog");
    }
}