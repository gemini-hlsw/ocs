//
// $Id: HttpTracker.java 295 2006-02-24 20:54:15Z shane $
//

package edu.gemini.dataman.osgi;

import org.osgi.util.tracker.ServiceTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import javax.servlet.ServletException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Hashtable;

public class HttpTracker extends ServiceTracker {
    private static final Logger LOG = Logger.getLogger(HttpTracker.class.getName());

    private static final String APP_CONTEXT = "/admin";


    public HttpTracker(BundleContext context) {
        super(context, HttpService.class.getName(), null);
    }

    public Object addingService(ServiceReference ref) {
        LOG.info("Adding HttpService");

        HttpService http = (HttpService) context.getService(ref);

        try {
            http.registerServlet(APP_CONTEXT, new DatamanAdminServlet(context), new Hashtable(), null);
        } catch (ServletException ex) {
            LOG.log(Level.SEVERE, "Trouble setting up admin interface.", ex);
        } catch (NamespaceException ex) {
            LOG.log(Level.SEVERE, "Trouble setting up admin interface.", ex);
        }
        return http;
    }

    public void removedService(ServiceReference ref, Object service) {
        HttpService http = (HttpService) service;
        http.unregister(APP_CONTEXT);
        context.ungetService(ref);
    }
}
