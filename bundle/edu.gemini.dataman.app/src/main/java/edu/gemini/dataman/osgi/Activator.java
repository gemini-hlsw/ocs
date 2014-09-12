//
// $Id: Activator.java 295 2006-02-24 20:54:15Z shane $
//

package edu.gemini.dataman.osgi;

import edu.gemini.dataman.context.DatamanConfig;
import edu.gemini.dataman.context.DatamanContext;
import edu.gemini.dataman.raw.RawCopier;
import edu.gemini.dirmon.DirListener;
import edu.gemini.dirmon.DirLocation;
import edu.gemini.util.security.principal.StaffPrincipal;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 */
public final class Activator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private OsgiDatamanContext _dmanContext;
    private WorkingStoreTracker _wsTracker;
    private HttpTracker _httpTracker;

    private final Set<Principal> _user = Collections.<Principal>singleton(StaffPrincipal.Gemini());

    public void start(BundleContext ctx) throws OsgiConfigException, IOException {
        LOG.fine("start dataman-app bundle");

        _httpTracker = new HttpTracker(ctx);
        _httpTracker.open();

        _dmanContext = new OsgiDatamanContext(ctx, _user);
        _dmanContext.start();

        RawCopier rc = _registerRawCopier(ctx, _dmanContext);
        String path = _dmanContext.getConfig().getWorkDir().getPath();
        _wsTracker = new WorkingStoreTracker(ctx, path, rc);
        _wsTracker.start();
    }

    private static RawCopier _registerRawCopier(BundleContext ctx, DatamanContext dctx) {
        DatamanConfig conf = dctx.getConfig();
        LOG.fine("Register DirListener for: " + conf.getRawDir());

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(DirLocation.DIR_PATH_PROP, conf.getRawDir().getPath());
        RawCopier rc = new RawCopier(dctx);
        String name = DirListener.class.getName();
        ctx.registerService(name, rc, props);
        return rc;
    }

    public void stop(BundleContext ctx) throws Exception {
        LOG.fine("stop dataman-app bundle");
        _wsTracker.stop();
        _wsTracker = null;

        _dmanContext.stop();
        _dmanContext = null;

        _httpTracker.close();
        _httpTracker = null;
    }
}
