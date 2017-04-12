//
// $Id: OdbMailConfig.java 4410 2004-02-02 00:51:35Z shane $
//
package edu.gemini.dbTools.mail;

import edu.gemini.spdb.cron.util.Props;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;

/**
 * Encapsulates the mail configuration options and, most importantly,
 * provides a means of creating a Message object which can be filled
 * in with the recipient(s), subject, and text and sent.
 */
public class OdbMailConfig extends Props {

    private static final String SMTP_PROPERTY = "SITE_SMTP_SERVER";
    private static final String SENDER_ADDR_PROPERTY = "ODB_MAIL_SENDER_ADDR";
    private static final String SENDER_NAME_PROPERTY = "ODB_MAIL_SENDER_NAME";
    private static final String DEFAULT_SENDER_ADDR = "noreply@gemini.edu";
    private static final String DEFAULT_SENDER_NAME = "Gemini ODB";

    private final String smtpHost;
    public final InternetAddress sender;
    public final File stateFile;

    public OdbMailConfig(final File tempDir, final Map<String, String> env) throws UnsupportedEncodingException {
        super(env);
        smtpHost = getString(SMTP_PROPERTY);
        sender = new InternetAddress();
        sender.setAddress(getString(SENDER_ADDR_PROPERTY, DEFAULT_SENDER_ADDR));
        sender.setPersonal(getString(SENDER_NAME_PROPERTY, DEFAULT_SENDER_NAME));
        stateFile = new File(tempDir, "odbMailState.xml");
    }

    public Message createMessage() throws MessagingException {
        final Properties sessionProps = new Properties();
        sessionProps.put("mail.transport.protocol", "smtp");
        sessionProps.put("mail.smtp.host", smtpHost);
        final Session session = Session.getInstance(sessionProps, null);
        final MimeMessage mess = new MimeMessage(session);
        mess.setFrom(sender);
        return mess;
    }

}
