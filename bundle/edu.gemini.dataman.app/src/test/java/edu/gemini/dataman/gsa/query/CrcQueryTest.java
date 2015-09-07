//
// $
//

package edu.gemini.dataman.gsa.query;

import edu.gemini.dataman.context.TestDatamanConfig;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.*;

/**
 * Test cases for the CrcQuery.  Uses a file URL, writing first to a file
 * with the information that would come from the server.
 */

@Ignore
public class CrcQueryTest extends GsaQueryTestBase {

    public static void writeLine(TestDatamanConfig config, String filename, long crc) throws IOException {
        writeLine(config, filename, "0x" + Long.toHexString(crc));
    }

    private void writeLine(String filename, long crc) throws IOException {
        writeLine(config, filename, crc);
    }

    private void writeLine(String filename, String line) throws IOException {
        writeLine(config, filename, line);
    }

    private static void writeLine(TestDatamanConfig config, String filename, String line) throws IOException {
        File dir  = config.getCrcQueryDir();
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


    /**
     * Tests a query that returns nothing, which is what we get if the CADC
     * doesn't recognize the file.
     */
    @Test public void testNotFound() throws Exception {
        writeLine(FILENAME, "");
        CrcQuery query = new CrcQuery(config);
        assertNull(query.getCrc(FILENAME));
    }

    @Test public void testNotFoundException() throws Exception {
        CrcQuery query = new CrcQuery(config);
        assertNull(query.getCrc(FILENAME));
    }

    /**
     * Tests what happens if the output seems good but doesn't parse.
     */
    @Test public void testParseProblem() throws Exception {
        writeLine(FILENAME, "0xNotHexNumber");
        CrcQuery query = new CrcQuery(config);
        try {
            query.getCrc(FILENAME);
            fail("expected a parse exception");
        } catch (Exception ex) {
            // expected
        }
    }

    /**
     * Tests what happens if there is output, but it doesn't start with 0x.
     */
    @Test public void testNotAHexNumber() throws Exception {
        writeLine(FILENAME, "random string");
        CrcQuery query = new CrcQuery(config);
        assertNull(query.getCrc(FILENAME));
    }

    /**
     * Tests expected output.
     */
    @Test public void testExpectedOutput() throws Exception {
        writeLine(FILENAME, 1L);
        CrcQuery query = new CrcQuery(config);
        Long crc = query.getCrc(FILENAME);
        assertNotNull(crc);
        assertEquals(1L, crc.longValue());
    }

}
