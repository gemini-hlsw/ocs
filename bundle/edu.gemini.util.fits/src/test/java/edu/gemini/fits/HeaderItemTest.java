//
// $Id: HeaderItemTest.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 *
 */
public final class HeaderItemTest extends TestCase {

    public void testBoolean() throws Exception {
        String expected = "SIMPLE  =                    T / file does conform to FITS standard             ";
        String comment  = "file does conform to FITS standard";

        HeaderItem c1;
        c1 = DefaultHeaderItem.create("SIMPLE", true, comment);
        _match(expected, c1);

        HeaderItem c2;
        c2 = HeaderItemFormat.parse(expected);
        assertEquals(c1, c2);

        assertEquals("SIMPLE", c1.getKeyword());
        assertTrue(c1.getBooleanValue());
        assertEquals("T", c1.getValue());
        assertEquals(comment, c1.getComment());

        try {
            c1.getIntValue();
            fail("should have thrown an exception");
        } catch (NumberFormatException ex) {
            // expected
        }
    }

    public void testInt() throws Exception {
        String expected = "BITPIX  =                   16 / number of bits per data pixel                  ";
        String comment  = "number of bits per data pixel";

        HeaderItem c1;
        c1 = DefaultHeaderItem.create("BITPIX", 16, comment);
        _match(expected, c1);

        HeaderItem c2;
        c2 = HeaderItemFormat.parse(expected);
        assertEquals(c1, c2);

        assertEquals("BITPIX", c1.getKeyword());
        assertEquals(16, c1.getIntValue());
        assertEquals("16", c1.getValue());
        assertEquals(comment, c1.getComment());
    }

    public void testEmptyString() throws Exception {
        String expected = "TRKFRAME= '        '           / Tracking co-ordinate                           ";
        _testString(expected, "TRKFRAME", "", "Tracking co-ordinate");
    }

    // sub 8 char strings are padded with blanks before the closing quote
    public void testSub8CharString() throws Exception {
        String expected = "FRAME   = 'FK5     '           / Target coordinate system                       ";
        _testString(expected, "FRAME", "FK5", "Target coordinate system");
    }

    public void test8CharString() throws Exception {
        String expected = "OIAOBJEC= '18086073'           / Object Name for OIWFS, Chop A                  ";
        _testString(expected, "OIAOBJEC", "18086073", "Object Name for OIWFS, Chop A");
    }

    // A string over 8 characters must not be padded.
    // A string can be 20 characters long before it moves the comment over.
    public void testSub20CharString() throws Exception {
        String expected = "OBSID   = 'GS-2005B-Q-4-10'    / Observation ID / Data label                    ";
        _testString(expected, "OBSID", "GS-2005B-Q-4-10", "Observation ID / Data label");
    }

    public void test20CharString() throws Exception {
        String expected = "OBSID   = 'GS-2005B-Q-4-1-001' / Observation ID / Data label                    ";
        _testString(expected, "OBSID", "GS-2005B-Q-4-1-001", "Observation ID / Data label");
    }

    public void testOver20CharString() throws Exception {
        String expected = "OBSID   = 'GS-2005B-Q-4-10-001' / Observation ID / Data label                   ";
        _testString(expected, "OBSID", "GS-2005B-Q-4-10-001", "Observation ID / Data label");
    }

    public void testLongString() throws Exception {
        // 69 character value, too long to fit when quoted
        String val = "123456789012345678901234567890123456789012345678901234567890123456789";
        try {
            DefaultHeaderItem.create("LONGSTRG", val, null);

        } catch (FitsException ex) {
            // expected
        }
    }

    public void testUnterminatedString() throws Exception {
        String expected = "OBSID   = 'GS-2005B-Q-4-10-001    Observation ID   Data label                    ";

        try {
            HeaderItemFormat.parse(expected);
            fail("should have generated an exception");
        } catch (FitsParseException ex) {
            // expected
        }
    }

