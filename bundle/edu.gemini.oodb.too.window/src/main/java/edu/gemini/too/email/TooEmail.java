//
// $Id: TooEmail.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.too.email;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.shared.mail.MailTemplate;
import edu.gemini.shared.mail.MailUtil;
import edu.gemini.shared.util.GeminiRuntimeException;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TooEmail {
    private static final Logger LOG = Logger.getLogger(TooEmail.class.getName());

    private static final String FROM_ADDR      = "noreply@gemini.edu";
    private static final String FROM_NAME      = "Gemini ODB";

    private enum DestinationType {
        to, cc, bcc
    }

    private final class Key {
        private final TooType too;
        private final DestinationType dest;

        Key(TooType too, DestinationType dest) {
            if (too  == null) throw new IllegalArgumentException();
            if (dest == null) throw new IllegalArgumentException();

            this.too  = too;
            this.dest = dest;
        }

        TooType getTooType() { return too; }
        DestinationType getDestinationType() { return dest; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (dest != key.dest) return false;
            return too == key.too;

        }

        @Override
        public int hashCode() {
            int result = too.hashCode();
            result = 31 * result + dest.hashCode();
            return result;
        }
    }

    private static final String TOO_TYPE_ATTRIBUTE = "too";
    private static final String SITE_ATTRIBUTE = "site";
    private static final String TO_TAG         = "to";
    private static final String CC_TAG         = "cc";
    private static final String BCC_TAG        = "bcc";
    private static final String SUBJECT_TAG    = "subject";
    private static final String BODY_TAG       = "body";

    private static final String TOO_TYPE_PROP  = "TOO_TYPE";
    private static final String OBS_ID_PROP    = "OBS_ID";
    private static final String OBS_NAME_PROP  = "OBS_NAME";
    private static final String PROG_NAME_PROP = "PROG_NAME";


    private static InternetAddress _getSenderAddress() {
        InternetAddress addr;
        try {
            addr = new InternetAddress(FROM_ADDR);
            addr.setPersonal(FROM_NAME);
        } catch (MessagingException ex) {
            LOG.log(Level.WARNING, "Invalid sender address: " + FROM_ADDR, ex);
            throw GeminiRuntimeException.newException("Invalid sender address: " +
                                                      FROM_ADDR, ex);
        } catch (UnsupportedEncodingException ex) {
            LOG.log(Level.WARNING, "Invalid sender name: " + FROM_NAME, ex);
            throw GeminiRuntimeException.newException("Invalid sender name: " +
                                                      FROM_NAME, ex);
        }
        return addr;
    }

    private static Document _getEmailDocument(TooEmailConfig conf) {
        URL emailFile = conf.getEmailConfig();

        SAXReader reader = new SAXReader();
        Document doc;
        try {
            doc = reader.read(emailFile);
        } catch (org.dom4j.DocumentException ex) {
            throw GeminiRuntimeException.newException("Could not load email configuration", ex);
        }
        return doc;
    }

    private static InternetAddress[] EMPTY_ADDRESS_ARRAY = new InternetAddress[0];

    private static InternetAddress[] _getAddresses(Element addrElement, TooType too, Site site) {
        String tooStr = addrElement.attributeValue(TOO_TYPE_ATTRIBUTE);
        if (!tooTypeMatches(tooStr, too)) return EMPTY_ADDRESS_ARRAY;

        String location = addrElement.attributeValue(SITE_ATTRIBUTE);
        if (!locationMatches(location, site)) return EMPTY_ADDRESS_ARRAY;

        String addrStr = addrElement.getTextTrim();
        if ((addrStr == null) || ("".equals(addrStr))) {
            return EMPTY_ADDRESS_ARRAY;
        }

        return MailUtil.parseAddresses(addrStr);
    }

    private static boolean tooTypeMatches(String tooTypeStr, TooType too) {
        if (tooTypeStr == null) return true; // applies to both TOO types
        tooTypeStr = tooTypeStr.trim().toLowerCase();
        return too.name().equals(tooTypeStr);
    }

    private static boolean locationMatches(String location, Site site) {
        if (location == null) return true;
        location = location.trim().toLowerCase();
        if ("south".equals(location)) {
            return Site.GS.equals(site);
        } else if ("north".equals(location)) {
            return Site.GN.equals(site);
        }
        return true;
    }

    @SuppressWarnings({"unchecked"})
    private static InternetAddress[] _getAddresses(Element parent, DestinationType dest, TooType too, Site site) {
        List<Element> addrElementList = (List<Element>) parent.elements(dest.name());
        if ((addrElementList == null) || (addrElementList.size() == 0)) {
            return EMPTY_ADDRESS_ARRAY;
        }

        if (addrElementList.size() == 1) {
            // the common case, only one tag named "to" or "cc"
            return _getAddresses(addrElementList.get(0), too, site);
        }

        List<InternetAddress> addrList = new ArrayList<InternetAddress>();
        for (Element addrElement : addrElementList) {
            addrList.addAll(Arrays.asList(_getAddresses(addrElement, too, site)));
        }
        return addrList.toArray(EMPTY_ADDRESS_ARRAY);
    }

    private final String _smtpHost;
    private final InternetAddress _sender;

    private final Map<Key, InternetAddress[]> addrMap;
    private final MailTemplate _subjTemplate;
    private final MailTemplate _bodyTemplate;

    /**
     * Constructs with a remote reference to the client-side handler.
     */
    public TooEmail(TooEmailConfig conf) {
        _smtpHost = conf.getSmtpServer();

        _sender = _getSenderAddress();

        Document doc = _getEmailDocument(conf);
        Element root = doc.getRootElement();

        Map<Key, InternetAddress[]> addrMap = new HashMap<Key, InternetAddress[]>();
        TooType[] tooTypes = { TooType.rapid, TooType.standard };
        Site site = conf.getSite();
        for (DestinationType dest : DestinationType.values()) {
            for (TooType too : tooTypes) {
                Key k = new Key(too, dest);
                addrMap.put(k, _getAddresses(root, dest, too, site));
            }
        }
        this.addrMap = Collections.unmodifiableMap(addrMap);

        Element subjectElement = root.element(SUBJECT_TAG);
        if (subjectElement == null) {
            throw new GeminiRuntimeException("Email conf missing 'subject'.");
        }
        _subjTemplate = new MailTemplate(subjectElement.getTextTrim());

        Element bodyElement = root.element(BODY_TAG);
        if (bodyElement == null) {
            throw new GeminiRuntimeException("Email conf missing 'body'.");
        }
        _bodyTemplate = new MailTemplate(bodyElement.getTextTrim());
    }


    private Properties _getTemplateProperties(ISPObservation obs, SPProgram prgDo)
             {
        Properties props = new Properties();
        props.put(OBS_ID_PROP, obs.getObservationIDAsString(""));

        SPObservation obsDo = (SPObservation) obs.getDataObject();
        String title = obsDo.getTitle();
        props.put(OBS_NAME_PROP, title);
        props.put(TOO_TYPE_PROP, Too.get(obs).getDisplayValue());

        props.put(PROG_NAME_PROP, prgDo.getTitle());

        return props;
    }

    private static InternetAddress[] _merge(InternetAddress[] a1, InternetAddress[] a2) {
        if (a1.length == 0) return a2;
        if (a2.length == 0) return a1;

        InternetAddress[] res = new InternetAddress[a1.length + a2.length];
        System.arraycopy(a1, 0, res, 0, a1.length);
        System.arraycopy(a2, 0, res, a1.length, a2.length);
        return res;
    }

    private InternetAddress[] _lookupAddresses(TooType too, DestinationType dest) {
        InternetAddress[] addrs = addrMap.get(new Key(too, dest));
        return (addrs == null) ? EMPTY_ADDRESS_ARRAY : addrs;
    }

    private InternetAddress[] _getToAddress(SPProgram prog, TooType too) {
        InternetAddress[] addrs = _lookupAddresses(too, DestinationType.to);

        SPProgram.PIInfo pinfo = prog.getPIInfo();
        if (pinfo == null) return addrs;

        String email = pinfo.getEmail();
        return (email == null) ? addrs : _merge(addrs, MailUtil.parseAddresses(email));
    }

    private InternetAddress[] _getCcAddress(SPProgram prog, TooType too) {
        InternetAddress[] addrs = _lookupAddresses(too, DestinationType.cc);

        // First add in the NGOs.
        String ngoEmail = prog.getPrimaryContactEmail();
        if (ngoEmail != null) {
            addrs = _merge(addrs, MailUtil.parseAddresses(ngoEmail));
        }

        // Now add in the gemini contacts.
        String gemContactEmails = prog.getContactPerson();
        return (gemContactEmails == null) ? addrs :
                       _merge(addrs, MailUtil.parseAddresses(gemContactEmails));
    }

    private String _getNotifiedString(InternetAddress[] toAddrs, InternetAddress[] ccAddrs) {
        StringBuilder buf = new StringBuilder();

        buf.append("The following people have been notified of this event:\n");

        Set<String> addrSet = new TreeSet<String>();
        for (InternetAddress toAddr : toAddrs) {
            addrSet.add(toAddr.toString());
        }
        for (InternetAddress ccAddr : ccAddrs) {
            addrSet.add(ccAddr.toString());
        }
        for (String addr : addrSet) {
            buf.append("\t").append(addr).append("\n");
        }

        return buf.toString();
    }

    public Message createMessage(ISPObservation obs)  {
        // Get the too Type
        final TooType tooType = Too.get(obs);
        if (tooType == TooType.none) {
            LOG.log(Level.WARNING, "Tried to email alert for non-TOO observation.");
            return null;
        }

        // Get the parent program and data object.
        ISPProgram prg = obs.getProgram();
        SPProgram  prgDo = (SPProgram) prg.getDataObject();

        // First fill in the subject and body template.
        Properties props = _getTemplateProperties(obs, prgDo);
        String subj = _subjTemplate.subsitute(props);
        String body = _bodyTemplate.subsitute(props);

        InternetAddress[] toAddrs  = _getToAddress(prgDo, tooType);
        InternetAddress[] ccAddrs  = _getCcAddress(prgDo, tooType);
        InternetAddress[] bccAddrs = _lookupAddresses(tooType, DestinationType.bcc);

        String notifiedMsg = _getNotifiedString(toAddrs, ccAddrs);
        body = body + "\n\n" + notifiedMsg;

        Properties sessionProps = new Properties();
        sessionProps.put("mail.transport.protocol", "smtp");
        sessionProps.put("mail.smtp.host", _smtpHost);
        Session session = Session.getInstance(sessionProps, null);

        MimeMessage mess = new MimeMessage(session);

        try {
            if (toAddrs.length > 0) {
                mess.addRecipients(Message.RecipientType.TO, toAddrs);
            }
            if (ccAddrs.length > 0) {
                mess.addRecipients(Message.RecipientType.CC, ccAddrs);
            }
            if (bccAddrs.length > 0) {
                mess.addRecipients(Message.RecipientType.BCC, bccAddrs);
            }
            mess.setFrom(_sender);
            mess.setSubject(subj);
            mess.setText(body);
        } catch (MessagingException ex) {
            LOG.log(Level.WARNING, "Problem creating message.", ex);
            return null;
        }

        return mess;
    }

}
