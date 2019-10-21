package edu.gemini.too.email.osgi;

import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.core.Version;
import edu.gemini.too.email.TooEmailConfig;
import org.osgi.framework.BundleContext;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;
import java.net.URL;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.logging.Level;

final class OsgiEmailConfig implements TooEmailConfig {
    private static final Logger LOG = Logger.getLogger(OsgiEmailConfig.class.getName());

    private static final String GENERIC_MAIL_CONFIG_PREFIX = "edu.gemini.oodb.mail";
    private static final String SMTP_HOST_KEY    = GENERIC_MAIL_CONFIG_PREFIX + ".smtpHost";
    private static final String SENDER_ADDR_KEY  = GENERIC_MAIL_CONFIG_PREFIX + ".senderAddr";
    private static final String SENDER_NAME_KEY  = GENERIC_MAIL_CONFIG_PREFIX + ".senderName";

    private static final String SITE_KEY = "edu.gemini.site";

    private final String _smtpHost;
    private final InternetAddress _sender;
    private final Site _site;

    OsgiEmailConfig(BundleContext ctx) {
        _smtpHost   = getProperty(ctx, SMTP_HOST_KEY);

        final String addr = getProperty(ctx, SENDER_ADDR_KEY);
        final String name = getProperty(ctx, SENDER_NAME_KEY);

        try {
            _sender = new InternetAddress(addr);
            _sender.setPersonal(name);
        } catch (final AddressException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Bad value for property '" +
                               SENDER_ADDR_KEY + "': " + addr);
        } catch (final UnsupportedEncodingException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Bad value for property '" +
                               SENDER_NAME_KEY + "': " + name);
        }

        final String siteStr = getProperty(ctx, SITE_KEY);
        try {
            _site = Site.parse(siteStr);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException("Bad value for property '" +
                               SITE_KEY + "': " + siteStr);
        }
    }

    public String getSmtpServer() {
        return _smtpHost;
    }

    public InternetAddress getSender() {
        return _sender;
    }

    public Site getSite() {
        return _site;
    }

    public URL getEmailConfig() {
        // If we are testing, use the testing email configurations.
        // If we are in production, use the production email configurations.
        final String test = Version.current.isTest() ? "-test" : "";
        return getClass().getClassLoader().getResource("/resources/emailConf" + test + ".xml");
    }

    private String getProperty(BundleContext ctx, String key) {
        String res = ctx.getProperty(key);
        if (res == null) {
            throw new RuntimeException("Missing configuration: " + key);
        }

        return res;
    }
}
