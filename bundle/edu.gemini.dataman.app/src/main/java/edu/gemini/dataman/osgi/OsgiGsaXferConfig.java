//
// $Id$
//

package edu.gemini.dataman.osgi;

import org.osgi.framework.BundleContext;

import edu.gemini.dataman.context.GsaXferConfig;

/**
 * Simple Dataman SSH configuration properties.
 */
final class OsgiGsaXferConfig extends OsgiXferConfig implements GsaXferConfig {
    private String _mdIngestScript;
    private String _cadcRoot;
    private String _cadcGroup;

    OsgiGsaXferConfig(BundleContext ctx, String key) throws OsgiConfigException {
    	super(ctx, key);
    	_mdIngestScript = getProp(ctx, XFER_MD_INGEST_SCRIPT, key);
        _cadcRoot       = getProp(ctx, XFER_CADC_ROOT, key);
        _cadcGroup      = getProp(ctx, XFER_CADC_GROUP, key);
    }

    public String getMdIngestScript() {
            return _mdIngestScript;
    }

    public String getCadcRoot() {
            return _cadcRoot;
    }

    public String getCadcGroup() {
        return _cadcGroup;
    }
}
