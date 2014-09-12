//
// $Id: OsgiDatamanConfig.java 245 2006-01-03 18:52:39Z shane $
//

package edu.gemini.dataman.osgi;

import edu.gemini.dataman.context.DatamanConfig;
import edu.gemini.dataman.context.GsaXferConfig;
import edu.gemini.dataman.context.XferConfig;
import edu.gemini.dataman.context.GsaUrl;
import edu.gemini.file.util.osgi.OsgiPatternFileFilterFactory;
import org.osgi.framework.BundleContext;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Simple Dataman configuration properties.
 */
final class OsgiDatamanConfig implements DatamanConfig {
    private static final Logger LOG = Logger.getLogger(OsgiDatamanConfig.class.getName());

    public static final long DEFAULT_SCAN_TIME = 30 * 60 * 1000; // 30 min.

    private FileFilter _filter;
    private BundleContext _ctx;
    private XferConfig _baseXfer;
    private GsaXferConfig _gsaXfer;
    private GsaUrl _gsaCrcUrl;
    private GsaUrl _gsaXferUrl;
    private URL _gsaAllXferUrl;
    private Collection<InternetAddress> _checkCc;
    private long _scanTime;

    OsgiDatamanConfig(BundleContext ctx) throws OsgiConfigException {
        _ctx = ctx;

        _filter     = OsgiPatternFileFilterFactory.create(ctx);
        //REL-182: transfer to base should be optional
        try{
            _baseXfer   = new OsgiXferConfig(ctx, "base");
        }   catch(OsgiConfigException ex){
            _baseXfer = null;
        }
        _gsaXfer = new OsgiGsaXferConfig(ctx, "gsa");
        _gsaCrcUrl  = new GsaUrl(_ctx.getProperty(GSA_CRC_URL_PROP));

        String template = _ctx.getProperty(GSA_XFER_URL_PROP);
        _gsaXferUrl = new GsaUrl(template);

        template = template.replace("file=%FILE%", "");
        template = template.replaceAll("&&", "&");
        try {
            _gsaAllXferUrl = new URL(template);
        } catch (MalformedURLException ex) {
            throw new OsgiConfigException("Malformed URL: " + template);
        }

        _checkCc    = _parseCheckMailCc();
        _scanTime   = _parseScanTime();


        // Make sure every property has a value.  This will call methods
        // before the class is constructed, but the only state they require
        // is the BundleContext and this class is final so none of them
        // will be overriden.

        Method[] meths = DatamanConfig.class.getDeclaredMethods();
        for (Method m : meths) {
            Class[] params = m.getParameterTypes();
            if (params.length > 0) continue;

            try {
                Object res = m.invoke(this);
                if ((res == null) && (!"getBaseXferConfig".equals(m.getName()))) {
                    String propName = m.getName().substring(3);
                    throw new OsgiConfigException("Missing property: " +
                                                     propName);
                }
            } catch (Exception ex) {
                throw OsgiConfigException.newException(ex);
            }
        }
    }

    private File _getFileProp(String name) {
        String str = _ctx.getProperty(name);
        if (str == null) return null;
        return new File(str);
    }

    public File getRawDir() {
        return _getFileProp(RAW_DIR_PROP);
    }

    public File getWorkDir() {
        return _getFileProp(WORK_DIR_PROP);
    }

    public FileFilter getFileFilter() {
        return _filter;
    }

    public XferConfig getBaseXferConfig() {
        return _baseXfer;
    }

    public GsaXferConfig getGsaXferConfig() {
        return _gsaXfer;
    }

    public GsaUrl getGsaCrcUrl() {
        return _gsaCrcUrl;
    }

    public GsaUrl getGsaXferStatusUrl() {
        return _gsaXferUrl;
    }

    public URL getGsaAllXferStatusUrl() {
        return _gsaAllXferUrl;
    }

    public String getSmtpHost() {
        return _ctx.getProperty(SMTP_HOST_PROP);
    }

    public Collection<InternetAddress> getCheckMailCc() {
        return _checkCc;
    }

    public long getOdbScanTime() {
        return _scanTime;
    }

    private Collection<InternetAddress> _parseCheckMailCc() {
        String ccList = _ctx.getProperty(CHECK_MAIL_CC);
        if (ccList == null) return Collections.emptyList();

        StringTokenizer st = new StringTokenizer(ccList, " \t,;", false);
        List<InternetAddress> lst = new ArrayList<InternetAddress>();
        while (st.hasMoreTokens()) {
            String addrStr = st.nextToken();
            InternetAddress addr;
            try {
                addr = new InternetAddress(addrStr);
                addr.validate();
            } catch (AddressException e) {
                LOG.warning("illegal email address: " + addrStr);
                continue;
            }
            lst.add(addr);
        }
        return Collections.unmodifiableList(lst);
    }

    private long _parseScanTime() throws OsgiConfigException {
        String val = _ctx.getProperty(SCAN_TIME_PROP);
        if (val == null) return DEFAULT_SCAN_TIME;
        try {
            // specified in minutes, convert to milliseconds
            return Long.parseLong(val) * 60 * 1000;
        } catch (Exception ex) {
            throw OsgiConfigException.newException(ex);
        }
    }
}
