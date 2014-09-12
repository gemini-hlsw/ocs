//
// $
//

package edu.gemini.skycalc;

import junit.framework.TestCase;
import static edu.gemini.skycalc.Angle.Unit.*;

/**
 * Test cases for {@link Offset}.
 */
public class OffsetTest extends TestCase {

    public void testDistanceFromBase() {
        Angle p = new Angle(3.0, ARCSECS);
        Angle q = new Angle(4.0, ARCSECS);

        Offset o = new Offset(p, q);

        Angle d = o.distance();
        assertEquals(ARCSECS, d.getUnit());
        assertEquals(5.0, d.getMagnitude());
    }

    public void testDistanceFromBaseMixedUnits() {
        Angle p = new Angle(3.0, ARCMINS).convertTo(ARCSECS);
        Angle q = new Angle(4.0, ARCMINS);

        Offset o = new Offset(p, q);

        Angle d = o.distance();
        assertEquals(ARCSECS, d.getUnit());
        assertEquals(5.0 * 60, d.getMagnitude());

        p = new Angle(3.0, ARCMINS);
        q = new Angle(4.0, ARCMINS).convertTo(ARCSECS);

        o = new Offset(p, q);

        d = o.distance();
        assertEquals(ARCMINS, d.getUnit());
        assertEquals(5.0, d.getMagnitude());
    }

    public void testRelativeDistance() {
        Angle  p1 = new Angle(6.0, ARCSECS);
        Angle  q1 = new Angle(8.0, ARCSECS);
        Offset o1 = new Offset(p1, q1);

        Angle  p2 = new Angle(3.0, ARCSECS);
        Angle  q2 = new Angle(4.0, ARCSECS);
        Offset o2 = new Offset(p2, q2);

        Angle d1 = o1.distance(o2);
        Angle d2 = o2.distance(o1);
        Angle expected = new Angle(5.0, ARCSECS);

        assertEquals(expected, d1);
        assertEquals(expected, d2);
    }
}
