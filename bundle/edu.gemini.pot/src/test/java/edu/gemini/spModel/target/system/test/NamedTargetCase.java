package edu.gemini.spModel.target.system.test;

import edu.gemini.spModel.target.system.*;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Class NamedTargetCase tests the NamedTarget coordinate class.
 */
public final class NamedTargetCase {
    NamedTarget _t1;
    NamedTarget _t2;

    @Before
    public void setUp() throws Exception {
        _t1 = new NamedTarget();
        _t2 = new NamedTarget();
    }

    @Test
    public void testSimple() {
        NamedTarget t1 = new NamedTarget();
        assertNotNull(t1);

        // Alt/Az
        HMS c1 = new HMS("16:11:12.345");
        DMS c2 = new DMS("-20:30:40.567");
        t1.setC1C2(c1, c2);

        assertEquals("16:11:12.345", t1.getRaHms());
        assertEquals("-20:30:40.57", t1.getDecDms());

        //Planet
        t1.setSolarObject(NamedTarget.SolarObject.NEPTUNE);
        assertEquals(NamedTarget.SolarObject.NEPTUNE, t1.getSolarObject());
    }

    private void _doTestOne(String lIn, String bIn,
                            String lEx, String bEx) {
        _t1.setRaHms(lIn);
        _t1.setDecDms(bIn);
        String lOut = _t1.getRaHms();
        String bOut = _t1.getDecDms();

        assertEquals("Failed comparison,", lEx, lOut);
        assertEquals("Failed comparison,", bEx, bOut);
    }

    @Test
    public void testWithStrings() {
        _doTestOne("0:0:0", "-0:0:0", "00:00:00.000", "00:00:00.00");
        _doTestOne("12:13:14.5", "32:33:34.0", "12:13:14.500", "32:33:34.00");
        _doTestOne("22:13:0", "-2:33:34.0", "22:13:00.000", "-02:33:34.00");
    }

    @Test
    public void testSerialization() throws Exception {
        final NamedTarget outObject = new NamedTarget();
        outObject.setC1C2("210:11:12.4", "-11:12:13.4");
        outObject.setSolarObject(NamedTarget.SolarObject.URANUS);
        final NamedTarget inObject = ser(outObject);
        assertTrue(outObject.equals(inObject));
    }
}
