//
// $Id: MailConfig.java 893 2007-07-19 19:43:20Z swalker $
//

package edu.gemini.auxfile.workflow;

import javax.mail.internet.InternetAddress;
import java.util.List;

/**
 *
 */
public interface MailConfig {
    String getSmtpHost();
    InternetAddress getSender();
    List<InternetAddress> getFinderRecipients();
    List<InternetAddress> getMaskRecipients();
}
