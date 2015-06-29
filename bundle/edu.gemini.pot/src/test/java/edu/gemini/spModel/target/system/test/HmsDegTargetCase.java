/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: HmsDegTargetCase.java 18053 2009-02-20 20:16:23Z swalker $
 */
package edu.gemini.spModel.target.system.test;

import edu.gemini.spModel.target.system.ITarget;
import edu.gemini.spModel.target.system.HmsDegTarget;
import static edu.gemini.spModel.test.TestFile.ser;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Class SPTargetTest tests classes related to SPTarget.
 */
public final class HmsDegTargetCase {
    ITarget _t1;
    ITarget _t2;

    @Before
    public void setUp() throws Exception {
        _t1 = new HmsDegTarget();
        _t2 = new HmsDegTarget();
    }

    // Create targets of various types
    @Test
    public void testSimple() {
        HmsDegTarget t1 = new HmsDegTarget();
        assertNotNull(t1);

        t1.getRa().setValue("10:11:12.345");
        t1.getDec().setValue("-20:30:40.567");

        assertEquals(t1.getRa().toString(), "10:11:12.345");
        assertEquals(t1.getDec().toString(), "-20:30:40.57");
    }

    private void _doTestOne(String raIn, String decIn,
                            String raEx, String decEx) {
        _t1.getRa().setValue(raIn);
        _t1.getDec().setValue(decIn);
        String raOut = _t1.getRa().toString();
        String decOut = _t1.getDec().toString();

        assertEquals("Failed comparison,", raEx, raOut);
        assertEquals("Failed comparison,", decEx, decOut);
    }

    @Test
    public void testWithStrings() {
        _doTestOne("0:0:0", "-0:0:0", "00:00:00.000", "00:00:00.00");
        _doTestOne("12:13:14.5", "32:33:34.0", "12:13:14.500", "32:33:34.00");
        _doTestOne("22:13:0", "-2:33:34.0", "22:13:00.000", "-02:33:34.00");
    }

    @Test
    public void testSerialization() throws Exception {
        final HmsDegTarget outObject = new HmsDegTarget();
        outObject.getRa().setValue("10:11:12.34");
        outObject.getDec().setValue("-11:12:13.4");
        final HmsDegTarget inObject = ser(outObject);
        assertTrue(outObject.equals(inObject));
    }
}
