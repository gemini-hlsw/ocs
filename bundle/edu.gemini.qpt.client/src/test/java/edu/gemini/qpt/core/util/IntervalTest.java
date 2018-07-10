package edu.gemini.qpt.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import edu.gemini.qpt.core.util.Interval.Overlap;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlFactory;

public class IntervalTest {

    @Test
    public void testPio() {

        PioFactory fact = new PioXmlFactory();
        Interval a = new Interval(10, 20);
        ParamSet params = a.getParamSet(fact, "test");
        Interval b = new Interval(params);
        assertEquals(a, b);

    }

    @Test
    public void testContains() {
        
        // Simple contains
        final int start = 10;
        final int end = 20;
        Interval a = new Interval(start, end);
        for (int i = start; i < end; i++)
            assertTrue(a.contains(i));
        assertFalse(a.contains(start - 1));
        assertFalse(a.contains(end));
                
    }

    @Test
    public void testOverlapsAndAbuts() {
        Interval a = new Interval(5, 10);
        
        // Abutting interval, low side
        final Interval lowAdjacent = new Interval(3, 5);
        assertTrue(a.abuts(lowAdjacent));
        assertTrue(a.overlaps(lowAdjacent, Overlap.NONE));
        assertFalse(a.overlaps(lowAdjacent, Overlap.EITHER));
        assertFalse(a.overlaps(lowAdjacent, Overlap.PARTIAL));
        assertFalse(a.overlaps(lowAdjacent, Overlap.TOTAL));

        // Abutting interval, high side
        final Interval hiAdjacent = new Interval(10, 15);
        assertTrue(a.abuts(hiAdjacent));
        assertTrue(a.overlaps(hiAdjacent, Overlap.NONE));
        assertFalse(a.overlaps(hiAdjacent, Overlap.EITHER));
        assertFalse(a.overlaps(hiAdjacent, Overlap.PARTIAL));
        assertFalse(a.overlaps(hiAdjacent, Overlap.TOTAL));

        // Partial overlap, low side
        final Interval lowOverlap = new Interval(3, 6);
        assertFalse(a.abuts(lowOverlap));
        assertFalse(a.overlaps(lowOverlap, Overlap.NONE));
        assertTrue(a.overlaps(lowOverlap, Overlap.EITHER));
        assertTrue(a.overlaps(lowOverlap, Overlap.PARTIAL));
        assertFalse(a.overlaps(lowOverlap, Overlap.TOTAL));

        // Partial overlap, high side
        final Interval hiOverlap = new Interval(9, 16);
        assertFalse(a.abuts(hiOverlap));
        assertFalse(a.overlaps(hiOverlap, Overlap.NONE));
        assertTrue(a.overlaps(hiOverlap, Overlap.EITHER));
        assertTrue(a.overlaps(hiOverlap, Overlap.PARTIAL));
        assertFalse(a.overlaps(hiOverlap, Overlap.TOTAL));

        // Partial overlap, high side
        final Interval totalOverlap = new Interval(7, 8);
        assertFalse(a.abuts(totalOverlap));
        assertFalse(a.overlaps(totalOverlap, Overlap.NONE));
        assertTrue(a.overlaps(totalOverlap, Overlap.EITHER));
        assertFalse(a.overlaps(totalOverlap, Overlap.PARTIAL));
        assertTrue(a.overlaps(totalOverlap, Overlap.TOTAL));
        
    }


    @Test
    @Ignore
    public void testPlus() {
        fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testMinus() {
        
        // Clip from end
        final int start = 10;
        final int end = 20;
        Interval a = new Interval(start, end);
        Interval b = a.minus(new Interval(15,25));
        assertEquals(b, new Interval(10, 15));
        
    }

    @Test
    @Ignore
    public void testCompareTo() {
        fail("Not yet implemented");
    }

    @Test
    @Ignore
    public void testEquals() {
        fail("Not yet implemented");
    }

    public void testLength() {
        fail("impl");
    }
    
}
