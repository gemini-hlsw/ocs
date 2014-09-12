//
// $
//

package edu.gemini.dataman.gsa.query;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.File;
import java.util.*;

//
// NOTICE: these test cases require that the QueuedQuery execute an ssh command.
// So, in order for these tests to work, it needs the username and password of
// the person running the test.
//
// These can be specified using the system properties:
//
//   edu.gemini.dataman.context.user
//   edu.gemini.dataman.context.password
//

/**
 * Test cases for the QueuedQueryTest.  To make this work, you have to
 * supply two system properties for the ssh code. See
 * {@link edu.gemini.dataman.context.TestGsaXferConfig}.
 */

@Ignore
public final class QueuedQueryTest extends GsaQueryTestBase {
    private QueuedQuery query;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // use the test ssh session instead of the real one
//        TestSshSession.register();
        query = new QueuedQuery(config);
    }

    @After
    public void tearDown() throws IOException {
//        TestSshSession.unregister();
        super.tearDown();
    }

    private static final String FILENAME1 = "S20080718S0001.fits";
    private static final String FILENAME2 = "S20080718S0002.fits";

    private void mkFile(String filename) throws Exception {
        File f = new File(config.getGsaXferConfig().getDestDir(), filename);
        assertTrue(f.createNewFile());
    }

    @Test public void testOneNotFound() throws Exception {
        assertFalse(query.isQueued(FILENAME1));
    }

    @Test public void testOneFound() throws Exception {
        mkFile(FILENAME1);
        assertTrue(query.isQueued(FILENAME1));
    }

    @Test public void testGetEmptySet() throws Exception {
        Set<String> queuedFiles = query.getQueuedFiles();
        assertNotNull(queuedFiles);
        assertEquals(0, queuedFiles.size());
    }

    @Test public void testGetSingleItemSet() throws Exception {
        mkFile(FILENAME1);

        Set<String> queuedFiles = query.getQueuedFiles();
        assertEquals(1, queuedFiles.size());
        assertEquals(FILENAME1, queuedFiles.iterator().next());
    }

    @Test public void testMultipleItemSet() throws Exception {
        mkFile(FILENAME1); mkFile(FILENAME2);

        Set<String> queuedFiles = query.getQueuedFiles();
        assertEquals(2, queuedFiles.size());

        Set<String> expected = new HashSet<String>();
        expected.add(FILENAME1);
        expected.add(FILENAME2);

        assertEquals(expected, queuedFiles);
    }
}
