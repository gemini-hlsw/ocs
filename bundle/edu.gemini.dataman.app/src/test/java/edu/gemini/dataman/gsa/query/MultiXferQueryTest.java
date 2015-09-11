//
// $
//

package edu.gemini.dataman.gsa.query;

import edu.gemini.dataman.context.TestDatamanConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Test cases for the XferQuery.  Uses a file URL, writing first to a file
 * with the information that would come from the server.
 */
@Ignore
public class MultiXferQueryTest extends GsaQueryTestBase {

    public static void writeXferStatus(TestDatamanConfig config, XferQueryBase.Status status) throws IOException {
        writeLines(config, new String[] { toString(status) });
    }

    public static void writeXferStatus(TestDatamanConfig config, List<XferQueryBase.Status> statuses) throws IOException {
        writeXferStatus(config, statuses.toArray(new XferQueryBase.Status[statuses.size()]));
    }

    public static void writeXferStatus(TestDatamanConfig config, XferQueryBase.Status[] statuses) throws IOException {
        String[] lines = new String[statuses.length];
        int i = 0;
        for (XferQueryBase.Status status : statuses) {
            lines[i++] = toString(status);
        }
        writeLines(config, lines);
    }

    private static void writeLines(TestDatamanConfig config, String[] lines) throws IOException {
        File dir  = config.getXferQueryDir();
        File file = new File(dir, TestDatamanConfig.MULTI_XFER_QUERY_FILE);

        if (file.exists() && !file.delete()) {
            fail("Couldn't delete existing file: " + file);
        }

        Writer w = new FileWriter(file);
        try {
            for (String line : lines) {
                w.write(line);
                w.write('\n');
            }
            w.flush();
        } finally {
            w.close();
        }
    }

    private static String toString(XferQueryBase.Status status) {
        String timeStr = status.getCadcFormattedTime();
        String s = String.format("%s\t%s\t%s",
                status.getFilename(), timeStr, status.getCode().name());
        if (status.getInfo() != null) s = s + "\t" + status.getInfo();
        return s;
    }

    private void writeExpectedResults(SingleXferQuery.Status[] statuses) throws IOException {
        writeXferStatus(config, statuses);
    }

    private void writeExpectedResults(List<SingleXferQuery.Status> statuses) throws IOException {
        writeXferStatus(config, statuses);
    }

    private void writeLines(List<String> lines) throws IOException {
        writeLines(config, lines.toArray(new String[lines.size()]));
    }

    private void writeLines(String[] lines) throws IOException {
        writeLines(config, lines);
    }

    // Gets the current time in seconds -- setting the milliseconds to 0
    private static long getRoundedTime() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static final String FILENAME1 = "S20080718S0001.fits";
    private static final String FILENAME2 = "S20080718S0002.fits";
    private static final Set<String> FILENAMES = new HashSet<String>();

    static {
        FILENAMES.add(FILENAME1);
        FILENAMES.add(FILENAME2);
    }

    private MultiXferQuery query;
    private XferQueryBase.Status status1;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        query = new MultiXferQuery(config);

        status1 = new XferQueryBase.Status(FILENAME1, getRoundedTime(),
                           XferQueryBase.Status.Code.transferring, "some info");
    }

    private Map<String, XferQueryBase.Status> getResults(Set<String> files) throws Exception {
        Map<String, XferQueryBase.Status> res = query.getStatus(files);
        assertEquals(files.size(), res.size());
        assertEquals(status1, res.get(FILENAME1));
        return res;
    }

    /**
     * Tests using the mutli xfer query with a single file.
     */
    @Test public void testSingleFileTransferring() throws Exception {
        writeExpectedResults(new XferQueryBase.Status[] {status1});
        Set<String> files = new HashSet<String>();
        files.add(FILENAME1);
        getResults(files);
    }

    /**
     * Tests a normal case for two files.
     */
    @Test public void testTwoFilesTransferring() throws Exception {
        XferQueryBase.Status status2;
        status2 = new XferQueryBase.Status(FILENAME2, getRoundedTime(),
                           XferQueryBase.Status.Code.transferring, "some info");
        writeExpectedResults(new XferQueryBase.Status[] {status1, status2});

        Map<String, XferQueryBase.Status> res = getResults(FILENAMES);
        assertEquals(status2, res.get(FILENAME2));
    }

    /**
     * Tests a file not found.
     */
    @Test public void testFileNotFound() throws Exception {
        writeExpectedResults(new XferQueryBase.Status[] {status1});

        Map<String, XferQueryBase.Status> res = getResults(FILENAMES);
        assertEquals(XferQueryBase.Status.Code.notFound, res.get(FILENAME2).getCode());
    }

    /**
     * Tests incomplete results.
     */
    @Test public void testIncompleteResults() throws Exception {
        String[] lines = new String[2];
        lines[0] = toString(status1);
        lines[1] = FILENAME2;
        writeLines(lines);

        Map<String, XferQueryBase.Status> res = getResults(FILENAMES);

        XferQueryBase.Status status2 = res.get(FILENAME2);
        assertEquals(XferQueryBase.Status.Code.unknown, status2.getCode());
        assertEquals("unexpected query result", status2.getInfo());
    }

    /**
     * Tests a query that returns too much information.
     */
    @Test public void testTooManyResults() throws Exception {
        String[] lines = new String[2];
        lines[0] = toString(status1);
        lines[1] = String.format("%s\tFri Oct 26 02:00:08 2007\tsuccess\tinfo\textra",
                        FILENAME2);
        writeLines(lines);

        Map<String, XferQueryBase.Status> res = getResults(FILENAMES);
        XferQueryBase.Status status2 = res.get(FILENAME2);
        assertEquals(XferQueryBase.Status.Code.unknown, status2.getCode());
        assertEquals("unexpected query result", status2.getInfo());
    }

    /**
     * Tests a query that returns too much information.
     */
    @Test public void testUnknownStatus() throws Exception {
        String[] lines = new String[2];
        lines[0] = toString(status1);
        lines[1] = String.format("%s\tFri Oct 26 02:00:08 2007\txxxx",
                        FILENAME2);
        writeLines(lines);

        Map<String, XferQueryBase.Status> res = getResults(FILENAMES);
        XferQueryBase.Status status2 = res.get(FILENAME2);
        assertEquals(XferQueryBase.Status.Code.unknown, status2.getCode());
        assertEquals("unknown e-transfer return code 'xxxx'", status2.getInfo());
    }

    /**
     * Tests a filename that doesn't match -- should work anyway
     */
    @Test public void testFilenameDoesntMatch() throws Exception {
        String[] lines = new String[2];
        lines[0] = toString(status1);
        lines[1] = String.format("%s.gz\tFri Oct 26 02:00:08 2007\ttransferring",
                        FILENAME2);
        writeLines(lines);

        Map<String, XferQueryBase.Status> res = getResults(FILENAMES);
        XferQueryBase.Status status2 = res.get(FILENAME2);
        assertEquals(XferQueryBase.Status.Code.transferring, status2.getCode());
    }

    /**
     * Tests an un parseable time, which shouldn't be a problem.
     */
    @Test public void testBadTimeString() throws Exception {
        String[] lines = new String[2];
        lines[0] = toString(status1);
        lines[1] = String.format("%s\txxx\ttransferring", FILENAME2);
        writeLines(lines);

        Map<String, XferQueryBase.Status> res = getResults(FILENAMES);
        XferQueryBase.Status status2 = res.get(FILENAME2);
        assertEquals(XferQueryBase.Status.Code.transferring, status2.getCode());
        assertEquals(0, status2.getTime());
    }
}
