//
// $Id: OsgiXferConfig.java 145 2005-09-26 21:49:16Z rnorris $
//

package edu.gemini.dataman.osgi;

import edu.gemini.dataman.context.XferConfig;
import edu.gemini.util.ssh.SshConfig$;
import org.osgi.framework.BundleContext;
import edu.gemini.util.ssh.SshConfig$;

/**
 * Simple Dataman SSH configuration properties.
 */
class OsgiXferConfig implements XferConfig {
    private String _user;
    private String _host;
    private String _pass;
    private String _tempDir;
    private String _destDir;

    OsgiXferConfig(BundleContext ctx, String key) throws OsgiConfigException {
        _user = getProp(ctx, XFER_USER_PREFIX, key);
        _host = getProp(ctx, XFER_HOST_PREFIX, key);
        _pass = getProp(ctx, XFER_PASS_PREFIX, key);

        _tempDir = getProp(ctx, XFER_TEMP_DIR_PREFIX, key);
        _destDir = getProp(ctx, XFER_DEST_DIR_PREFIX, key);
    }

    protected String getProp(BundleContext ctx, String prefix, String key)
            throws OsgiConfigException {
        String fullPropName = prefix + '.' + key;
        String val = ctx.getProperty(fullPropName);
        if (val == null) {
            throw new OsgiConfigException("missing value for: " + fullPropName);
        }
        return val;
    }


    public String getUser() {
        return _user;
    }

    public String getHost() {
        return _host;
    }

    public String getPassword() {
        return _pass;
    }

    public String getTempDir() {
        return _tempDir;
    }

    public int getTimeout() {
        return SshConfig$.MODULE$.DEFAULT_TIMEOUT();
    }

    public String getDestDir() {
        return _destDir;
    }
}
