//
// $
//

package edu.gemini.shared.skyobject.coords;

import static edu.gemini.shared.skyobject.coords.HmsDegCoordinates.Epoch;
import static edu.gemini.shared.skyobject.coords.HmsDegCoordinates.Epoch.Type.*;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for {@link HmsDegCoordinates.Epoch}.
 */
public class EpochTest {

    @Test
    public void testConstruction() {
        Epoch j2010 = new Epoch(JULIAN, 2010.2);
        Epoch b1960 = new Epoch(BESSELIAN, 1960.5);

        assertEquals(JULIAN, j2010.getType());
        assertEquals(2010.2, j2010.getYear(), 0.000001);

        assertEquals(BESSELIAN, b1960.getType());
        assertEquals(1960.5, b1960.getYear(), 0.000001);

        // can't construct with null type
        try {
            new Epoch(null, 2000.0);
            fail("can't construct with null type");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testComparable() {
        Epoch j1999 = new Epoch(JULIAN, 1999.0);
        Epoch j2001 = new Epoch(JULIAN, 2001.0);

        // B anything sorts before J anything
        assertEquals(-1, Epoch.B1950.compareTo(Epoch.J2000));
        assertEquals(-1, new Epoch(BESSELIAN, 2000).compareTo(new Epoch(JULIAN, 1950)));

        // B and J are different, even if the year is the same.
        assertEquals(-1, Epoch.B1950.compareTo(new Epoch(JULIAN, 1950)));

        // If the type is the same, the year matters.
        assertEquals( 1, Epoch.J2000.compareTo(j1999));
        assertEquals(-1, Epoch.J2000.compareTo(j2001));
        assertEquals( 0, Epoch.J2000.compareTo(new Epoch(JULIAN, 2000.0)));
    }

    @Test
    public void testToString() {
        assertEquals("J2000.0", Epoch.J2000.toString());
        assertEquals("J2001.0", new Epoch(JULIAN, 2001.0).toString());
        assertEquals("J2000.5", new Epoch(JULIAN, 2000.5).toString());
    }
}
