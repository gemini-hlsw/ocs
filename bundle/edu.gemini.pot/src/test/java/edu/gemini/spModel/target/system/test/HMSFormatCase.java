/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file LICENSE for complete details.
 *
 * $Id: HMSFormatCase.java 21620 2009-08-20 19:41:32Z swalker $
 */
package edu.gemini.spModel.target.system.test;

import edu.gemini.spModel.target.system.FormatSeparator;
import edu.gemini.spModel.target.system.HMSFormat;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Class HMSFormatTest tests the basic HMSFormat class which
 * handles formatting output of an HHMMSS
 */
public final class HMSFormatCase {
    private static final double _ERROR = 0.0001;

    private void _doStringTest(double posIn, String expected) {
        //System.out.println("Test: " + posIn);

        String posOut = (new HMSFormat()).format(posIn);
        assertEquals("Failed comparison,", expected, posOut);
    }

    private void _doStringTest2(double posIn, String expected, int precision) {
        //System.out.println("PTest: " + posIn + " " + precision);
        HMSFormat t1 = new HMSFormat(precision);
        String posOut = t1.format(posIn);
        assertEquals("Failed comparison,", expected, posOut);
    }

    private void _doBothTest(String posIn, String expected, int precision) {
        //System.out.println("BTest: " + posIn + " " + precision);
        HMSFormat t1 = new HMSFormat(precision);
        double din = t1.parse(posIn);
        String posOut = t1.format(din);
        assertEquals("Failed comparison,", expected, posOut);
    }

    private void _doXMSTest(int h, int m, double s,
                            String expected, int precision) {
        //System.out.println("BTest: " + posIn + " " + precision);
        HMSFormat t1 = new HMSFormat(precision);
        double din = t1.parse(h, m, s);
        String posOut = t1.format(din);
        assertEquals("Failed comparison,", expected, posOut);
    }

    @Test
    public void testBasics() {
        _doStringTest(0, "00:00:00.000");
        _doStringTest(15.0, "01:00:00.000");
        _doStringTest(22.5, "01:30:00.000");
        _doStringTest(180.0, "12:00:00.000");
        _doStringTest(192.234, "12:48:56.160");
        _doStringTest(450.0, "06:00:00.000");
    }

    @Test
    public void testPrecision() {
        _doStringTest2(0, "00:00:00.00", 2);
        _doStringTest2(15.0, "01:00:00.00", 2);
        _doStringTest2(22.5, "01:30:00.0", 1);
        _doStringTest2(180.0, "12:00:00.0000", 4);
        _doStringTest2(192.234, "12:48:56.2", 1);
    }

    @Test
    public void testBothWays() {
        _doBothTest("0:0:0", "00:00:00.00", 2);
        _doBothTest("00:00:00.0", "00:00:00.00", 2);
        _doBothTest("0:01:02", "00:01:02.00", 2);
        _doBothTest("0:1:2", "00:01:02.00", 2);
        _doBothTest("1:01:02.34567", "01:01:02.3", 1);
        // Note that this rounds differently than positive?
        _doBothTest("20:10:3.5555", "20:10:03.556", HMSFormat.DEFAULT_PRECISION);
        _doBothTest("20:10:3.5555", "20:10:03.556", HMSFormat.DEFAULT_PRECISION);
        _doBothTest("20:10:3.4544", "20:10:03.454", HMSFormat.DEFAULT_PRECISION);
        _doBothTest("10:1:2.344", "10:01:02.3", 1);
        _doBothTest("10:01:0", "10:01:00.0", 1);
        _doBothTest("10:01:02.0", "10:01:02.00", 2);
        _doBothTest("10:01:02.345678", "10:01:02.35", 2);
        _doBothTest("10:01:02.345678", "10:01:02.3457", 4);
        _doBothTest("10:11:12.3440", "10:11:12.3440", 4);
        _doBothTest("10:60:60.345", "11:01:00.35", 2);
        _doBothTest("-00:00:00.0", "00:00:00.0", 1);
        _doBothTest("-0:01:02", "23:58:58.0", 1);
        _doBothTest("-10:60:60.345", "12:58:59.655", 3);
        _doBothTest("10:60:60.345", "11:01:00.345", 3);
        _doBothTest("23:60:60.345", "00:01:00.345", 3);
    }

    @Test
    public void testXMS() {
        _doXMSTest(0, 0, 0, "00:00:00.00", 2);
        _doXMSTest(10, 23, 14.234, "10:23:14.23", 2);
        _doXMSTest(10, 23, 14.234, "10:23:14.234", 3);
        _doXMSTest(25, 1, 22.12345, "01:01:22.1234", 4);
    }

    @Test
    public void testParse() {
        // Use _t1
        HMSFormat t1 = new HMSFormat();
        double d = t1.parse("12:48:56.16");
        assertEquals(192.234, d, _ERROR);
        d = t1.parse("12 48 56.16");
        assertEquals(192.234, d, _ERROR);
        d = t1.parse("12h48m56.16s");
        assertEquals(192.234, d, _ERROR);
        d = t1.parse("1:00:02");
        assertEquals(15.0083, d, _ERROR);
    }

    @Test
    public void testSeparator() {
        HMSFormat t1 = new HMSFormat(FormatSeparator.COLON, 2);
        String posOut = t1.format(192.234);
        assertEquals("Failed comparison,", posOut, "12:48:56.16");

        t1 = new HMSFormat(FormatSeparator.SPACES, 2);
        posOut = t1.format(192.234);
        assertEquals("Failed comparison,", posOut, "12 48 56.16");

        t1 = new HMSFormat(FormatSeparator.LETTERS, 2);
        posOut = t1.format(192.234);
        assertEquals("Failed comparison,", posOut, "12h48m56.16s");
    }

    @Test
    public void testEquals() {
        HMSFormat t2 = new HMSFormat();

        // Check separator difference
        HMSFormat t3 = new HMSFormat(FormatSeparator.SPACES, 3);
        assertTrue(!t3.equals(t2));

        // Check precision difference
        HMSFormat t4 = new HMSFormat(FormatSeparator.COLON, 2);
        assertTrue(!t4.equals(t3));

        // Check both
        HMSFormat t5 = new HMSFormat(FormatSeparator.LETTERS, 5);
        HMSFormat t6 = new HMSFormat(FormatSeparator.LETTERS, 5);
        assertTrue(t5.equals(t6));
        assertTrue(!t5.equals(t2));
    }

    @Test
    public void testClone() {
        HMSFormat t1 = new HMSFormat(FormatSeparator.LETTERS, 5);
        HMSFormat t2 = (HMSFormat) t1.clone();
        assertTrue(t2.equals(t1));
        // Just paranoid
        assertTrue(t2.getPrecision() == t1.getPrecision());
        assertTrue(t1.getSeparator() == t2.getSeparator());
    }

    @Test
    public void testSerialization() throws Exception {
        final HMSFormat outObject = new HMSFormat(FormatSeparator.SPACES);
        final HMSFormat inObject = ser(outObject);
        assertTrue(outObject.equals(inObject));
    }
}