    // Strings with ' characters have to be encoded with double '
    public void testEncodedString() throws Exception {
        String expected = "SSA     = 'O'' Brien'          / SSA                                            ";
        _testString(expected, "SSA", "O' Brien", "SSA");

        expected = "SSA     = '''O Brien'          / SSA                                            ";
        _testString(expected, "SSA", "'O Brien", "SSA");

        expected = "SSA     = 'O Brien'''          / SSA                                            ";
        _testString(expected, "SSA", "O Brien'", "SSA");

        expected = "SSA     = 'O'''' Brien'        / SSA                                            ";
        _testString(expected, "SSA", "O'' Brien", "SSA");
    }

    public void testLongKeyword() throws Exception {
        try {
            DefaultHeaderItem.create("123456789", "value", "comment");
            fail("was a long keyword");
        } catch (FitsException ex) {
            // expected
        }
    }

    public void testCommentTruncation() throws Exception {
        String expected = "OBSERVAT= 'Gemini-South'       / Name of telescope (Gemini-North|Gemini-South)12";
        String lcomment = "Name of telescope (Gemini-North|Gemini-South)123";
        String tcomment = "Name of telescope (Gemini-North|Gemini-South)12";

        HeaderItem c1;
        c1 = DefaultHeaderItem.create("OBSERVAT", "Gemini-South", lcomment);
        _match(expected, c1);
        assertEquals(tcomment, c1.getComment());
    }

    public void testComment() throws Exception {
        String expected = "COMMENT   FITS (Flexible Image Transport System) format defined in Astronomy and";
        String comment  = "FITS (Flexible Image Transport System) format defined in Astronomy and";
        _testComment(expected, "COMMENT", comment);

        expected = "COMMENT  FITS (Flexible Image Transport System) format defined in Astronomy and1";
        comment  = "FITS (Flexible Image Transport System) format defined in Astronomy and1";
        _testComment(expected, "COMMENT", comment);

        expected = "COMMENT FITS (Flexible Image Transport System) format defined in Astronomy and12";
        comment  = "FITS (Flexible Image Transport System) format defined in Astronomy and12";
        _testComment(expected, "COMMENT", comment);

        expected = "COMMENT FITS (Flexible Image Transport System) format defined in Astronomy and12";
        comment  = "FITS (Flexible Image Transport System) format defined in Astronomy and123";

        HeaderItem c1;
        c1 = DefaultHeaderItem.createComment("COMMENT", comment);
        _match(expected, c1);
        assertEquals(comment.substring(0, 72), c1.getComment());
    }

    public void testDouble() throws Exception {
        String expected = "EPOCH   =                2000. / Epoch for Target coordinates                   ";
        String comment  = "Epoch for Target coordinates";

        HeaderItem c1;
        c1 = DefaultHeaderItem.create("EPOCH", 2000.0, comment);
        _match(expected, c1);

        HeaderItem c2;
        c2 = HeaderItemFormat.parse(expected);
        assertEquals(c1, c2);

        assertEquals("EPOCH", c1.getKeyword());
        assertEquals(2000.0, c1.getDoubleValue());
        assertEquals("2000.", c1.getValue());
        assertEquals(comment, c1.getComment());
    }

    private static void _testComment(String expected, String key, String comment)
            throws Exception {

        HeaderItem c1;
        c1 = DefaultHeaderItem.createComment(key, comment);
        _match(expected, c1);

        HeaderItem c2;
        c2 = HeaderItemFormat.parse(expected);
        assertEquals(c1, c2);

        assertEquals(key, c1.getKeyword());
        assertNull(c1.getValue());
        assertEquals(comment, c1.getComment());
    }

    private static void _testString(String expected, String key, String val, String comment)
            throws Exception {

        HeaderItem c1;
        c1 = DefaultHeaderItem.create(key, val, comment);
        _match(expected, c1);

        HeaderItem c2;
        c2 = HeaderItemFormat.parse(expected);
        assertEquals(c1, c2);

        assertEquals(key, c1.getKeyword());
        assertEquals(val, c1.getValue());
        assertEquals(comment, c1.getComment());
    }

    private static void _match(String expected, HeaderItem item) throws Exception {
        byte[] expBytes = expected.getBytes(FitsConstants.CHARSET_NAME);
        byte[] actBytes = HeaderItemFormat.toBytes(item);
//        System.out.println("expBytes = " + (new String(expBytes)));
//        System.out.println("actBytes = " + (new String(actBytes)));
        assertTrue(Arrays.equals(expBytes, actBytes));
    }
}
