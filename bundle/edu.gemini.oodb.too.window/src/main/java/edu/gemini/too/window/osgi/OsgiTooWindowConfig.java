//
// $
//

package edu.gemini.too.window.osgi;

import edu.gemini.spModel.core.Site;
import edu.gemini.too.window.TooWindowConfig;
import org.osgi.framework.BundleContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class OsgiTooWindowConfig implements TooWindowConfig {
    private static final Logger LOG = Logger.getLogger(OsgiTooWindowConfig.class.getName());

    private static final String SITE_KEY = "edu.gemini.site";

    private Site _site;

    OsgiTooWindowConfig(BundleContext ctx) {
        String siteStr = getProperty(ctx, SITE_KEY);
        try {
            _site = Site.parse(siteStr);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException("Bad value for property '" +
                               SITE_KEY + "': " + siteStr);
        }
    }

    public Site getSite() {
        return _site;
    }

    private String getProperty(BundleContext ctx, String key) {
        String res = ctx.getProperty(key);
        if (res == null) {
            throw new RuntimeException("Missing configuration: " + key);
        }

        return res;
    }
}
