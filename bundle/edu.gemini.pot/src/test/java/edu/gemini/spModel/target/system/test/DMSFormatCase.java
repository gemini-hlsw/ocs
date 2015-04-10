/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: DMSFormatCase.java 21620 2009-08-20 19:41:32Z swalker $
 */
package edu.gemini.spModel.target.system.test;

import edu.gemini.spModel.target.system.DMSFormat;
import edu.gemini.spModel.target.system.FormatSeparator;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Class DMSFormatTest tests the basic DMSFormat class which
 * handles formatting output of an HHMMSS
 */
public final class DMSFormatCase {

    private void _doStringTest(double posIn, String expected) {
        //System.out.println("Test: " + posIn);
        String posOut = (new DMSFormat()).format(posIn);
        assertEquals("Failed comparison,", expected, posOut);
    }

    private void _doStringTest2(double posIn, String expected, int precision) {
        //System.out.println("PTest: " + posIn + " " + precision);
        DMSFormat t1 = new DMSFormat(precision);
        String posOut = t1.format(posIn);
        assertEquals("Failed comparison,", expected, posOut);
    }

    private void _doBothTest(String posIn, String expected, int precision) {
        //System.out.println("BTest: " + posIn + " " + precision);
        DMSFormat t1 = new DMSFormat(precision);
        double din = t1.parse(posIn);
        String posOut = t1.format(din);
        assertEquals("Failed comparison,", expected, posOut);
    }

    private void _doXMSTest(int d, int m, double s,
                            String expected, int precision) {
        //System.out.println("BTest: " + posIn + " " + precision);
        DMSFormat t1 = new DMSFormat(precision);
        double din = t1.parse(d, m, s);
        String posOut = t1.format(din);
        assertEquals("Failed comparison,", expected, posOut);
    }

    @Test
    public void testBasics() {
        _doStringTest(0, "00:00:00.00");
        _doStringTest(-10.5, "-10:30:00.00");
        _doStringTest(15.0, "15:00:00.00");
        _doStringTest(-15.0, "-15:00:00.00");
        _doStringTest(22.5, "22:30:00.00");
        _doStringTest(-82.345, "-82:20:42.00");
        _doStringTest(89.234, "89:14:02.40");
        _doStringTest(45.250, "45:15:00.00");
        _doStringTest(220.0, "220:00:00.00");
        _doStringTest(320.0, "320:00:00.00");
        _doStringTest(4320.0, "4320:00:00.00");
        _doStringTest(54320.0, "54320:00:00.00");
        _doStringTest(-220.0, "-220:00:00.00");
        _doStringTest(-320.0, "-320:00:00.00");
        _doStringTest(-4320.0, "-4320:00:00.00");
        _doStringTest(-54320.0, "-54320:00:00.00");
    }

    @Test
    public void testSeparator() {
        DMSFormat t1 = new DMSFormat(FormatSeparator.COLON, 2);
        String posOut = t1.format(19.234);
        assertEquals("Failed comparison,", "19:14:02.40", posOut);

        t1 = new DMSFormat(FormatSeparator.SPACES, 2);
        posOut = t1.format(19.234);
        assertEquals("Failed comparison,", "19 14 02.40", posOut);

        posOut = t1.format(12349.234);
        assertEquals("Failed comparison,", "12349 14 02.40", posOut);

        t1 = new DMSFormat(FormatSeparator.LETTERS, 2);
        posOut = t1.format(12349.234);
        assertEquals("Failed comparison,", "12349d14m02.40s", posOut);

        posOut = t1.format(19.234);
        assertEquals("Failed comparison,", "19d14m02.40s", posOut);

        // Check for tricky negatives!
        t1 = new DMSFormat(FormatSeparator.COLON, 2);
        posOut = t1.format(-19.234);
        assertEquals("Failed comparison,", "-19:14:02.40", posOut);

        t1 = new DMSFormat(FormatSeparator.SPACES, 2);
        posOut = t1.format(-19.234);
        assertEquals("Failed comparison,", "-19 14 02.40", posOut);

        t1 = new DMSFormat(FormatSeparator.LETTERS, 2);
        posOut = t1.format(-19.234);
        assertEquals("Failed comparison,", "-19d14m02.40s", posOut);

        t1 = new DMSFormat(FormatSeparator.SPACES, 2);
        posOut = t1.format(-12349.234);
        assertEquals("Failed comparison,", "-12349 14 02.40", posOut);

        t1 = new DMSFormat(FormatSeparator.LETTERS, 2);
        posOut = t1.format(-12349.234);
        assertEquals("Failed comparison,", "-12349d14m02.40s", posOut);

    }

