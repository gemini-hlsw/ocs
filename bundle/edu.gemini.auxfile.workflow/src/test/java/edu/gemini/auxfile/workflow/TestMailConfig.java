//
// $
//

package edu.gemini.auxfile.workflow;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 */
public final class TestMailConfig implements MailConfig {

    public static final class Builder {
        private String smtpHost = "exchange.gemini.edu";
        private InternetAddress sender;
        private List<InternetAddress> finderRecipients = new ArrayList<InternetAddress>();
        private List<InternetAddress> maskRecipients   = new ArrayList<InternetAddress>();

        public Builder() {
            try {
                this.sender = new InternetAddress("noreply@gemini.edu");
            } catch (AddressException e) {
                throw new RuntimeException();
            }
        }

        public Builder smtpHost(String host) {
            this.smtpHost = host;
            return this;
        }

        public Builder sender(InternetAddress addr) {
            this.sender = addr;
            return this;
        }

        public Builder finderRecipients(List<InternetAddress> finderRecipients) {
            this.finderRecipients = Collections.unmodifiableList(new ArrayList<InternetAddress>(finderRecipients));
            return this;
        }

        public Builder maskRecipients(List<InternetAddress> maskRecipients) {
            this.maskRecipients = Collections.unmodifiableList(new ArrayList<InternetAddress>(maskRecipients));
            return this;
        }

        public TestMailConfig build() {
            return new TestMailConfig(this);
        }
    }

    private final String smtpHost;
    private final InternetAddress sender;
    private final List<InternetAddress> finderRecipients;
    private final List<InternetAddress> maskRecipients;

    private TestMailConfig(Builder b) {
        smtpHost         = b.smtpHost;
        sender           = b.sender;
        finderRecipients = b.finderRecipients;
        maskRecipients   = b.maskRecipients;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public InternetAddress getSender() {
        return sender;
    }

    public List<InternetAddress> getFinderRecipients() {
        return finderRecipients;
    }

    public List<InternetAddress> getMaskRecipients() {
        return maskRecipients;
    }
}
