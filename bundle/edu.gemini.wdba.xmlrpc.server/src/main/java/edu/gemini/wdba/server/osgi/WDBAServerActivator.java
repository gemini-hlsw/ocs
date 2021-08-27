package edu.gemini.wdba.server.osgi;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.event.ExecEvent;
import edu.gemini.util.security.principal.StaffPrincipal;
import edu.gemini.wdba.exec.ExecXmlRpcHandler;
import edu.gemini.wdba.fire.FireService;
import edu.gemini.wdba.glue.WdbaGlueService;
import edu.gemini.wdba.glue.api.WdbaContext;
import edu.gemini.wdba.server.WdbaXmlRpcServlet;
import edu.gemini.wdba.session.DBUpdateService;
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
import java.net.URL;
import java.security.Principal;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.function.Consumer;
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
    private static final String FIRE_URL = "edu.gemini.fire.url";

    private ServiceTracker<HttpService, HttpService> _httpTracker;
    private ServiceTracker<IDBDatabaseService, WdbaContext> _glueTracker;
    private ServiceTracker<IDBDatabaseService, IDBDatabaseService> _dbTracker;
    private BundleContext _bundleContext;
    private final WdbaXmlRpcServlet _servlet = new WdbaXmlRpcServlet();
    private HttpService _http;

    private DBUpdateService dbUpdateService;
    private Option<FireService> fireService;

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
                        final Site site = parseSite(_bundleContext);

                        final WdbaContext ctx = new WdbaContext(site, new WdbaGlueService(db, user), user);

                        final Option<URL> fireUrl = parseFireUrl(_bundleContext);
                        if (fireUrl.isEmpty()) {
                            LOG.warning("Missing FIRE URL property '" + FIRE_URL + "', no FIRE messages will be posted.");
                        } else {
                            fireUrl.foreach(u -> LOG.info("Using FIRE URL: " + u.toString()));
                        }

                        fireService     = fireUrl.map(url -> FireService.posting(db, url));
                        dbUpdateService = new DBUpdateService(ctx);

                        // Define what happens when the session receives an
                        // ExecEvent.  This is configurable so that it is easier
                        // to change for testing.  DBUpdateService records the
                        // event in the ODB in a separate thread and when it is
                        // finished with that, a FireMessage is produced and
                        // posted in another thread.
                        final Consumer<ExecEvent> eventConsumer = e -> {
                            try {
                                dbUpdateService
                                    .handleEvent(e)
                                    .whenCompleteAsync((maybeEvent, maybeException) ->
                                        ImOption.apply(maybeEvent)
                                                .foreach(evt -> fireService.foreach(s -> s.handleEvent(evt)))
                                    );
                            } catch (InterruptedException ex) {
                                LOG.info("Interrupted while processing exec event: " + e);
                            }
                        };

                        ExecXmlRpcHandler.setContext(ctx);
                        TccXmlRpcHandler.setContext(ctx);
                        SessionXmlRpcHandler.setContext(ctx, eventConsumer);

                        fireService.foreach(FireService::start);
                        dbUpdateService.start();

                        return ctx;
                    }

                    public void modifiedService(ServiceReference<IDBDatabaseService> ref, WdbaContext object) {
                    }

                    public void removedService(ServiceReference<IDBDatabaseService> ref, WdbaContext object) {
                        LOG.info("Removing WDBA Access Service");
                        dbUpdateService.stop();
                        fireService.foreach(FireService::stop);
                        ExecXmlRpcHandler.setContext(null);
                        TccXmlRpcHandler.setContext(null);
                        SessionXmlRpcHandler.setContext(null, e -> {});

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

    private static String getProperty(BundleContext ctx, String key) {
        final String res = ctx.getProperty(key);
        if (res == null) throw new RuntimeException("Missing configuration: " + key);
        return res;
    }

    private static Option<String> getOptionalProperty(BundleContext ctx, String key) {
        return ImOption.apply(ctx.getProperty(key));
    }

    private static Site parseSite(BundleContext ctx) {
        final String s = getProperty(ctx, SITE_KEY);
        try {
            return Site.parse(s);
        } catch (Exception ex) {
            final String message = "Could not parse '" + SITE_KEY + "' key: " + ex.getMessage();
            LOG.log(Level.WARNING, message, ex);
            throw new RuntimeException(message, ex);
        }
    }

    private static Option<URL> parseFireUrl(BundleContext ctx) {
        final Option<String> urlStr = getOptionalProperty(ctx, FIRE_URL);
        return urlStr.flatMap(s -> {
            try {
                return new Some<>(new URL(s));
            } catch (Exception ex) {
                final String message = "Could not parse '" + FIRE_URL + "' key: " + ex.getMessage();
                LOG.log(Level.WARNING, message, ex);
                return None.instance();
            }
        });
    }

    public void stop(BundleContext bundleContext) {
        LOG.info("Stop WDBA OSGi Service");

        _dbTracker.close();
        _dbTracker = null;

        _httpTracker.close();
        _httpTracker = null;

        _glueTracker.close();
        _glueTracker = null;
    }
}
