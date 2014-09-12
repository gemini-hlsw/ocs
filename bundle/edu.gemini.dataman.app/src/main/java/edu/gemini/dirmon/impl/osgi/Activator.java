//
// $Id: Activator.java 53 2005-08-31 04:10:57Z shane $
//

package edu.gemini.dirmon.impl.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.net.InetAddress;
import java.util.logging.Logger;


/**
 *
 */
public final class Activator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private DirListenerTracker _dirTracker;


    public void start(BundleContext bundleContext) throws Exception {
        LOG.info("Start dirmon-server bundle");

        // Record our address.
        String addr = InetAddress.getLocalHost().getHostAddress();

        // Start watching for dir listeners.
        _dirTracker = new DirListenerTracker(bundleContext, addr);
        _dirTracker.start();
    }

    public void stop(BundleContext bundleContext) throws Exception {
        LOG.info("Stop dirmon-server bundle");
        _dirTracker.stop();
    }
}
