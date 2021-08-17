package edu.gemini.wdba.server.osgi;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.Site;
import edu.gemini.util.security.principal.StaffPrincipal;
import edu.gemini.wdba.exec.ExecXmlRpcHandler;
import edu.gemini.wdba.fire.FireService;
import edu.gemini.wdba.glue.WdbaGlueService;
import edu.gemini.wdba.glue.api.WdbaContext;
import edu.gemini.wdba.server.WdbaXmlRpcServlet;
import edu.gemini.wdba.session.SessionXmlRpcHandler;
import edu.gemini.wdba.tcc.TccXmlRpcHandler;
import edu.gemini.wdba.xmlrpc.IExecXmlRpc;
import edu.gemini.wdba.xmlrpc.ISessionXmlRpc;
import edu.gemini.wdba.xmlrpc.ITccXmlRpc;
import edu.gemini.wdba.xmlrpc.WdbaConstants;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.servlet.ServletException;
import java.security.Principal;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gemini Observatory/AURA
 * $Id: WDBAServerActivator.java 887 2007-07-04 15:38:49Z gillies $
 */
public class WDBAServerActivator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger(WDBAServerActivator.class.getName());

    private static final String COMMAND_SCOPE    = "osgi.command.scope";
    private static final String COMMAND_FUNCTION = "osgi.command.function";


    private static final String APP_CONTEXT = WdbaConstants.APP_CONTEXT;
    private static final String SITE_KEY = "edu.gemini.site";

    private ServiceTracker<HttpService, HttpService> _httpTracker;
    private ServiceTracker<IDBDatabaseService, WdbaContext> _glueTracker;
    private ServiceTracker<IDBDatabaseService, IDBDatabaseService> _dbTracker;
    private BundleContext _bundleContext;
    private final WdbaXmlRpcServlet _servlet = new WdbaXmlRpcServlet();
    private HttpService _http;

    private ExecutorService fireExecutor;

    public void start(BundleContext bundleContext) throws Exception {
        LOG.info("Start WDBA OSGi Service");

        // We will run as superuser
        final Set<Principal> user = Collections.singleton(StaffPrincipal.Gemini());

        _bundleContext = bundleContext;

        // Track WDBA DatabaseAccess
        _glueTracker = new ServiceTracker<>(_bundleContext, IDBDatabaseService.class.getName(),
                new ServiceTrackerCustomizer<IDBDatabaseService, WdbaContext>() {
                    public WdbaContext addingService(ServiceReference<IDBDatabaseService> ref) {
                        LOG.info("Adding Wdba Access Service");
                        final IDBDatabaseService db = _bundleContext.getService(ref);

                        // Get the site for TCC and Session
                        String siteStr = getProperty(_bundleContext, SITE_KEY);
                        Site site;
                        try {
                            site = Site.parse(siteStr);
                        } catch (Exception ex) {
                            LOG.info("Could not parse site: " + siteStr);
                            throw new RuntimeException("Could not parse site: " + siteStr);
                        }

                        fireExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "FireExecutor"));
                        final FireService fireService = FireService.loggingOnly(db);
                        fireExecutor.execute(fireService);

                        final WdbaContext ctx = new WdbaContext(site, new WdbaGlueService(db, user), user, fireService);
                        ExecXmlRpcHandler.setContext(ctx);
                        TccXmlRpcHandler.setContext(ctx);
                        SessionXmlRpcHandler.setContext(ctx);
                        return ctx;
                    }

                    public void modifiedService(ServiceReference<IDBDatabaseService> ref, WdbaContext object) {
                    }

                    public void removedService(ServiceReference<IDBDatabaseService> ref, WdbaContext object) {
                        LOG.info("Removing WDBA Access Service");
                        fireExecutor.shutdownNow();
                        ExecXmlRpcHandler.setContext(null);
                        TccXmlRpcHandler.setContext(null);
                        SessionXmlRpcHandler.setContext(null);

                    }
                });
        _glueTracker.open();

        _httpTracker = new ServiceTracker<>(_bundleContext, HttpService.class.getName(),
                new ServiceTrackerCustomizer<HttpService, HttpService>() {
                    public HttpService addingService(ServiceReference<HttpService> ref) {
                        LOG.info("Adding HttpService");

                        _http = _bundleContext.getService(ref);
                        try {
                            _http.registerServlet(APP_CONTEXT, _servlet, new Hashtable<>(), null);
                        } catch (ServletException ex) {
                            LOG.log(Level.SEVERE, "Trouble setting up wdba web application.", ex);
                        } catch (NamespaceException ex) {
                            LOG.log(Level.SEVERE, "Trouble setting up wdba web applicaiton.", ex);
                        }
                        return _http;
                    }

                    public void modifiedService(ServiceReference<HttpService> ref, HttpService object) {
                    }

                    public void removedService(ServiceReference<HttpService> ref, HttpService service) {
                        LOG.info("Removing HttpService");
                        XmlRpcHandlerMapping mapping = _servlet.getXmlRpcServletServer().getHandlerMapping();
                        if (mapping instanceof PropertyHandlerMapping) {
                            PropertyHandlerMapping phm = (PropertyHandlerMapping) mapping;
                            phm.removeHandler(IExecXmlRpc.NAME);
                            phm.removeHandler(ITccXmlRpc.NAME);
                            phm.removeHandler(ISessionXmlRpc.NAME);
                        }
                        service.unregister(APP_CONTEXT);
                        _bundleContext.ungetService(ref);
                    }
                });
        _httpTracker.open();

        _dbTracker = new ServiceTracker<>(bundleContext, IDBDatabaseService.class, null);
        _dbTracker.open();

        final Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(COMMAND_SCOPE,    "wdba");
        dict.put(COMMAND_FUNCTION, new String[] { "simVisit", "simWackyVisit" });
        bundleContext.registerService(Commands.class.getName(), new Commands(_dbTracker), dict);
        System.out.println("edu.gemini.wdba.session started.");
    }

    private String getProperty(BundleContext ctx, String key) {
        final String res = ctx.getProperty(key);
        if (res == null) throw new RuntimeException("Missing configuration: " + key);
        return res;
    }

    public void stop(BundleContext bundleContext) throws Exception {
        LOG.info("Stop WDBA OSGi Service");

        _dbTracker.close();
        _dbTracker = null;

        _httpTracker.close();
        _httpTracker = null;

        _glueTracker.close();
        _glueTracker = null;
    }
}
