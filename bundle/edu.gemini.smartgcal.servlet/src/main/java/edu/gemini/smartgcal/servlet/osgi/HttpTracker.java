package edu.gemini.smartgcal.servlet.osgi;

import edu.gemini.smartgcal.servlet.SmartGcalConfigServlet;
import edu.gemini.spModel.core.OcsVersionUtil;
import edu.gemini.util.osgi.ExternalStorage$;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

import javax.servlet.ServletException;
import java.io.File;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HttpService tracker.  When the HttpService is available, it registers the
 * smartgcal configuration servlet so that it can begin to handle requests.
 */
public class HttpTracker extends ServiceTracker<HttpService, HttpService> {
    private static final Logger LOG = Logger.getLogger(HttpTracker.class.getName());

    private static final String APP_CONTEXT = "/gcal";
    private static final String PROPERTY_CONTEXT = "edu.gemini.smartgcal.";
    private static final String SVN_ROOT_URL_PROP = "svnRootUrl";
    private static final String SVN_USER_PROP = "svnUser";
    private static final String SVN_PASSWORD_PROP = "svnPassword";
    private static final String UPLOAD_PASSWORD = "uploadPassword";

    public HttpTracker(BundleContext ctx) {
        super(ctx, HttpService.class.getName(), null);
    }

    private static final File dir(BundleContext ctx) {
        final String dbDirStr = ctx.getProperty("edu.gemini.spdb.dir");
        final File dbDir = (dbDirStr == null) ?
                ExternalStorage$.MODULE$.getExternalDataFile(ctx, "spdb") :
                new File(dbDirStr);
        return new File(OcsVersionUtil.getVersionDir(dbDir, Version.current), "gcalServlet");
    }

    public HttpService addingService(ServiceReference ref) {
        HttpService http = (HttpService) context.getService(ref);

        String svnRootUrl     = getProperty(SVN_ROOT_URL_PROP);
        String svnUser        = getProperty(SVN_USER_PROP);
        String svnPassword    = getProperty(SVN_PASSWORD_PROP);
        String uploadPassword = getProperty(UPLOAD_PASSWORD);
        String fileCachePath  = dir(context).getPath();

        try {
            http.registerServlet(
                    APP_CONTEXT,
                    new SmartGcalConfigServlet(svnRootUrl, svnUser, svnPassword, uploadPassword, fileCachePath),
                    new Hashtable(),
                    null);

        } catch (ServletException ex) {
            LOG.log(Level.SEVERE, "Trouble setting up servlet.", ex);
        } catch (NamespaceException ex) {
            LOG.log(Level.SEVERE, "Trouble setting up servlet.", ex);
        }

        return http;
    }

    public void removedService(ServiceReference ref, HttpService http) {
        http.unregister(APP_CONTEXT);
        context.ungetService(ref);
    }

    private String getProperty(String name) {
        String fullName = PROPERTY_CONTEXT+name;
        String property = context.getProperty(fullName);
        if (property == null)  LOG.log(Level.WARNING, String.format("Property %s is not defined.", fullName));
        return property;
    }
}
