//
// $
//

package edu.gemini.too.email;

import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public final class TooEmailTest extends SpModelTestBase {

    private static final String[] NGO_EMAILS = {
            "ngo1@partner1.pc1",
            "ngo2@partner2.pc2"
    };

    private static final String GEMINI_EMAIL = "support@gemini.edu";
    private static final String PI_EMAIL = "p1@partner1.pc1";

    private static final String PROG_ID  = "GS-2010B-Q-1";

    private static final String PROG_NAME = "Email Test Program";
    private static final String OBS_NAME  = "Email Test Observation";
    private static final String SUBJECT   = "type: @TOO_TYPE@, id: @OBS_ID@";
    private static final String BODY      = "name: @OBS_NAME@, prog: @PROG_NAME@";

    private static String cat(String[] emails) {
        StringBuilder buf = new StringBuilder();
        if (emails.length > 0) buf.append(emails[0]);
        for (int i=1; i<emails.length; ++i) {
            buf.append(";").append(emails[i]);
        }
        return buf.toString();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp(SPProgramID.toProgramID(PROG_ID));

        // Store some email information.
        SPProgram prog = (SPProgram) getProgram().getDataObject();
        prog.setTitle(PROG_NAME);
        prog.setContactPerson(GEMINI_EMAIL);
        prog.setPrimaryContactEmail(cat(NGO_EMAILS));
        SPProgram.PIInfo piInfo = new SPProgram.PIInfo("Biff", "Henderson", PI_EMAIL, "999", null);
        prog.setPIInfo(piInfo);
        getProgram().setDataObject(prog);

        // Store the observation name.
        SPObservation obs = (SPObservation) getObs().getDataObject();
        obs.setTitle(OBS_NAME);
        getObs().setDataObject(obs);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private static final String ANY_TYPE       = "too@gemini.edu";
    private static final String RAPID          = "rtoo@gemini.edu";
    private static final String STANDARD       = "stoo@gemini.edu";
    private static final String SOUTH_ANY_TYPE = "gstoo@gemini.edu";
    private static final String SOUTH_RAPID    = "gsrtoo@gemini.edu";
    private static final String SOUTH_STANDARD = "gsstoo@gemini.edu";

    private Document createDoc() {
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("email");
        root.addElement("from").addText("noreply@gemini.edu");
        root.addElement("subject").addText(SUBJECT);
        root.addElement("body").setText(BODY);

        root.addElement("to").setText(ANY_TYPE);
        root.addElement("to").addAttribute("too", "rapid").setText(RAPID);
        root.addElement("to").addAttribute("too", "standard").setText(STANDARD);
        root.addElement("to").addAttribute("site", "south").setText(SOUTH_ANY_TYPE);
        root.addElement("to").addAttribute("site", "south").addAttribute("too", "rapid").setText(SOUTH_RAPID);
        root.addElement("to").addAttribute("site", "south").addAttribute("too", "standard").setText(SOUTH_STANDARD);

        return doc;
    }

    private void verifyMessage(Message m, TooType too, Site site) throws Exception {
        // Check the from
        InternetAddress[] from = (InternetAddress[]) m.getFrom();
        assertEquals(1, from.length);
        assertEquals("noreply@gemini.edu", from[0].getAddress());

        // Compare the subject
        String subject = SUBJECT.replace("@TOO_TYPE@", too.getDisplayValue()).replace("@OBS_ID@", PROG_ID + "-1");
        assertEquals(subject, m.getSubject());

        // Compare first lines of the body.
        String body    = BODY.replace("@OBS_NAME@", OBS_NAME).replace("@PROG_NAME@", PROG_NAME);
        String content = m.getContent().toString();
        int i = content.indexOf("\n");
        assertEquals(body, content.substring(0, i));

        // Check the "to" recipients.
        Set<String> expected = new HashSet<String>();
        expected.add(PI_EMAIL);
        expected.add(ANY_TYPE);
        if (site == Site.GS) expected.add(SOUTH_ANY_TYPE);
        switch (too) {
            case rapid:
                expected.add(RAPID);
                if (site == Site.GS) expected.add(SOUTH_RAPID);
                break;
            case standard:
                expected.add(STANDARD);
                if (site == Site.GS) expected.add(SOUTH_STANDARD);
                break;
            case none:
                break;
        }

        assertEquals(expected, getAddresses(m, Message.RecipientType.TO));

        // Check the "cc" addresses.
        expected.clear();
        expected.add(GEMINI_EMAIL);
        expected.addAll(Arrays.asList(NGO_EMAILS));
        assertEquals(expected, getAddresses(m, Message.RecipientType.CC));
    }

    private Set<String> getAddresses(Message m, Message.RecipientType type) throws Exception {
        InternetAddress[] addrs = (InternetAddress[]) m.getRecipients(type);
        Set<String> actual = new HashSet<String>();
        for (InternetAddress addr : addrs) actual.add(addr.getAddress());
        return actual;
    }

    @Test
    public void testMessages() throws Exception {
        for (TooType too : TooType.values()) {
            for (Site site : Site.values()) {
                // Create the configuration for this site.
                TestTooEmailConfig conf;
                conf = new TestTooEmailConfig(site, createDoc());

                // Set the ToO type.
                Too.set(getProgram(), too);

                // Generate the message.
                Message m = (new TooEmail(conf)).createMessage(getObs());

                // If not a TOO, no message is generated.
                if (too == TooType.none) {
                    assertNull(m);
                    continue;
                }

                // Make sure the message contains what we expected
                verifyMessage(m, too, site);
            }
        }
    }
}
