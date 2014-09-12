//
// $
//

package edu.gemini.too.email;

import edu.gemini.spModel.core.Site;

import javax.mail.internet.InternetAddress;
import java.net.URL;

// Before this was a bundle, this information came from the SITE file.  Now
// it will be obtained from BundleProperties.  However the emailConf.xml file
// will remain an XML file.

/**
 * Email server configuration information.
 */
public interface TooEmailConfig {

    String getSmtpServer();

    Site getSite();

    InternetAddress getSender();

    /**
     * Returns the email template and email addresses XML configuration File.
     */
    URL getEmailConfig();
}
