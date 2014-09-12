package edu.gemini.ags.servlet.osgi;

import edu.gemini.ags.servlet.AgsServlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

import javax.servlet.ServletException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Activator implements BundleActivator {
    private static final String APP_CONTEXT = "/ags";

    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private final class HttpTracker extends ServiceTracker<HttpService, HttpService> {
        public HttpTracker(BundleContext context) {
            super(context, HttpService.class.getName(), null);
        }

        @Override public HttpService addingService(ServiceReference<HttpService> ref) {
            LOG.info("Adding HttpService");
            final HttpService http = context.getService(ref);

            try {
                http.registerServlet(APP_CONTEXT, new AgsServlet(), new Hashtable(), null);
            } catch (ServletException ex) {
                LOG.log(Level.SEVERE, "Trouble setting up web application.", ex);
            } catch (NamespaceException ex) {
                LOG.log(Level.SEVERE, "Trouble setting up web applicaiton.", ex);
            }
            return http;
        }

        @Override public void removedService(ServiceReference<HttpService> ref, HttpService http) {
            LOG.info("Remove HttpService");
            http.unregister(APP_CONTEXT);
            context.ungetService(ref);
        }
    }

    private HttpTracker httpTracker;

    @Override public void start(BundleContext ctx) throws Exception {
        LOG.info("Start AGS Servlet");
        httpTracker = new HttpTracker(ctx);
        httpTracker.open();
    }

    @Override public void stop(BundleContext ctx) throws Exception {
        LOG.info("Stop AGS Servlet");
        httpTracker.close();
        httpTracker = null;
    }
}
