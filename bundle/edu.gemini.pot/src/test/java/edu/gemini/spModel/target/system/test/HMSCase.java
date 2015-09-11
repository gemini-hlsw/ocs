/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file LICENSE for complete details.
 *
 * $Id: HMSCase.java 18053 2009-02-20 20:16:23Z swalker $
 */
package edu.gemini.spModel.target.system.test;

import edu.gemini.spModel.target.system.CoordinateParam.Units;
import edu.gemini.spModel.target.system.HMS;
import edu.gemini.spModel.target.system.HMSFormat;
import edu.gemini.spModel.target.system.FormatSeparator;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Class HMSTest tests the basic HMS class which
 * represents a piece of a coordinate in hours minutes and seconds.
 */
public final class HMSCase {
    // The error value used to check "closeness"
    private static final double _ERROR = 0.000001;

    private HMS _t1;
    private HMS _t2;

    @Before
    public void setUp() throws Exception {
        _t1 = new HMS();
        _t2 = new HMS();
    }

    private void _doStringTest(String posIn, String expected) {
        //System.out.println("Test: " + posIn);
        HMS t1 = new HMS(posIn);
        String posOut = t1.toString();
        assertEquals("Failed comparison,", expected, posOut);
    }

    private void _doStringTest2(String posIn, String expected, int precision) {
        //System.out.println("PTest: " + posIn + " " + precision);
        HMSFormat nf = new HMSFormat(precision);
        HMS t1 = new HMS(posIn);
        t1.setFormat(nf);
        String posOut = t1.toString();
        assertEquals("Failed comparison,", expected, posOut);
    }

    @Test
    public void testBasics() {
        _doStringTest("0:0:0", "00:00:00.000");
        _doStringTest("00:00:00.0", "00:00:00.000");
        _doStringTest("0:01:02", "00:01:02.000");
        _doStringTest("0:1:2", "00:01:02.000");
        _doStringTest("1:01:02.34567", "01:01:02.346");
        _doStringTest("10:1:2.3457", "10:01:02.346");
        _doStringTest("10:01:0", "10:01:00.000");
        _doStringTest("10:01:02.0", "10:01:02.000");
        // Here's a bit of interesting rounding.  I'm not totally sold that
        // this is correct.
        _doStringTest("10:01:02.3467", "10:01:02.347");  // Okay
        _doStringTest("10:01:59.3451", "10:01:59.345"); // Okay
        _doStringTest("10:01:59.3445", "10:01:59.345");  // Okay?  59.35?
        _doStringTest("10:01:60.346", "10:02:00.346");
        _doStringTest("10:60:60.345", "11:01:00.345");
        _doStringTest("10:11:12.3440", "10:11:12.344");
        _doStringTest("10:60:60.3445", "11:01:00.344");
        _doStringTest("-00:00:00.0", "00:00:00.000");
        _doStringTest("-0:01:02", "23:58:58.000");
        _doStringTest("-10:60:60.345", "12:58:59.655");
    }

    @Test
    public void testCreateNewFormat() {
        _doStringTest2("0:0:0", "00:00:00.00", 2);
        _doStringTest2("00:00:00.0", "00:00:00.00", 2);
        _doStringTest2("0:01:02", "00:01:02.00", 2);
        _doStringTest2("0:1:2", "00:01:02.00", 2);
        _doStringTest2("1:01:02.34567", "01:01:02.3", 1);
        _doStringTest2("10:1:2.344", "10:01:02.3", 1);
        _doStringTest2("10:01:0", "10:01:00.00", 2);
        _doStringTest2("10:01:02.0", "10:01:02.00", 2);
        _doStringTest2("10:01:02.345678", "10:01:02.35", 2);
        _doStringTest2("10:01:02.345678", "10:01:02.3457", 4);
        _doStringTest2("10:11:12.3440", "10:11:12.3440", 4);
        _doStringTest2("10:60:60.344", "11:01:00.34", 2);
        _doStringTest2("10:60:60.346", "11:01:00.35", 2);
        _doStringTest2("-00:00:00.0", "00:00:00.0", 1);
        _doStringTest2("-0:01:02", "23:58:58.0", 1);
        _doStringTest2("-10:60:60.345", "12:58:59.65", 2); // was 66
        _doStringTest2("10:60:60.345", "11:01:00.345", 3);

    }

    @Test
    public void testGetValues() {
        String posIn = "10:11:12.3456";
        HMS t1 = new HMS(posIn);

        assertEquals(10, t1.getHours());
        assertEquals(11, t1.getMinutes());
        assertEquals(12.3456, t1.getSeconds(), _ERROR);

//        HMS t2 = new HMS(150.1868);
//        assertEquals(10, t1.getHours());
//        assertEquals(11, t1.getMinutes());
//        assertEquals(12.3456, t1.getSeconds(), _ERROR);
    }

    @Test
    public void testSeparator() {
        String posIn = "10:11:12.3456";
        HMS t1 = new HMS(posIn);
        String posOut = t1.toString();
        assertEquals("Failed comparison,", posOut, "10:11:12.346");

        HMSFormat f = new HMSFormat(FormatSeparator.SPACES);
        t1.setFormat(f);
        posOut = t1.toString();
        assertEquals("Failed comparison,", posOut, "10 11 12.346");

        f = new HMSFormat(FormatSeparator.LETTERS);
        t1.setFormat(f);
        posOut = t1.toString();
        assertEquals("Failed comparison,", posOut, "10h11m12.346s");
    }

    @Test
    public void testEquals() {
        assertTrue(_t1.equals(_t2));

        HMS t3 = new HMS(187.5);
        HMS t4 = new HMS("12:30:00");
        assertTrue(t4.equals(t3));

        HMS t5 = new HMS(220.2572);
        HMS t6 = new HMS(220.2572);
        assertTrue(t5.equals(t6));
        assertTrue(!t3.equals(t5));
        HMS t7 = new HMS("14:41:01.728");
        assertTrue(t5.equals(t7));
    }

    @Test
    public void testUnitsException() {
        _t1.setUnits(Units.DEGREES);  // No problem
        try {
            _t1.setUnits(Units.YEARS);
        } catch (IllegalArgumentException ex) {
            // Good!
            return;
        }
        fail("Did not catch IllegalArgumentException for units.\n");
    }

    @Test
    public void testClone() {
        HMS t1 = new HMS("00:21:23.4567");
        HMS t2 = (HMS) t1.clone();

        assertTrue(t2.equals(t1));
    }

    @Test
    public void testSerialization() throws Exception {
        final HMSFormat fmt = new HMSFormat(FormatSeparator.SPACES);
        final HMS outObject = new HMS("21:22:34.50");
        outObject.setFormat(fmt);
        final HMS inObject = ser(outObject);
        assertTrue(outObject.equals(inObject));
    }
}
