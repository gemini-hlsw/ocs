//
// $Id: OsgiMailConfig.java 893 2007-07-19 19:43:20Z swalker $
//

package edu.gemini.auxfile.workflow.osgi;

import edu.gemini.auxfile.copier.AuxFileType;
import edu.gemini.auxfile.workflow.MailConfig;
import edu.gemini.auxfile.workflow.AddressFetcher;
import org.osgi.framework.BundleContext;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

/**
 *
 */
final class OsgiMailConfig implements MailConfig {
    private static final Logger LOG = Logger.getLogger(OsgiMailConfig.class.getName());

    private static final String GENERIC_MAIL_CONFIG_PREFIX = "edu.gemini.oodb.mail";
    private static final String CONFIG_PREFIX = "edu.gemini.auxfile";

    private static final String SMTP_HOST_KEY    = "smtpHost";
    private static final String SENDER_ADDR_KEY  = "senderAddr";
    private static final String SENDER_NAME_KEY  = "senderName";


    private String _smtpHost;
    private InternetAddress _sender;
    private List<InternetAddress> _finderRecipients;
    private List<InternetAddress> _maskRecipients;

    OsgiMailConfig(BundleContext ctx) {
        _smtpHost   = getProperty(ctx, GENERIC_MAIL_CONFIG_PREFIX, SMTP_HOST_KEY);

        String addr = getProperty(ctx, GENERIC_MAIL_CONFIG_PREFIX, SENDER_ADDR_KEY);
        String name = getProperty(ctx, GENERIC_MAIL_CONFIG_PREFIX, SENDER_NAME_KEY);

        try {
            _sender = new InternetAddress(addr);
            _sender.setPersonal(name);
        } catch (AddressException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Bad value for property '" +
                               SENDER_ADDR_KEY + "': " + addr);
        } catch (UnsupportedEncodingException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException("Bad value for property '" +
                               SENDER_NAME_KEY + "': " + name);
        }

        addr = ctx.getProperty(CONFIG_PREFIX + "." + AuxFileType.other.name() + ".mail");
        _finderRecipients = AddressFetcher.parseAddresses(addr, null);

        addr = ctx.getProperty(CONFIG_PREFIX + "." + AuxFileType.fits.name() + ".mail");
        _maskRecipients = AddressFetcher.parseAddresses(addr, null);
    }

    private String getOptionalProperty(BundleContext ctx, String prefix, String key) {
        return ctx.getProperty(prefix + '.' + key);
    }

    private String getProperty(BundleContext ctx, String prefix, String key) {
        String res = getOptionalProperty(ctx, prefix, key);
        if (res == null) throw new RuntimeException("Missing configuration: " + prefix + "." + key);
        return res;
    }

    public String getSmtpHost() {
        return _smtpHost;
    }

    public InternetAddress getSender() {
        return _sender;
    }

    public List<InternetAddress> getFinderRecipients() {
        return _finderRecipients;
    }

    public List<InternetAddress> getMaskRecipients() {
        return _maskRecipients;
    }
}
