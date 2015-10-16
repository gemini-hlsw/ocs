package edu.gemini.spModel.target.system.test;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.target.system.HmsDegTarget;
import static edu.gemini.spModel.test.TestFile.ser;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Class SPTargetTest tests classes related to SPTarget.
 */
public final class HmsDegTargetCase {
    HmsDegTarget _t1;
    HmsDegTarget _t2;

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

        t1.setRaString("10:11:12.345");
        t1.setDecString("-20:30:40.567");

        assertEquals(t1.getRaString(None.instance()).getOrNull(), "10:11:12.345");
        assertEquals(t1.getDecString(None.instance()).getOrNull(), "-20:30:40.57");
    }

    private void _doTestOne(String raIn, String decIn,
                            String raEx, String decEx) {
        _t1.setRaString(raIn);
        _t1.setDecString(decIn);
        String raOut = _t1.getRaString(None.instance()).getOrNull();
        String decOut = _t1.getDecString(None.instance()).getOrNull();

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
        outObject.setRaString("10:11:12.34");
        outObject.setDecString("-11:12:13.4");
        final HmsDegTarget inObject = ser(outObject);
        assertTrue(outObject.equals(inObject));
    }
}
