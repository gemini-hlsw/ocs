package edu.gemini.spModel.core.osgi;

import edu.gemini.spModel.core.Site;
import org.osgi.framework.BundleContext;
import java.util.logging.Logger;

/**
 * Support for parsing an edu.gemini.site bundle property into a Site object.
 */
public final class SiteProperty {
    private static final Logger LOG = Logger.getLogger(SiteProperty.class.getName());

    public static final String NAME = "edu.gemini.site";

    // defeat instantiation
    private SiteProperty() {}

    /**
     * Gets the value of the <code>edu.gemini.site</code> property if defined,
     * <code>null</code> otherwise.
     */
    public static Site get(BundleContext ctx) {
        final String strValue = ctx.getProperty(NAME);
        if (strValue == null) {
            LOG.warning(String.format("%s property not specified", NAME));
            return null;
        }

        final Site res = Site.tryParse(strValue);
        if (res == null) {
            LOG.warning(String.format("Could not parse '%s' as a Gemini site", strValue));
        }
        return res;
    }
}
