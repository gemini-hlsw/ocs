//
// $
//

package edu.gemini.auxfile.workflow;

import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import javax.mail.internet.InternetAddress;
import java.util.Map;
import java.util.List;

/**
 *
 */
public class AddressFetcherTest {
    private IDBDatabaseService odb;

    private ProgramBuilder progBuilder;

    private SPProgramID progId;

    @Before
    public void setUp() throws Exception {
        odb    = DBLocalDatabase.createTransient();
        progId = SPProgramID.toProgramID("GS-2009A-Q-42");
        progBuilder = (new ProgramBuilder(odb)).progId(progId);
    }

    @After
    public void tearDown() {
        odb.getDBAdmin().shutdown();
    }

    private void assertEmails(String[] expected, List<InternetAddress> actual) {
        assertEquals(expected.length, actual.size());
        for (int i=0; i<expected.length; ++i) {
            assertEquals(expected[i], actual.get(i).toString());
        }
    }

    @Test
    public void testEmpty() {
        progBuilder.build();

        Map<AddressFetcher.Role, List<InternetAddress>> res;
        res = AddressFetcher.INSTANCE.getProgramEmails(progId, odb);

        assertEmails(new String[0], res.get(AddressFetcher.Role.PI));
        assertEmails(new String[0], res.get(AddressFetcher.Role.NGO));
        assertEmails(new String[0], res.get(AddressFetcher.Role.CS));
    }

    @Test
    public void testOne() {
        progBuilder.pi("swalker@gemini.edu").ngo("swalker2m@mac.com").cs("swalker2m@yahoo.com").build();

        Map<AddressFetcher.Role, List<InternetAddress>> res;
        res = AddressFetcher.INSTANCE.getProgramEmails(progId, odb);

        assertEmails(new String[] {"swalker@gemini.edu"},  res.get(AddressFetcher.Role.PI));
        assertEmails(new String[] {"swalker2m@mac.com" },  res.get(AddressFetcher.Role.NGO));
        assertEmails(new String[] {"swalker2m@yahoo.com"}, res.get(AddressFetcher.Role.CS));
    }

    @Test
    public void testMultiple() {
        progBuilder.pi("swalker@gemini.edu, swalker2m@mac.com").cs("swalker2m@yahoo.com").build();

        Map<AddressFetcher.Role, List<InternetAddress>> res;
        res = AddressFetcher.INSTANCE.getProgramEmails(progId, odb);

        assertEmails(new String[] {"swalker@gemini.edu", "swalker2m@mac.com"},  res.get(AddressFetcher.Role.PI));
        assertEmails(new String[0], res.get(AddressFetcher.Role.NGO));
        assertEmails(new String[] {"swalker2m@yahoo.com"}, res.get(AddressFetcher.Role.CS));
    }
}
