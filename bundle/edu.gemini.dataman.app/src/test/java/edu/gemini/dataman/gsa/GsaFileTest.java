//
// $
//

package edu.gemini.dataman.gsa;

import edu.gemini.dataman.context.TestDatamanConfig;
import edu.gemini.dataman.gsa.query.CrcQueryTest;
import edu.gemini.dataman.gsa.query.SingleXferQuery;
import edu.gemini.dataman.gsa.query.SingleXferQueryTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;

/**
 * Test cases for determining the GsaFile status.
 */
@Ignore
public class GsaFileTest {
    protected TestDatamanConfig config;

    @Before
    public void setUp() throws Exception {
        config = new TestDatamanConfig();
    }

    @After
    public void tearDown() throws IOException {
        config.cleanup();
    }

    private static final String FILENAME1 = "S20080718S0001.fits";
    private static final String FILENAME2 = "S20080718S0002.fits";

    private void assertEqualsIgnoreInfo(GsaFileStatus expected, GsaFileStatus actual) {
        assertEquals(expected.getFilename(), actual.getFilename());
        assertEquals(expected.getState(),    actual.getState());
        assertEquals(expected.getCrc(),      actual.getCrc());
    }

    @Test public void testOneNotFound() throws Exception {
        GsaFileStatus actual   = GsaFileStatus.query(config, FILENAME1);
        GsaFileStatus expected = new GsaFileStatus(FILENAME1, GsaFileStatus.State.notFound);
        assertEqualsIgnoreInfo(expected, actual);
    }

    @Test public void testOneQueued() throws Exception {
        SingleXferQuery.Status xferStatus;
        xferStatus = new SingleXferQuery.Status(FILENAME1, System.currentTimeMillis(), SingleXferQuery.Status.Code.pickup, "");
        SingleXferQueryTest.writeXferStatus(config, xferStatus);

        GsaFileStatus actual   = GsaFileStatus.query(config, FILENAME1);
        GsaFileStatus expected = new GsaFileStatus(FILENAME1, GsaFileStatus.State.queued);
        assertEqualsIgnoreInfo(expected, actual);
    }

    @Test public void testOneProcessing() throws Exception {
        SingleXferQuery.Status xferStatus;
        xferStatus = new SingleXferQuery.Status(FILENAME1, System.currentTimeMillis(), SingleXferQuery.Status.Code.transferring, "");
        SingleXferQueryTest.writeXferStatus(config, xferStatus);

        GsaFileStatus actual   = GsaFileStatus.query(config, FILENAME1);
        GsaFileStatus expected = new GsaFileStatus(FILENAME1, GsaFileStatus.State.processing);
        assertEqualsIgnoreInfo(expected, actual);
    }

    @Test public void testOneSuccessInXfer() throws Exception {
        // Testing a successful result when the file is still being reported
        // by the xfer software.  At some point, it disappears, but for a while
        // it can still return success.
        SingleXferQuery.Status xferStatus;
        xferStatus = new SingleXferQuery.Status(FILENAME1, System.currentTimeMillis(), SingleXferQuery.Status.Code.success, "");
        SingleXferQueryTest.writeXferStatus(config, xferStatus);

        // Write the expected CRC
        CrcQueryTest.writeLine(config, FILENAME1, 42L);

        GsaFileStatus actual   = GsaFileStatus.query(config, FILENAME1);
        GsaFileStatus expected = new GsaFileStatus(FILENAME1, GsaFileStatus.State.accepted, "", 42L);
        assertEqualsIgnoreInfo(expected, actual);
    }

    @Test public void testOneAccepted() throws Exception {
        // Here we'll test the more common case in which the xfer software is
        // reporting nothing for the file that has been accepted.
        // Write the expected CRC
        CrcQueryTest.writeLine(config, FILENAME1, 42L);

        GsaFileStatus actual   = GsaFileStatus.query(config, FILENAME1);
        GsaFileStatus expected = new GsaFileStatus(FILENAME1, GsaFileStatus.State.accepted, "", 42L);
        assertEqualsIgnoreInfo(expected, actual);
    }

    @Test public void testOneRejected() throws Exception {
        SingleXferQuery.Status xferStatus;
        xferStatus = new SingleXferQuery.Status(FILENAME1, System.currentTimeMillis(), SingleXferQuery.Status.Code.rejected, "");
        SingleXferQueryTest.writeXferStatus(config, xferStatus);

        GsaFileStatus actual   = GsaFileStatus.query(config, FILENAME1);
        GsaFileStatus expected = new GsaFileStatus(FILENAME1, GsaFileStatus.State.rejected);
        assertEqualsIgnoreInfo(expected, actual);
    }
}
