//
// $Id: MailSender.java 4409 2004-02-02 00:50:58Z shane $
//
package edu.gemini.shared.mail;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

/**
 * An interface that an object which can send email implements.  A default
 * sender is provided.  The primary usage of this interface is expected to
 * be for testing email sending applications without actually sending emails.
 */
public interface MailSender {

    /**
     * A default sender.
     */
    MailSender DEFAULT_SENDER = new MailSender() {
        public void send(Message msg) throws MessagingException {
            Transport.send(msg);
        }
    };

    void send(Message msg) throws MessagingException;
}
