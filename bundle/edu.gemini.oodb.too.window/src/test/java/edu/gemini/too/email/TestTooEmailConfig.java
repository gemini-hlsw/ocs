//
// $
//

package edu.gemini.too.email;

import edu.gemini.spModel.core.Site;
import org.dom4j.Document;
import org.dom4j.io.XMLWriter;

import javax.mail.internet.InternetAddress;
import java.io.File;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 */
public class TestTooEmailConfig implements TooEmailConfig {
    public static final String SMTP_SERVER = "smtp@gemini.edu";

    public static InternetAddress SENDER;

    static {
        try {
            SENDER = new InternetAddress("odb@gemini.edu");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private final Site site;
    private final File file;

    public TestTooEmailConfig(Site site, Document doc) throws Exception {
        this.site = site;
        file = getTempFile();

        XMLWriter writer = new XMLWriter(new FileWriter(file));
        writer.write(doc);
        writer.close();
    }

    @Override public String getSmtpServer() {
        return SMTP_SERVER;
    }

    @Override public Site getSite() {
        return site;
    }

    @Override
    public InternetAddress getSender() {
        return SENDER;
    }

    @Override
    public URL getEmailConfig() {
        try {
            return file.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static File getTempFile() {
        try {
            return File.createTempFile("TestTooEmailConfig", "xml");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
