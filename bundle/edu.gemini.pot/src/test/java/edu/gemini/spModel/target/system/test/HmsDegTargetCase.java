/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: HmsDegTargetCase.java 18053 2009-02-20 20:16:23Z swalker $
 */
package edu.gemini.spModel.target.system.test;

import edu.gemini.spModel.target.system.CoordinateTypes.Epoch;
import edu.gemini.spModel.target.system.ITarget;
import edu.gemini.spModel.target.system.HmsDegTarget;
import edu.gemini.spModel.target.system.HMS;
import edu.gemini.spModel.target.system.ICoordinate;
import edu.gemini.spModel.target.system.DMS;
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

    private void _doB1950ConvertTest(String raIn, String decIn,
                                     String raExpect, String decExpect) {
        _t1.setC1C2(raIn, decIn);
        _t1.setSystemOption(HmsDegTarget.SystemType.B1950);
        HmsDegTarget nhms = _t1.getTargetAsJ2000();

        assertEquals(" RA failed: ", raExpect, nhms.raToString());
        assertEquals("Dec failed: ", decExpect, nhms.decToString());
    }

    private void _doJ2000ConvertTest(String raIn, String decIn,
                                     String raExpect, String decExpect) {
        // First setup a HmsDegTarget as B1950
        _t1.setSystemOption(HmsDegTarget.SystemType.B1950);

        // Create an external J2000
        _t2.setSystemOption(HmsDegTarget.SystemType.J2000);
        _t2.setC1C2(raIn, decIn);

        // Now set t1 with a J2000
        _t1.setTargetWithJ2000((HmsDegTarget) _t2);

        assertEquals(" RA failed: ", raExpect, _t1.c1ToString());
        assertEquals("Dec failed: ", decExpect, _t1.c2ToString());
    }

    // Create targets of various types
    @Test
    public void testSimple() {
        HmsDegTarget t1 = new HmsDegTarget();
        assertNotNull(t1);

        ICoordinate ra = new HMS("10:11:12.345");
        ICoordinate dec = new DMS("-20:30:40.567");
        t1.setC1C2(ra, dec);

        assertEquals(t1.raToString(), "10:11:12.345");
        assertEquals(t1.decToString(), "-20:30:40.57");
    }

    @Test
    public void testFK4ToFK5Conversion() {
        _doB1950ConvertTest("0:0:0.0", "0:0:0.0", "00:02:33.774", "00:16:42.06");
        _doB1950ConvertTest("02:0:0.0", "40:0:0.0", "02:03:02.228", "40:14:24.27");
        _doB1950ConvertTest("08:0:0.0", "20:0:0.0", "08:02:54.645", "19:51:33.54");
        _doB1950ConvertTest("10:0:0.0", "60:0:0.0", "10:03:30.546", "59:45:28.63");
        _doB1950ConvertTest("16:0:0.0", "80:0:0.0", "15:57:09.269", "79:51:33.79");
        _doB1950ConvertTest("22:0:0.0", "40:0:0.0", "22:02:05.864", "40:14:29.94");
        _doB1950ConvertTest("2:0:0.0", "-20:0:0.0", "02:02:21.575", "-19:45:34.66");
        _doB1950ConvertTest("8:0:0.0", "-40:0:0.0", "08:01:45.183", "-40:08:24.38");
        _doB1950ConvertTest("10:0:0.0", "-60:0:0.0", "10:01:35.954", "-60:14:29.75");
        _doB1950ConvertTest("16:0:0.0", "-80:0:0.0", "16:08:07.582", "-80:08:05.81");
        _doB1950ConvertTest("22:0:0.0", "-40:0:0.0", "22:03:01.376", "-39:45:28.73");
    }

    @Test
    public void testFK5ToFK4Conversion() {
        _doJ2000ConvertTest("0:0:0.0", "0:0:0.0", "23:57:26.234", "-00:16:42.28");
        _doJ2000ConvertTest("02:0:0.0", "40:0:0.0", "01:56:58.655", "39:45:28.92");
        _doJ2000ConvertTest("08:0:0.0", "20:0:0.0", "07:57:05.045", "20:08:15.54");
        _doJ2000ConvertTest("10:0:0.0", "60:0:0.0", "09:56:27.347", "60:14:23.85");
        _doJ2000ConvertTest("16:0:0.0", "80:0:0.0", "16:02:57.868", "80:08:15.32");
        _doJ2000ConvertTest("22:0:0.0", "40:0:0.0", "21:57:54.354", "39:45:34.45");
        _doJ2000ConvertTest("2:0:0.0", "-20:0:0.0", "01:57:38.377", "-20:14:30.69");
        _doJ2000ConvertTest("8:0:0.0", "-40:0:0.0", "07:58:14.810", "-39:51:42.14");
        _doJ2000ConvertTest("10:0:0.0", "-60:0:0.0", "09:58:24.203", "-59:45:33.56");
        _doJ2000ConvertTest("16:0:0.0", "-80:0:0.0", "15:52:03.654", "-79:51:23.66");
        _doJ2000ConvertTest("22:0:0.0", "-40:0:0.0", "21:56:57.744", "-40:14:24.83");
    }

    private void _doTestOne(String raIn, String decIn,
                            String raEx, String decEx) {
        _t1.setC1(raIn);
        _t1.setC2(decIn);
        String raOut = _t1.c1ToString();
        String decOut = _t1.c2ToString();

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
        outObject.setRaDec("10:11:12.34", "-11:12:13.4");
        final HmsDegTarget inObject = ser(outObject);
        assertTrue(outObject.equals(inObject));
    }
}
