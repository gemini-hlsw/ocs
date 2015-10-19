//
// $Id: Activator.java 295 2006-02-24 20:54:15Z shane $
//

package edu.gemini.dataman.osgi;

//import edu.gemini.dataman.context.DatamanConfig;
//import edu.gemini.dataman.context.DatamanContext;
//import edu.gemini.dataman.raw.RawCopier;
//import edu.gemini.dirmon.DirListener;
//import edu.gemini.dirmon.DirLocation;
//import edu.gemini.util.security.principal.StaffPrincipal;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.core.osgi.SiteProperty;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

//import java.io.IOException;
//import java.security.Principal;
//import java.util.*;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 *
 */
public final class Activator implements BundleActivator {

    public static final String COMMAND_SCOPE    = "osgi.command.scope";
    public static final String COMMAND_FUNCTION = "osgi.command.function";

    public static final String GSA_HOST         = "edu.gemini.dataman.gsa.query.host";
    public static final String GSA_AUTH         = "edu.gemini.dataman.gsa.query.auth";

    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    // Commenting out the old Data Manager activator functionality.  This will
    // be reviewed and restored or replaced in subsequent PRs.  For now, I want
    // the new things to work without trying to start any of the old Data Man.
    // Actually I may just convert the whole thing to Scala.

//    private OsgiDatamanContext _dmanContext;
//    private WorkingStoreTracker _wsTracker;
//    private HttpTracker _httpTracker;
//
//    private final Set<Principal> _user = Collections.<Principal>singleton(StaffPrincipal.Gemini());
//    private final List<BundleActivator> _delegates = new ArrayList<>();

    private String requiredProp(BundleContext ctx, String propName) {
        final String prop = ctx.getProperty(propName);
        if (prop == null) {
            LOG.warning("Data Manager property `" + propName + "` missing.  Data Manager not started.");
            throw new RuntimeException("Missing " + propName + " in app configuration.");
        }
        return prop;
    }

    public void start(BundleContext ctx) throws Exception {
        LOG.fine("start dataman-app bundle");

        final String gsaHost = requiredProp(ctx, GSA_HOST);
        LOG.info("Data Manager GSA host is " + gsaHost);

        final String gsaAuth = requiredProp(ctx, GSA_AUTH);

        final Site site = SiteProperty.get(ctx);
        if (site == null) {
            LOG.warning("Data Manager could not determine the site.  Not started.");
            throw new RuntimeException("Missing or not parseable " + SiteProperty.NAME + " property.");
        } else {
            LOG.info("Data Manager site is " + site.displayName);
        }

        final Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(COMMAND_SCOPE, "gsa");
        dict.put(COMMAND_FUNCTION, new String[]{"gsa"});

        // TODO: we don't have a GS GSA test server or GS data so fix to GN for now
        ctx.registerService(GsaCommands.class, GsaCommands$.MODULE$.apply(gsaHost, Site.GN, gsaAuth), dict);

//        _delegates.add(new edu.gemini.datasetfile.osgi.Activator());
//        _delegates.add(new edu.gemini.dirmon.impl.osgi.Activator());
//        _delegates.add(new edu.gemini.datasetrecord.osgi.Activator());
//        for (BundleActivator ba: _delegates)
//            ba.start(ctx);
//
//        _httpTracker = new HttpTracker(ctx);
//        _httpTracker.open();
//
//        _dmanContext = new OsgiDatamanContext(ctx, _user);
//        _dmanContext.start();
//
//        RawCopier rc = _registerRawCopier(ctx, _dmanContext);
//        String path = _dmanContext.getConfig().getWorkDir().getPath();
//        _wsTracker = new WorkingStoreTracker(ctx, path, rc);
//        _wsTracker.start();
    }

//    private static RawCopier _registerRawCopier(BundleContext ctx, DatamanContext dctx) {
//        DatamanConfig conf = dctx.getConfig();
//        LOG.fine("Register DirListener for: " + conf.getRawDir());
//
//        Hashtable<String, Object> props = new Hashtable<String, Object>();
//        props.put(DirLocation.DIR_PATH_PROP, conf.getRawDir().getPath());
//        RawCopier rc = new RawCopier(dctx);
//        String name = DirListener.class.getName();
//        ctx.registerService(name, rc, props);
//        return rc;
//    }

    public void stop(BundleContext ctx) throws Exception {
        LOG.fine("stop dataman-app bundle");

//        for (BundleActivator ba: _delegates)
//            ba.stop(ctx);
//        _delegates.clear();
//
//        _wsTracker.stop();
//        _wsTracker = null;
//
//        _dmanContext.stop();
//        _dmanContext = null;
//
//        _httpTracker.close();
//        _httpTracker = null;
    }
}
