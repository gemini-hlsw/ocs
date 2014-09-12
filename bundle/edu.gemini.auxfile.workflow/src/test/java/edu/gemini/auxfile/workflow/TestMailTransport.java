//
// $
//

package edu.gemini.auxfile.workflow;

import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 */
public final class TestMailTransport implements MailTransport {
    private Message message;
    
    public void send(Message message) throws MessagingException {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