    @Test
    public void testPrecision() {
        _doStringTest2(0, "00:00:00.00", 2);
        _doStringTest2(15.0, "15:00:00.00", 2);
        _doStringTest2(22.5, "22:30:00.0", 1);
        _doStringTest2(1222.5, "1222:30:00.0", 1);
        _doStringTest2(-80.33, "-80:19:48.0000", 4);
        _doStringTest2(-45.234, "-45:14:02.4", 1);
        _doStringTest2(-1222.5, "-1222:30:00.0", 1);
        _doStringTest2(-1222.5, "-1222:30:00.0000", 4);
    }

    @Test
    public void testBothWays() {
        _doBothTest("0:0:0", "00:00:00.00", 2);
        _doBothTest("00:00:00.0", "00:00:00.00", 2);
        _doBothTest("0:01:02", "00:01:02.00", 2);
        _doBothTest("0:1:2", "00:01:02.00", 2);
        _doBothTest("1:01:02.34567", "01:01:02.3", 1);
        _doBothTest("10:1:2.344", "10:01:02.3", 1);
        _doBothTest("-20:10:3.5", "-20:10:03.50", DMSFormat.DEFAULT_PRECISION);
        _doBothTest("-20:10:3.52", "-20:10:03.52", DMSFormat.DEFAULT_PRECISION);
        // Note that this rounds differently than positive?
        _doBothTest("-20:10:3.555", "-20:10:03.55", DMSFormat.DEFAULT_PRECISION);
        _doBothTest("20:10:3.555", "20:10:03.55", DMSFormat.DEFAULT_PRECISION);
        _doBothTest("10:01:0", "10:01:00.0", 1);
        _doBothTest("10:01:02.0", "10:01:02.00", 2);
        _doBothTest("10:01:02.345678", "10:01:02.35", 2);
        _doBothTest("10:01:02.345678", "10:01:02.3457", 4);
        _doBothTest("10:11:12.3440", "10:11:12.3440", 4);
        _doBothTest("10:60:60.345", "11:01:00.35", 2);
        _doBothTest("-00:00:00.0", "00:00:00.0", 1);
        _doBothTest("-0:01:02", "-00:01:02.0", 1);
        _doBothTest("-10:60:60.345", "-11:01:00.345", 3);
        _doBothTest("10:60:60.345", "11:01:00.345", 3);
    }

    // Test DMS parse
    @Test
    public void testXMS() {
        _doXMSTest(0, 0, 0, "00:00:00.00", 2);
        _doXMSTest(10, 23, 14.234, "10:23:14.23", 2);
        _doXMSTest(10, 23, 14.234, "10:23:14.234", 3);
        _doXMSTest(-60, 1, 22.12345, "-60:01:22.1234", 4);
    }


    // Test for working equals using values and units
    @Test
    public void testEquals() {
        DMSFormat t2 = new DMSFormat();

        // Check separator difference
        DMSFormat t3 = new DMSFormat(FormatSeparator.SPACES, 3);
        assertTrue(!t3.equals(t2));

        // Check precision difference
        DMSFormat t4 = new DMSFormat(FormatSeparator.COLON, 2);
        assertTrue(!t4.equals(t3));

        // Check both
        DMSFormat t5 = new DMSFormat(FormatSeparator.LETTERS, 5);
        DMSFormat t6 = new DMSFormat(FormatSeparator.LETTERS, 5);
        assertTrue(t5.equals(t6));
        assertTrue(!t5.equals(t2));
    }

    @Test
    public void testClone() {
        DMSFormat t1 = new DMSFormat(FormatSeparator.LETTERS, 5);
        DMSFormat t2 = (DMSFormat) t1.clone();
        assertTrue(t2.equals(t1));
        // Just paranoid
        assertTrue(t2.getPrecision() == t1.getPrecision());
        assertTrue(t1.getSeparator() == t2.getSeparator());
    }

    @Test
    public void testSerialization() throws Exception {
        final DMSFormat outObject = new DMSFormat(FormatSeparator.SPACES);
        final DMSFormat inObject = ser(outObject);
        assertTrue(outObject.equals(inObject));
    }
}
