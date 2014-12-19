//
// $
//

package edu.gemini.shared.skyobject.coords;


import edu.gemini.skycalc.Angle;
import static edu.gemini.shared.skyobject.coords.HmsDegCoordinates.Epoch;
import static org.junit.Assert.*;
import org.junit.Test;


/**
 * HmsDegCoordinates test cases.  There isn't much to test here, since this
 * is largely just a grouping of related information.  In other words, a very
 * data-heavy class.
 */
public class HmsDegCoordinatesTest {
    private static final Angle ra  = new Angle(0, Angle.Unit.DEGREES);
    private static final Angle dec = new Angle(1, Angle.Unit.DEGREES);

    @Test
    public void testBuilderDefaults() {
        HmsDegCoordinates.Builder b = new HmsDegCoordinates.Builder(ra, dec);
        HmsDegCoordinates c = b.build();

        assertEquals(ra,  c.getRa());
        assertEquals(dec, c.getDec());

        // defaults to J2000
        assertEquals(Epoch.J2000, c.getEpoch());

        // Defaults to no proper motion.
        Angle zero = new Angle(0, Angle.Unit.MILLIARCSECS);
        assertEquals(zero, c.getPmRa());
        assertEquals(zero, c.getPmDec());
    }

    @Test
    public void testBuilderNulls() {
        // Cannot construct with a null RA or dec.
        try {
            new HmsDegCoordinates.Builder(null, dec);
            fail();
        } catch (IllegalArgumentException ex) {
            // okay
        }
        try {
            new HmsDegCoordinates.Builder(ra, null);
            fail();
        } catch (IllegalArgumentException ex) {
            // okay
        }

        // cannot subsequently set a null RA or dec.
        HmsDegCoordinates.Builder b = new HmsDegCoordinates.Builder(ra, dec);

        try {
            b.ra(null);
            fail();
        } catch (IllegalArgumentException ex) {
            // okay
        }
        try {
            b.dec(null);
            fail();
        } catch (IllegalArgumentException ex) {
            // okay
        }

        // cannot set a null epoch
        try {
            b.epoch(null);
            fail();
        } catch (IllegalArgumentException ex) {
            // okay
        }

        // cannot set a null pm in RA
        try {
            b.pmRa(null);
            fail();
        } catch (IllegalArgumentException ex) {
            // okay
        }

        // cannot set a null pm in declination
        try {
            b.pmDec(null);
            fail();
        } catch (IllegalArgumentException ex) {
            // okay
        }
    }

    @Test
    public void testBuilderInitFromObject() {
        Angle pmRa  = new Angle(1, Angle.Unit.MILLIARCSECS);
        Angle pmDec = new Angle(2, Angle.Unit.MILLIARCSECS);

        HmsDegCoordinates.Builder b;
        b = new HmsDegCoordinates.Builder(ra, dec).epoch(Epoch.J2000).pmRa(pmRa).pmDec(pmDec);

        HmsDegCoordinates c1 = b.build();

        assertEquals(Epoch.J2000, c1.getEpoch());
        assertEquals(ra.getMagnitude(), c1.getRa().getMagnitude(), 0.000001);
        assertEquals(dec.getMagnitude(), c1.getDec().getMagnitude(), 0.000001);
        assertEquals(pmRa.getMagnitude(), c1.getPmRa().getMagnitude(), 0.000001);
        assertEquals(pmDec.getMagnitude(), c1.getPmDec().getMagnitude(), 0.000001);

        // Builder should be initialized with the current values.  So when
        // asked to build, it should build a new equivalent object (but not
        // the same object!)
        HmsDegCoordinates c2 = c1.builder().build();

        assertEquals(c1, c2);
        assertNotSame(c1, c2);
    }

    @Test
    public void testConvert() {
        HmsDegCoordinates.Builder b;
        b = new HmsDegCoordinates.Builder(ra, dec).epoch(Epoch.J2000);
        HmsDegCoordinates c = b.build();

        // nothing should be created for the conversion to HmsDegCoordinates
        // since this is already an HmsDegCoordinates instance
        assertSame(c, c.toHmsDeg(0));
    }

}
