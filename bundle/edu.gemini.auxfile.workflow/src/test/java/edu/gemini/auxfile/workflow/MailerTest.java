//
// $
//

package edu.gemini.auxfile.workflow;

import edu.gemini.auxfile.copier.AuxFileType;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.FileUtil;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;


/**
 *
 */
public class MailerTest {
    private File dir;
    private IDBDatabaseService odb;

    private ProgramBuilder progBuilder;
    private TestMailConfig.Builder mailConfigBuilder;
    private TestMailTransport transport;

    private SPProgramID progId;

    @Before
    public void setUp() throws Exception {
        dir = new File("/tmp/odb" + System.identityHashCode(this));
        if (dir.exists()) FileUtil.deleteDir(dir);
        if (!dir.mkdirs()) fail("could not make odb directory");

        odb = DBLocalDatabase.createTransient();
        progId = SPProgramID.toProgramID("GS-2009A-Q-42");
        progBuilder = (new ProgramBuilder(odb)).progId(progId);

        mailConfigBuilder = new TestMailConfig.Builder();
        mailConfigBuilder.sender(new InternetAddress("noreply@gemini.edu"));

        transport = new TestMailTransport();
    }

    @After
    public void tearDown() {
        odb.getDBAdmin().shutdown();
        if (!dir.delete()) fail("Could not delete: " + dir);
    }

    private Set<String> hashAddresses(Message.RecipientType type) throws MessagingException {
        Message msg = transport.getMessage();
        Address[] addrs = msg.getRecipients(type);

        Set<String> res = new HashSet<String>();
        for (Address addr : addrs) {
            res.add(addr.toString());
        }
        return res;
    }

    @Test
    public void testEmptyConfigWithCS() throws Exception {

        progBuilder.cs("swalker2m@yahoo.com").build();

        Mailer m = new Mailer(mailConfigBuilder.build(), odb, transport);
        m.notifyStored(progId, AuxFileType.other, "TestFinder.jpg");

        Message msg = transport.getMessage();

        Address[] addrs = msg.getRecipients(Message.RecipientType.TO);

        assertEquals(addrs.length, 1);
        assertEquals(addrs[0].toString(), "swalker2m@yahoo.com");
    }

    @Test
    public void testConfigWithCS() throws Exception {
        List<InternetAddress> finderRecipients = new ArrayList<InternetAddress>();
        finderRecipients.add(new InternetAddress("swalker2m@mac.com"));
        mailConfigBuilder.finderRecipients(finderRecipients);

        progBuilder.cs("swalker2m@yahoo.com").build();

        Mailer m = new Mailer(mailConfigBuilder.build(), odb, transport);
        m.notifyStored(progId, AuxFileType.other, "TestFinder.jpg");

        Set<String> addrs = hashAddresses(Message.RecipientType.TO);
        assertEquals(addrs.size(), 2);
        assertTrue(addrs.contains("swalker2m@yahoo.com"));
        assertTrue(addrs.contains("swalker2m@mac.com"));
    }

    @Test
    public void testEmptyConfigNoCS() throws Exception {
        progBuilder.ngo("swalker2m@yahoo.com").build();

        Mailer m = new Mailer(mailConfigBuilder.build(), odb, transport);
        m.notifyStored(progId, AuxFileType.other, "TestFinder.jpg");

        Set<String> addrs = hashAddresses(Message.RecipientType.TO);
        assertEquals(addrs.size(), 1);
        assertTrue(addrs.contains("swalker2m@yahoo.com"));
    }
}
