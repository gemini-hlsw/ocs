//
// $Id: OdbMailTemplate.java 4410 2004-02-02 00:51:35Z shane $
//
package edu.gemini.dbTools.mail;

import edu.gemini.shared.mail.MailTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;


public class OdbMailTemplate {
    private static final Logger LOG = Logger.getLogger(OdbMailTemplate.class.getName());

    public static final MailTemplate DOWN_FOR_REVIEW;
    public static final MailTemplate DOWN_PHASE2;
    public static final MailTemplate ON_HOLD;
    public static final MailTemplate UP_FOR_ACTIVATION;
    public static final MailTemplate UP_FOR_REVIEW;
    public static final MailTemplate UP_READY;

    public static final String PROG_ID_VAR     = "PROG_ID";
    public static final String OBS_ID_LIST_VAR = "OBS_ID_LIST";

    static {
        String tmplTxt;

        tmplTxt = readTemplate("Down_ForReview.txt");
        DOWN_FOR_REVIEW = new MailTemplate(tmplTxt);

        tmplTxt = readTemplate("Down_Phase2.txt");
        DOWN_PHASE2 = new MailTemplate(tmplTxt);

        tmplTxt = readTemplate("OnHold.txt");
        ON_HOLD = new MailTemplate(tmplTxt);

        tmplTxt = readTemplate("Up_ForActivation.txt");
        UP_FOR_ACTIVATION = new MailTemplate(tmplTxt);

        tmplTxt = readTemplate("Up_ForReview.txt");
        UP_FOR_REVIEW = new MailTemplate(tmplTxt);

        tmplTxt = readTemplate("Up_Ready.txt");
        UP_READY = new MailTemplate(tmplTxt);
    }

    private static String readTemplate(final String name) {
        final URL url = OdbMailTemplate.class.getResource("/resources/odbMailTemplates/" + name);
        if (url == null) {
            LOG.severe("Email template not found: " + name);
            return null;
        }

        final StringBuilder buf = new StringBuilder();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = br.readLine();
            while (line != null) {
                buf.append(line).append("\n");
                line = br.readLine();
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Couldn't close reader.", ex);
                }
            }
        }

        return buf.toString();
    }

}
