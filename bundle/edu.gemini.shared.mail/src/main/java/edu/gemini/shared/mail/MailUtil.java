//
// $Id: MailUtil.java 4409 2004-02-02 00:50:58Z shane $
//
package edu.gemini.shared.mail;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.mail.Message;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;


/**
 * Utility class for working with mail messages.
 */
public class MailUtil {
    private static final Logger LOG = Logger.getLogger(MailUtil.class.getName());

    public static final InternetAddress[] EMPTY_ADDRESS_ARRAY = new InternetAddress[0];

    /**
     * Parses a string of comma, semicolon, space, or tab separated email
     * addresses into an array of valid InternetAddresses.  Any invalid
     * addresses are left out of the return array.
     */
    public static InternetAddress[] parseAddresses(String addresses) {
        if (addresses == null) return EMPTY_ADDRESS_ARRAY;

        StringTokenizer st = new StringTokenizer(addresses, " \t,;", false);
        List<InternetAddress> lst = new ArrayList<InternetAddress>();
        while (st.hasMoreTokens()) {
            String addrStr = st.nextToken();
            try {
                InternetAddress addr = new InternetAddress(addrStr);
                addr.validate();
                lst.add(addr);
            } catch (AddressException e) {
                LOG.log(Level.WARNING, "illegal email address: " + addrStr);
            }
        }
        return lst.toArray(new InternetAddress[lst.size()]);
    }

    public static String toString(Message msg)
            throws IOException, MessagingException {
        StringBuilder buf = new StringBuilder();
        buf.append("From: ").append(toString(msg.getFrom())).append("\n");
        buf.append("To: ");
        buf.append(toString(msg.getRecipients(Message.RecipientType.TO)));
        buf.append("\n");
        buf.append("Cc: ");
        buf.append(toString(msg.getRecipients(Message.RecipientType.CC)));
        buf.append("\n");
        buf.append("Subject: ").append(msg.getSubject()).append("\n");
        buf.append("\n");
        buf.append(msg.getContent());
        return buf.toString();
    }

    public static String toString(Address[] addrs) {
        if ((addrs == null) || (addrs.length == 0)) return "";

        final StringBuilder buf = new StringBuilder();
        buf.append(addrs[0]);

        for (int i = 1; i < addrs.length; ++i) {
            buf.append(", ").append(addrs[i]);
        }

        return buf.toString();
    }

    public static boolean equals(Message msg1, Message msg2)
            throws IOException, MessagingException {
        /*
        System.out.println("------------");
        System.out.println(toString(msg1));
        System.out.println("************");
        System.out.println(toString(msg2));
        System.out.println("------------");
        */

        // Compare the subject of each message.
        String subject1 = msg1.getSubject();
        String subject2 = msg2.getSubject();
        if (!subject1.equals(subject2)) return false;

        // Compare the text of each message.
        Object content1 = msg1.getContent();
        Object content2 = msg2.getContent();
        if (!content1.equals(content2)) return false;

        // Compare the sender of each message.
        if (!addressesEqual(msg1.getFrom(), msg2.getFrom())) {
            return false;
        }

        // Compare the recipient(s) of each message.
        if (!recipientsEqual(Message.RecipientType.TO, msg1, msg2)) {
            return false;
        }
        return recipientsEqual(Message.RecipientType.CC, msg1, msg2);
    }

    public static boolean recipientsEqual(Message.RecipientType type,
                                          Message msg1, Message msg2)
            throws MessagingException {

        Address[] rec1 = msg1.getRecipients(type);
        Address[] rec2 = msg2.getRecipients(type);

        return addressesEqual(rec1, rec2);
    }

    public static boolean addressesEqual(Address[] addr1, Address[] addr2) {
        if (addr1 == null) {
            return (addr2 == null);
        } else if (addr2 == null) {
            return false;
        }
        if (addr1.length != addr2.length) return false;

        for (int i = 0; i < addr1.length; ++i) {
            Address adr1 = addr1[i];
            Address adr2 = addr2[i];

            if (!adr1.equals(adr2)) return false;
        }

        return true;
    }

}
