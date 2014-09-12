//
// $
//

package edu.gemini.skycalc;

import junit.framework.TestCase;
import edu.gemini.skycalc.Angle.Unit;
import static edu.gemini.skycalc.Angle.Unit.*;

import org.junit.Test;

/**
 * Test cases for the {@link Angle} class.
 */
public final class AngleTest extends TestCase {

    @Test
    public void testConstruction() throws Exception {
        // Construct without units.
        try {
            new Angle(0, null);
            fail("expected NullPointerException");
        } catch (NullPointerException ex) {
            // okay
        }

        // Normal construction
        Angle a = new Angle(180.0, DEGREES);
        assertEquals(180.0, a.getMagnitude());
        assertEquals(Unit.DEGREES, a.getUnit());

        // Using a multiple of a circle.
        a = new Angle(361.0, DEGREES);
        assertEquals(1.0, a.getMagnitude());

        a = new Angle(360.5, DEGREES);
        assertEquals(0.5, a.getMagnitude());

        a = new Angle(-360.5, DEGREES);
        assertEquals(-0.5, a.getMagnitude());

        a = new Angle(25.0, HOURS);
        assertEquals(1.0, a.getMagnitude());
    }

    @Test
    public void testPositiveAngle() {
        Angle a = new Angle(-1.0, HOURS);
        Angle b = a.toPositive();
        assertEquals(23.0, b.getMagnitude());
        assertNotSame(a, b);
        assertFalse(a.equals(b));

        assertSame(b, b.toPositive());
    }

    @Test
    public void testNegativeAngle() {
        Angle a = new Angle(1.0, HOURS);
        Angle b = a.toNegative();
        assertEquals(-23.0, b.getMagnitude());
        assertNotSame(a, b);
        assertFalse(a.equals(b));

        assertSame(b, b.toNegative());
    }

    public static Angle[] a180 = new Angle[Unit.values().length];

    static {
        a180[MILLIARCSECS.ordinal()] = new Angle(180 * 60 * 60 * 1000, MILLIARCSECS);
        a180[ARCSECS.ordinal()]      = new Angle(180 * 60 * 60, ARCSECS);
        a180[ARCMINS.ordinal()]      = new Angle(180 * 60, ARCMINS);
        a180[DEGREES.ordinal()]      = new Angle(180, DEGREES);
        a180[SECONDS.ordinal()]      = new Angle(12 * 60 * 60, SECONDS);
        a180[MINUTES.ordinal()]      = new Angle(12 * 60, MINUTES);
        a180[HOURS.ordinal()]        = new Angle(12, HOURS);
        a180[RADIANS.ordinal()]      = new Angle(Math.PI, RADIANS);
    }

    @Test
    public void testConversion() throws Exception {
        for (Unit u1 : Unit.values()) {
            Angle a = a180[u1.ordinal()];
            for (Unit u2 : Unit.values()) {
                Angle b = a.convertTo(u2);
                if (u1 == u2) {
                    assertSame(a, b);
                } else {
                    assertEquals(b, a180[u2.ordinal()]);
                }
            }
        }
    }

    // Normal comparisons among the same units.
    @Test
    public void testCompareAngle_Normal() throws Exception {
        Angle a0 = new Angle(0, DEGREES);
        Angle a1 = new Angle(1, DEGREES);

        assertEquals(-1, a0.compareToAngle(a1));
        assertEquals(1,  a1.compareToAngle(a0));
        assertEquals(0,  a0.compareToAngle(new Angle(0, DEGREES)));
    }

    // Mixed unit comparisons.
    @Test
    public void testCompareAngle_Mixed() throws Exception {
        Angle a59s = new Angle(59, ARCSECS);
        Angle a60s = new Angle(60, ARCSECS);
        Angle a61s = new Angle(61, ARCSECS);
        Angle a1m  = new Angle( 1, ARCMINS);

        assertEquals(-1, a59s.compareToAngle(a1m));
        assertEquals( 0, a60s.compareToAngle(a1m));
        assertEquals( 1, a61s.compareToAngle(a1m));

        assertEquals(-1, a1m.compareToAngle(a61s));
        assertEquals( 0, a1m.compareToAngle(a60s));
        assertEquals( 1, a1m.compareToAngle(a59s));
    }

    // +/- angle comparisons
    @Test
    public void testCompareAngle_Negative() throws Exception {
        Angle a_9  = new Angle(-9,  DEGREES);
        Angle a_10 = new Angle(-10, DEGREES);
        Angle a_11 = new Angle(-11, DEGREES);
        Angle a350 = new Angle(350, DEGREES);

        assertEquals( 1, a_9.compareToAngle(a_10));
        assertEquals( 0, a_10.compareToAngle(a_10));
        assertEquals(-1, a_11.compareToAngle(a_10));
        assertEquals( 0, a350.compareToAngle(a_10));
        assertEquals( 0, a_10.compareToAngle(a350));
    }

    @Test
    public void testAdd() throws Exception {
        assertEquals(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_2PI.add(Angle.ANGLE_PI_OVER_2));
        assertEquals(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_2PI.add(Math.PI/2, RADIANS));
        assertEquals(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_2PI.add(-3*Math.PI/2, RADIANS));

        assertEquals(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_PI_OVER_2.add(Angle.ANGLE_2PI));
        assertEquals(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_PI_OVER_2.add(2*Math.PI, RADIANS));
        assertEquals(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_PI_OVER_2.add(-2*Math.PI, RADIANS));

        assertEquals(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_PI.add(Angle.ANGLE_3PI_OVER_2));
        assertEquals(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_PI.add(3*Math.PI/2, RADIANS));
        assertEquals(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_PI.add(-Math.PI/2, RADIANS));

        assertEquals(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_3PI_OVER_2.add(Angle.ANGLE_PI));
        assertEquals(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_3PI_OVER_2.add(Math.PI, RADIANS));
        assertEquals(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_3PI_OVER_2.add(-Math.PI, RADIANS));
    }

}
