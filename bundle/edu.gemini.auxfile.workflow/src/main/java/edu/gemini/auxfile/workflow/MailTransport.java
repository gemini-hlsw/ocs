package edu.gemini.auxfile.workflow;

import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * A simple wrapper around javax.mail.Transport.  Enables testing.
 */
@FunctionalInterface
public interface MailTransport {
    void send(Message message) throws MessagingException;
}
