//
// $
//

package edu.gemini.catalog.skycat.binding.adapter;

import edu.gemini.catalog.skycat.SkycatOutputAdapter;
import edu.gemini.skycalc.DDMMSS;
import edu.gemini.skycalc.HHMMSS;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;

/**
 *
 */
public class Gsc2StsciOutputAdapterTest {

    private void verify(String expected, String catalogOutput) throws Exception {
        StringReader   sr = new StringReader(catalogOutput);
        BufferedReader br = new BufferedReader(sr);
        SkycatOutputAdapter adapt;
        adapt = Gsc2StsciOutputAdapter.FACTORY.create(br);

        StringBuilder buf = new StringBuilder();
        String line;
        while ((line = adapt.readLine()) != null) {
            buf.append(line).append('\n');
        }
        assertEquals(expected, buf.toString());
    }

    public static String withOutputHeader(String s) {
        return "   HSTGSID      GSC1ID  H  M  S      D  M  S    Rerr Derr  Epoch      Fpg             Jpg             Npg             U               B               V               R               I              c     status    a    PA      e    V M\n" + s;
    }

    public static String withAdaptedHeader(String s) {
        return Gsc2StsciOutputAdapter.HEADER_LINE + "\n-\n" + s;
    }

    @Test
    public void testEmpty() throws Exception {
        verify("", "");
    }

    @Test
    public void testHeader() throws Exception {
        // Any single line of output will generate a header.
        verify(withAdaptedHeader(""), "abc");
    }

    @Test
    public void testInvalid() throws Exception {
        // If there is a second line, it must be valid or you will only get
        // the header.
        verify(withAdaptedHeader(""),"abc\n123");
    }

    // Generates a valid line with the given id and
    private String outputLine(String id, String raH, String raM, String raS, String decD, String decM, String decS) {
        return outputLine(id, true, raH, raM, raS, decD, decM, decS);
    }
    private String outputLine(String id, boolean blankGscId, String raH, String raM, String raS, String decD, String decM, String decS) {
        StringBuilder buf = new StringBuilder();
        buf.append(id).append(" ");
        if (blankGscId) {
            buf.append(" "); // second ignored id
        } else {
            buf.append("X").append(id).append("  "); // second ignored id
        }
        buf.append(raH).append("\t");
        buf.append(raM).append("\t\t\t");
        buf.append(raS).append(" ");
        buf.append(decD).append(" ");
        buf.append(decM).append(" ");
        buf.append(decS).append(" ");
        buf.append("99.99 99.99 2000.000 ");
        for (int i=0; i<8; ++i) {
            String s = String.format("%d.%d%d", i, i, i);
            buf.append(String.format("%s %s %s ", s, s, s));
        }
        buf.append("0 00000000 0.0 0.0 0.00 0 0\n");

        return buf.toString();
    }

    private String adaptedLine(String id, String raStr, String decStr) throws Exception {
        StringBuilder buf = new StringBuilder();

        buf.append(id).append("\t");
        buf.append(String.format("%.7f", HHMMSS.parse(raStr).getMagnitude())).append("\t");
        buf.append(String.format("%.7f", DDMMSS.parse(decStr).getMagnitude())).append("\t");
        buf.append("2000.000\t");
        for (int i=0; i<8; ++i) {
            String s = String.format("%d.%d%d", i, i, i);
            buf.append(String.format("%s\t", s));
        }
        buf.append("0\t00000000\t0.0\t0.0\t0.00\t0\t0\n");

        return buf.toString();
    }

    @Test
    public void testSingleOutput() throws Exception {
        String adapted = adaptedLine("HST001", "03:30:15.5", "4:30:40");
        String outline = outputLine("HST001", true, "03", "30", "15.5", "+04", "30", "40");
        verify(withAdaptedHeader(adapted), withOutputHeader(outline));

        outline = outputLine("HST001", false, "03", "30", "15.5", "+04", "30", "40");
        verify(withAdaptedHeader(adapted), withOutputHeader(outline));
    }

    @Test
    public void testDecParsing() throws Exception {
        StringBuilder buf = new StringBuilder();
        buf.append(adaptedLine("HST1", "03:04:05.6", "07:08:09.1"));
        buf.append(adaptedLine("HST2", "03:04:05.6", "07:08:09.1"));
        buf.append(adaptedLine("HST3", "03:04:05.6", "07:08:09.1"));
        buf.append(adaptedLine("HST4", "03:04:05.6", "-07:08:09.1"));
        buf.append(adaptedLine("HST5", "03:04:05.6", "-07:08:09.1"));
        String expected = withAdaptedHeader(buf.toString());

        buf = new StringBuilder();
        buf.append(outputLine("HST1", "03", "04", "05.6", "7",   "08", "09.1"));
        buf.append(outputLine("HST2", "03", "04", "05.6", "07",  "08", "09.1"));
        buf.append(outputLine("HST3", "03", "04", "05.6", "+07", "08", "09.1"));
        buf.append(outputLine("HST4", "03", "04", "05.6", "-7",  "08", "09.1"));
        buf.append(outputLine("HST5", "03", "04", "05.6", "-07", "08", "09.1"));
        String output = withOutputHeader(buf.toString());

        verify(expected, output);
    }
}
