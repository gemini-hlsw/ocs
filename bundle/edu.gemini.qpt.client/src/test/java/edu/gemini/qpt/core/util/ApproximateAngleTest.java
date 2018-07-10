package edu.gemini.qpt.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlFactory;

public class ApproximateAngleTest {

    @Test
    public void testPio() {
        PioFactory fact = new PioXmlFactory();
        ApproximateAngle a = new ApproximateAngle(180, 25);
        ParamSet set = a.getParamSet(fact, "test");
        ApproximateAngle b = new ApproximateAngle(set);
        assertEquals(a, b);
    }

    @Test
    public void testContains() {
        final int angle = 350;
        final int range = 50;
        ApproximateAngle a = new ApproximateAngle(angle, range);
        assertTrue(a.contains(angle - range));
        assertTrue(a.contains(angle - range - 360));
        assertTrue(a.contains(angle + range));
        assertTrue(a.contains(angle + range - 360));
        assertFalse(a.contains(angle - range - 1));
        assertFalse(a.contains(angle + range + 1));
    }

    @Test
    public void testEquals() {
        final int angle = 350;
        final int range = 50;
        ApproximateAngle a = new ApproximateAngle(angle, range);
        ApproximateAngle b = new ApproximateAngle(angle + 360, range);
        assertEquals(a, b);
        ApproximateAngle c = new ApproximateAngle(angle + 1, range);
        assertFalse(a.equals(c));
    }
    
    @Test
    public void testHashcode() {
        final int angle = 350;
        final int range = 50;
        ApproximateAngle a = new ApproximateAngle(angle, range);
        ApproximateAngle b = new ApproximateAngle(angle + 360, range);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.hashCode() == b.hashCode());
    }
    
}
