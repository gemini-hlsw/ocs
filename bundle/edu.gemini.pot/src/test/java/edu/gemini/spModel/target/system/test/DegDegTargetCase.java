/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: DegDegTargetCase.java 18053 2009-02-20 20:16:23Z swalker $
 */
package edu.gemini.spModel.target.system.test;

import edu.gemini.spModel.target.system.DegDegTarget;
import edu.gemini.spModel.target.system.HmsDegTarget;
import edu.gemini.spModel.target.system.DMSLong;
import edu.gemini.spModel.target.system.DMS;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


/**
 * Class DegDegTargetTest tests the DegDegTarget coordinate class.
 */
public final class DegDegTargetCase {

    DegDegTarget _t1;
    DegDegTarget _t2;

    @Before
    public void setUp() throws Exception {
        _t1 = new DegDegTarget();
        _t2 = new DegDegTarget();
    }

    private void _doFK5ToGalacticTest(String raIn, String decIn,
                                      String longExpect, String latExpect) {
        // First setup a HmsDegTarget as J2000
        HmsDegTarget hms = new HmsDegTarget(HmsDegTarget.SystemType.J2000);
        hms.setRaDec(raIn, decIn);

        // Now set t1 with a J2000
        _t1.setTargetWithJ2000(hms);

        assertEquals(" l failed: ", longExpect, _t1.c1ToString());
        assertEquals(" b failed: ", latExpect, _t1.c2ToString());
    }


    // Used to convert DegDeg to J2000
    private void _doFk5ConvertTest(String longIn, String latIn,
                                   String raExpect, String decExpect) {
        _t1.setC1C2(longIn, latIn);
        _t1.setSystemOption(DegDegTarget.SystemType.GALACTIC);
        HmsDegTarget hms = _t1.getTargetAsJ2000();

        assertEquals(" RA failed: ", raExpect, hms.raToString());
        assertEquals("Dec failed: ", decExpect, hms.decToString());
    }

    // Create targets of various types
    @Test
    public void testSimple() {
        DegDegTarget t1 = new DegDegTarget();
        assertNotNull(t1);

        // Alt/Az
        DMS c1 = new DMSLong("233:11:12.345");
        DMS c2 = new DMS("-20:30:40.567");
        t1.setC1C2(c1, c2);

        assertEquals("233:11:12.34", t1.c1ToString());
        assertEquals("-20:30:40.57", t1.c2ToString());
    }

    @Test
    public void testGalacticToFK5Conversion() {
        //                 long      lat        ra            dec
        _doFk5ConvertTest("0:0:0.0", "0:0:0.0", "17:45:37.199", "-28:56:10.22");
        _doFk5ConvertTest("0:0:0.0", "90:0:0.0", "12:51:26.275", "27:07:41.70");
        _doFk5ConvertTest("33:0:0.0", "0:0:0.0", "18:51:33.726", "00:03:38.13");
        _doFk5ConvertTest("123:0:0.0", "27:24:0.0", "12:01:16.850", "89:43:17.74");
        _doFk5ConvertTest("240:0:0.0", "-60:00:0.0", "03:06:19.929", "-36:40:58.90");
    }

    @Test
    public void testFk5ToGalacticConversion() {
        //                     ra   f        dec           long          lat
        _doFK5ToGalacticTest("17:45:37.20", "-28:56:10.2", "00:00:00.02", "00:00:00.00");
        _doFK5ToGalacticTest("9:55:41.6", "69:00:27.99", "142:08:40.11", "40:56:36.42");
        _doFK5ToGalacticTest("0:00:00", "00:00:00", "96:20:14.17", "-60:11:18.79");
        _doFK5ToGalacticTest("15:00:00", "40:00:00", "67:04:06.94", "60:30:13.13");
        _doFK5ToGalacticTest("20:00:00", "-40:00:00", "00:16:38.80", "-29:36:20.79");
    }

    private void _doTestOne(String lIn, String bIn,
                            String lEx, String bEx) {
        _t1.setC1(lIn);
        _t1.setC2(bIn);
        String lOut = _t1.c1ToString();
        String bOut = _t1.c2ToString();

        assertEquals("Failed comparison,", lEx, lOut);
        assertEquals("Failed comparison,", bEx, bOut);
    }

    @Test
    public void testWithStrings() {
        _doTestOne("0:0:0", "-0:0:0", "00:00:00.00", "00:00:00.00");
        _doTestOne("120:13:14.5", "32:33:34.0", "120:13:14.50", "32:33:34.00");
        _doTestOne("22:13:0", "-2:33:34.0", "22:13:00.00", "-02:33:34.00");
        _doTestOne("220:12:0", "-2:33:34.0", "220:12:00.00", "-02:33:34.00");
    }

    @Test
    public void testSerialization() throws Exception {
        final DegDegTarget outObject = new DegDegTarget();
        outObject.setC1C2("210:11:12.4", "-11:12:13.4");
        final DegDegTarget inObject = ser(outObject);
        assertTrue(outObject.equals(inObject));
    }
}
