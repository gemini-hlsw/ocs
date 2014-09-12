/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: CoordinateTypesCase.java 18053 2009-02-20 20:16:23Z swalker $
 */
package edu.gemini.spModel.target.system.test;

import edu.gemini.spModel.target.system.CoordinateParam.Units;
import edu.gemini.spModel.target.system.CoordinateTypes.PM1;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


/**
 * Class CoordinateTypesTest tests the CoordinateTypes and
 * CoordinateParam classes.
 * This test uses one of the CoordinateTypes to test basic
 * functionality of CoordinateParam.  All others, should work
 * the same.
 */
public final class CoordinateTypesCase {

    static final private String STRING_ZERO = "0.0";
    static final private double ZERO = 0.0;

    static final private double ERROR = .00001;

    private PM1 _t1;
    private PM1 _t2;
    private PM1 _t3;
    private PM1 _t4;
    private PM1 _t5;

    @Before
    public void setUp() throws Exception {
        // Constructors
        _t1 = new PM1();
        assertNotNull(_t1);

        _t2 = new PM1(ZERO);
        assertNotNull(_t2);

        _t3 = new PM1(ZERO, Units.MILLI_ARCSECS_PER_YEAR);
        assertNotNull(_t3);

        _t4 = new PM1(STRING_ZERO);
        assertNotNull(_t4);

        _t5 = new PM1(STRING_ZERO, Units.MILLI_ARCSECS_PER_YEAR);
        assertNotNull(_t5);

    }

    // Create targets of various types
    @Test
    public void testPM1() {
        assertTrue(_t1.equals(_t2));
        assertTrue(_t1.equals(_t3));
        assertTrue(_t1.equals(_t4));
        assertTrue(_t1.equals(_t5));
    }

    // Test for working equals using values and units
    @Test
    public void testEquals() {
        // check pre condition
        assertTrue(_t1.equals(_t2));

        _t2.setUnits(Units.SECS_PER_YEAR);
        assertTrue(!_t1.equals(_t2));

        // check pre condition
        assertTrue(_t1.equals(_t3));

        _t3.setValue(4.0);
        assertTrue(!_t1.equals(_t3));

        assertTrue(!_t3.equals(_t2));
    }

    @Test
    public void testStrings() {
        assertTrue(_t1.equals(_t2));

        _t1.setValue(2.0);
        assertTrue(!_t1.equals(_t2));

        _t2.setValue("2.0");
        assertTrue(_t1.equals(_t2));

        // Harder
        _t1.setValue(2.1234);
        assertTrue(!_t1.equals(_t2));

        _t2.setValue("2.1234");
        assertTrue(_t1.equals(_t2));
    }

    @Test
    public void testUnitsException() {
        _t1.setUnits(Units.SECS_PER_YEAR);  // No problem
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
        // Setup _t1
        _t1.setValue(5.4321);

        PM1 cl = (PM1) _t1.clone();
        assertTrue(cl.equals(_t1));
    }

    @Test
    public void testGetValues() {
        // Setup
        _t1.setValue(3.98765);
        String s1 = _t1.getStringValue();

        assertTrue(!s1.equals(_t2.getStringValue()));

        _t2.setValue(3.98765);
        assertTrue(s1.equals(_t2.getStringValue()));

        assertTrue(_t1.getValue() == _t2.getValue());
    }

    @Test
    public void testFromString() {
        // First put a value in _t1
        _t1.setValue(2.345);
        assertEquals(2.345, _t1.getValue(), ERROR);
        assertEquals(Units.MILLI_ARCSECS_PER_YEAR, _t1.getUnits());
        // Reset the units to be different
        _t1.setUnits(Units.SECS_PER_YEAR);

        // Now try fromString
        String stringOut = _t1.toString();
        PM1 t2 = new PM1();
        t2.fromString(stringOut);
        assertEquals(_t1.getValue(), t2.getValue(), ERROR);
        assertEquals(_t1.getUnits(), t2.getUnits());

        String bogus1 = "X.f[Years]";
        String bogus2 = "1.23[BadUnit]";
        String bogus3 = "1.23[Years]";
        String notbogus = "1.23[arcsecs/year]";

        // Test bogus string
        boolean success = false;
        try {
            t2.fromString(bogus1);
        } catch (IllegalArgumentException ex) {
            success = true;
        }
        assertTrue("Check for bad value", success);

        // Check for bad Unit
        success = false;
        try {
            t2.fromString(bogus2);
        } catch (IllegalArgumentException ex) {
            success = true;
        }
        assertTrue("Check for invalid unit string", success);

        // Check for improper unit type for PM1
        success = false;
        try {
            t2.fromString(bogus3);
        } catch (IllegalArgumentException ex) {
            success = true;
        }
        assertTrue("Check for PM1 unit", success);

        // Check for all good
        success = false;
        try {
            t2.fromString(notbogus);
        } catch (IllegalArgumentException ex) {
            success = true;
        }
        assertTrue("Check for good string", !success);
    }

    @Test
    public void testSerialization() throws Exception {
        _t1.setValue(5.4321);
        _t1.setUnits(Units.SECS_PER_YEAR);

        final PM1 inObject = ser(_t1);
        assertTrue(_t1.equals(inObject));
    }
}
