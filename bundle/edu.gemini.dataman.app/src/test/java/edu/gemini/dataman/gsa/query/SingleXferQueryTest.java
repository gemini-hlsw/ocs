//
// $
//

package edu.gemini.dataman.gsa.query;

import edu.gemini.dataman.context.TestDatamanConfig;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;

import static org.junit.Assert.*;


/**
 * Test cases for the XferQuery.  Uses a file URL, writing first to a file
 * with the information that would come from the server.
 */
@Ignore
public class SingleXferQueryTest extends GsaQueryTestBase {

    public static void writeXferStatus(TestDatamanConfig config, SingleXferQuery.Status status) throws IOException {
        String timeStr = status.getCadcFormattedTime();
        String s = String.format("%s\t%s\t%s",
                status.getFilename(), timeStr, status.getCode().name());
        if (status.getInfo() != null) s = s + "\t" + status.getInfo();
        writeLine(config, status.getFilename(), s);

    }

    private void writeExpectedResults(SingleXferQuery.Status status) throws IOException {
        writeXferStatus(config, status);
    }

    private void writeLine(String filename, String line) throws IOException {
        writeLine(config, filename, line);
    }

    private static void writeLine(TestDatamanConfig config, String filename, String line) throws IOException {
        File dir  = config.getXferQueryDir();
        File file = new File(dir, filename);

        if (file.exists() && !file.delete()) {
            fail("Couldn't delete existing file: " + file);
        }

        Writer w = new FileWriter(file);
        try {
            w.write(line);
            w.write('\n');
            w.flush();
        } finally {
            w.close();
        }
    }


    private static final String FILENAME = "S20080718S0001.fits";

    private void assertException(String failureMessage) {
        SingleXferQuery query = new SingleXferQuery(config);
        try {
            query.getStatus(FILENAME);
            fail(failureMessage);
        } catch (IOException ex) {
            // expected
        }
    }

    /**
     * Tests a query that doesn't returns a FileNotFoundException.
     */
    @Test public void testFileNotFound() throws Exception {
        SingleXferQuery query = new SingleXferQuery(config);
        SingleXferQuery.Status res = query.getStatus(FILENAME);
        assertNotNull(res);
        assertEquals(SingleXferQuery.Status.Code.notFound, res.getCode());
    }

    /**
     * Tests a query that returns fewer than the minimum number of items.
     */
    @Test public void testIncompleteResults() throws Exception {
        writeLine(FILENAME, FILENAME);

        SingleXferQuery query = new SingleXferQuery(config);
        XferQueryBase.Status status = query.getStatus(FILENAME);
        assertEquals(XferQueryBase.Status.Code.unknown, status.getCode());
        assertEquals("unexpected query result", status.getInfo());
    }

    /**
     * Tests a query that returns too much information.
     */
    @Test public void testTooManyResults() throws Exception {
        writeLine(FILENAME,
                String.format("%s\tFri Oct 26 02:00:08 2007\tsuccess\tinfo\textra",
                        FILENAME));

        SingleXferQuery query = new SingleXferQuery(config);
        XferQueryBase.Status status = query.getStatus(FILENAME);
        assertEquals(XferQueryBase.Status.Code.unknown, status.getCode());
        assertEquals("unexpected query result", status.getInfo());
    }

    /**
     * Tests a filename that doesn't match.
     */
    @Test public void testFilenameDoesntMatch() throws Exception {
        writeLine(FILENAME,
                String.format("%s.gz\tFri Oct 26 02:00:08 2007\tsuccess", FILENAME));

        SingleXferQuery query = new SingleXferQuery(config);
        XferQueryBase.Status status = query.getStatus(FILENAME);
        assertEquals(XferQueryBase.Status.Code.success, status.getCode());
        assertEquals(FILENAME, status.getFilename());
    }

    /**
     * Tests an un parseable time, which shouldn't be a problem.
     */
    @Test public void testBadTimeString() throws Exception {
        writeLine(FILENAME, String.format("%s\t%s\t%s", FILENAME, "xxx", "success"));
        SingleXferQuery query = new SingleXferQuery(config);
        SingleXferQuery.Status actual = query.getStatus(FILENAME);
        SingleXferQuery.Status expect = new SingleXferQuery.Status(FILENAME, 0, SingleXferQuery.Status.Code.success, null);
        assertEquals(expect, actual);
    }

    // Gets the current time in seconds -- setting the milliseconds to 0
    private static long getRoundedTime() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /**
     * Tests a normal status without info.
     */
    @Test public void testNormalNoInfo() throws Exception {
        SingleXferQuery.Status expect;
        expect = new SingleXferQuery.Status(FILENAME, getRoundedTime(),
                                      SingleXferQuery.Status.Code.success, null);
        writeExpectedResults(expect);

        SingleXferQuery query = new SingleXferQuery(config);
        SingleXferQuery.Status actual = query.getStatus(FILENAME);
        assertEquals(expect, actual);
    }

    /**
     * Tests a normal status with info.
     */
    @Test public void testNormalWithInfo() throws Exception {
        SingleXferQuery.Status expect;
        expect = new SingleXferQuery.Status(FILENAME, getRoundedTime(),
                                      SingleXferQuery.Status.Code.success, "some info");
        writeExpectedResults(expect);

        SingleXferQuery query = new SingleXferQuery(config);
        SingleXferQuery.Status actual = query.getStatus(FILENAME);
        assertEquals(expect, actual);

    }
}
