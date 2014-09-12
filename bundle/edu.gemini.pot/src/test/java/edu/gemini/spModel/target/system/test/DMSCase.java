/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: DMSCase.java 18053 2009-02-20 20:16:23Z swalker $
 */
package edu.gemini.spModel.target.system.test;

import edu.gemini.spModel.target.system.CoordinateParam.Units;
import edu.gemini.spModel.target.system.DMS;
import edu.gemini.spModel.target.system.DMSFormat;
import edu.gemini.spModel.target.system.FormatSeparator;

import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Class DMSTest tests the basic DMS class which
 * represents a piece of a coordinate in hours minutes and seconds.
 */
public final class DMSCase {
    // The error value used to check "closeness"
    private static final double _ERROR = 0.000001;

    private DMS _t1;
    private DMS _t2;

    @Before
    public void setUp() throws Exception {
        _t1 = new DMS();
        _t2 = new DMS();
    }

    private void _doStringTest(String posIn, String expected) {
        DMS t1 = new DMS(posIn);
        String posOut = t1.toString();
        //System.out.println("in/out: " + posIn + "/" + posOut);
        assertEquals("Failed comparison,", expected, posOut);
    }

    private void _doStringTest2(String posIn, String expected, int precision) {
        DMSFormat nf = new DMSFormat(precision);
        DMS t1 = new DMS(posIn);
        t1.setFormat(nf);
        String posOut = t1.toString();
        //System.out.println("in/out: " + posIn + "/" + posOut);
        assertEquals("Failed comparison,", expected, posOut);
    }

    // Basic tests
    @Test
    public void testBasics() {
        _doStringTest("0:0:0", "00:00:00.00");
        _doStringTest("00:00:00.0", "00:00:00.00");
        _doStringTest("0:01:02", "00:01:02.00");
        _doStringTest("0:1:2", "00:01:02.00");
        _doStringTest("1:01:02.344567", "01:01:02.34");
        _doStringTest("10:1:2.34457", "10:01:02.34");
        _doStringTest("10:01:0", "10:01:00.00");
        _doStringTest("10:01:02.0", "10:01:02.00");
        _doStringTest("10:01:02.3445", "10:01:02.34");
        _doStringTest("10:01:59.3445", "10:01:59.34");
        _doStringTest("10:01:60.3445", "10:02:00.34");
        _doStringTest("10:60:60.3445", "11:01:00.34");
        _doStringTest("10:11:12.3440", "10:11:12.34");
        _doStringTest("-20:30:40.567", "-20:30:40.57");
        _doStringTest("10:60:60.345", "11:01:00.34");
        _doStringTest("100:00:00.21", "79:59:59.79");
        _doStringTest("-100:00:00.21", "-79:59:59.79");
        _doStringTest("1500:20:30.21", "60:20:30.21");
        _doStringTest("-1500:20:30.21", "-60:20:30.21");
    }

    @Test
    public void testLatitudeBehaviour() {
        _doStringTest("100:00:00.0", "80:00:00.00");
        _doStringTest("-100:00:00.0", "-80:00:00.00");
        _doStringTest("300:00:00.0", "-60:00:00.00");
        _doStringTest("-300:00:00.0", "60:00:00.00");
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
        _doStringTest2("-0:01:02", "-00:01:02.0", 1);
        _doStringTest2("-10:60:60.345", "-11:01:00.35", 2);
        _doStringTest2("10:60:60.345", "11:01:00.345", 3);

    }

    // Test the DMS getters
    @Test
    public void testGetValues() {
        String posIn = "10:11:12.3456";
        DMS t1 = new DMS(posIn);

        assertEquals(10, t1.getDegrees());
        assertEquals(11, t1.getMinutes());
        assertEquals(12.3456, t1.getSeconds(), _ERROR);

//        DMS t2 = new DMS(150.1868);
//        assertEquals(10, t1.getDegrees());
//        assertEquals(11, t1.getMinutes());
//        assertEquals(12.3456, t1.getSeconds(), _ERROR);
    }

    @Test
    public void testSeparator() {
        String posIn = "10:11:12.3456";
        DMS t1 = new DMS(posIn);
        String posOut = t1.toString();
        assertEquals("Failed comparison,", posOut, "10:11:12.35");

        DMSFormat f = new DMSFormat(FormatSeparator.SPACES);
        t1.setFormat(f);
        posOut = t1.toString();
        assertEquals("Failed comparison,", posOut, "10 11 12.35");

        f = new DMSFormat(FormatSeparator.LETTERS);
        t1.setFormat(f);
        posOut = t1.toString();
        assertEquals("Failed comparison,", posOut, "10d11m12.35s");
    }

    // Test for working equals using values and units
    @Test
    public void testEquals() {
        assertTrue(_t1.equals(_t2));

        DMS t3 = new DMS(87.5);
        DMS t4 = new DMS("87.5");
        assertTrue(t4.equals(t3));

        DMS t5 = new DMS(-20.417);
        DMS t6 = new DMS("-20.417");
        assertTrue(t5.equals(t6));

        assertTrue(!t3.equals(t5));
        DMS t7 = new DMS(-20.417);
        assertTrue(t5.equals(t7));
    }

    // Test valid units exception
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
        DMS t1 = new DMS("00:21:23.4567");
        DMS t2 = (DMS) t1.clone();

        assertTrue(t2.equals(t1));
    }

    @Test
    public void testSerialization() throws Exception {
        final DMSFormat fmt = new DMSFormat(FormatSeparator.SPACES);
        final DMS outObject = new DMS("21:22:34.50");
        outObject.setFormat(fmt);
        final DMS inObject = ser(outObject);
        assertTrue(outObject.equals(inObject));
    }
}
