package edu.gemini.catalog.api;

import edu.gemini.catalog.api.MagnitudeLimits.FaintnessLimit;
import edu.gemini.catalog.api.MagnitudeLimits.SaturationLimit;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Coordinates;
import org.junit.Test;

import static edu.gemini.shared.skyobject.Magnitude.Band.R;
import static edu.gemini.skycalc.Angle.Unit.ARCSECS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Deprecated
public final class QueryConstraintTest {

    private static final Coordinates base = new Coordinates(0, 0);

    private static final Angle a0    = new Angle( 0.0,     ARCSECS);
    private static final Angle a2    = new Angle( 2.0,     ARCSECS);
    private static final Angle a4_9  = new Angle( 4.99999, ARCSECS);
    private static final Angle a5    = new Angle( 5.0,     ARCSECS);
    private static final Angle a7    = new Angle( 7.0,     ARCSECS);
    private static final Angle a10   = new Angle(10.0,     ARCSECS);
    private static final Angle a11   = new Angle(11.0,     ARCSECS);
    private static final Angle a20   = new Angle(20.0,     ARCSECS);

    private static final Angle ma2   = Angle.ANGLE_0DEGREES.add( -2,       ARCSECS);
    private static final Angle ma4_9 = Angle.ANGLE_0DEGREES.add( -4.99999, ARCSECS);
    private static final Angle ma7   = Angle.ANGLE_0DEGREES.add( -7.0,     ARCSECS);
    private static final Angle ma10  = Angle.ANGLE_0DEGREES.add(-10.0,     ARCSECS);
    private static final Angle ma11  = Angle.ANGLE_0DEGREES.add(-11.0,     ARCSECS);
    private static final Angle ma20  = Angle.ANGLE_0DEGREES.add(-20.0,     ARCSECS);

    private static final RadiusLimits rad5  = new RadiusLimits(a5);
    private static final RadiusLimits rad10 = new RadiusLimits(a10);

    private static final FaintnessLimit  faint10     = new FaintnessLimit(10);
    private static final SaturationLimit saturation0 = new SaturationLimit(0);
    private static final MagnitudeLimits mag10 = new MagnitudeLimits(R, faint10, saturation0);

    private static final QueryConstraint par10_10 = new QueryConstraint(base, rad10, mag10);

    @Test public void testSame() {
        assertTrue(par10_10.isSupersetOf(par10_10.copy(new Coordinates(0, 0))));
    }

    private void assertSuper(QueryConstraint superParams, QueryConstraint subParams) {
        assertTrue(superParams.isSupersetOf(subParams));
        assertFalse(subParams.isSupersetOf(superParams));
    }

    private void assertNotSuper(QueryConstraint p1, QueryConstraint p2) {
        assertFalse(p1.isSupersetOf(p2));
        assertFalse(p2.isSupersetOf(p1));
    }

    @Test public void testSameBase() {
        assertSuper(par10_10, par10_10.copy(rad5));
    }

    private void testSuperRange(Coordinates c) {
        assertSuper(par10_10, par10_10.copy(rad5).copy(c));
    }

    private void testNotSuperRange(Coordinates c) {
        assertNotSuper(par10_10, par10_10.copy(rad5).copy(c));
    }

    @Test public void testWithinRangeLimits() {
        ImList<Coordinates> coords = DefaultImList.create(
            new Coordinates(a2,    a0),
            new Coordinates(a4_9,  a0),
            new Coordinates(ma2,   a0),
            new Coordinates(ma4_9, a0),

            new Coordinates(a0,    a2),
            new Coordinates(a0,    a4_9),
            new Coordinates(a0,    ma2),
            new Coordinates(a0,    ma4_9)
        );
        coords.foreach(new ApplyOp<Coordinates>() {
            @Override
            public void apply(Coordinates coordinates) {
                testSuperRange(coordinates);
            }
        });
    }

    @Test public void testOutOfRangeLimits() {
        ImList<Coordinates> coords = DefaultImList.create(
            new Coordinates(a7,    a0),
            new Coordinates(ma7,   a0),
            new Coordinates(a10,   a0),
            new Coordinates(ma10,  a0),
            new Coordinates(a11,   a0),
            new Coordinates(ma11,  a0),
            new Coordinates(a20,   a0),
            new Coordinates(ma20,  a0),

            new Coordinates(a0,    a7),
            new Coordinates(a0,    ma7),
            new Coordinates(a0,    a10),
            new Coordinates(a0,    ma10),
            new Coordinates(a0,    a11),
            new Coordinates(a0,    ma11),
            new Coordinates(a0,    a20),
            new Coordinates(a0,    ma20)
        );
        coords.foreach(new ApplyOp<Coordinates>() {
            @Override
            public void apply(Coordinates coordinates) {
                testNotSuperRange(coordinates);
            }
        });
    }

    @Test public void testPoleWithinRangeLimits() {
        Coordinates pole  = new Coordinates(0.0, 90.0);
        Coordinates close = new Coordinates(pole.getRa(), pole.getDec().add(-2, ARCSECS));

        // 10 arcsec radius at the pole
        QueryConstraint poleP  = par10_10.copy(pole);

        // 5 arcsec radius close to the pole
        QueryConstraint closeP = par10_10.copy(close).copy(rad5);

        assertSuper(poleP, closeP);
    }

    @Test public void testPoleOutOfRangeLimits() {
        Coordinates pole = new Coordinates(0.0, 90.0);
        Coordinates far  = new Coordinates(pole.getRa(), pole.getDec().add(-7, ARCSECS));

        // 10 arcsec radius at the pole
        QueryConstraint poleP = par10_10.copy(pole);

        // 5 arcsec radius, but a bit too far from the pole
        QueryConstraint farP  = par10_10.copy(far).copy(rad5);

        assertNotSuper(poleP, farP);
    }

    @Test public void testWayOutOfRange() {
        QueryConstraint far = par10_10.copy(rad5).copy(new Coordinates(180.0, 0.0));
        assertNotSuper(par10_10, far);
    }

    @Test public void testFaintnessLimits() {
        QueryConstraint brighter = par10_10.copy(mag10.copy(new FaintnessLimit(9.9)));
        assertSuper(par10_10, brighter);

        QueryConstraint dimmer = par10_10.copy(mag10.copy(new FaintnessLimit(10.1)));
        assertSuper(dimmer, par10_10);
    }

    @Test public void testSaturationLimits() {
        QueryConstraint brighter = par10_10.copy(mag10.copy(new SaturationLimit(-0.1)));
        assertSuper(brighter, par10_10);

        QueryConstraint dimmer = par10_10.copy(mag10.copy(new SaturationLimit(0.1)));
        assertSuper(par10_10, dimmer);
    }

    @Test public void testMissingSaturationLimits() {
        QueryConstraint noSat = par10_10.copy(mag10.copy(None.<SaturationLimit>instance()));
        assertSuper(noSat, par10_10);
    }

    @Test public void testOutOfMagLimits() {
        // Not the same magnitude band.
        QueryConstraint J = par10_10.copy(mag10.copy(Magnitude.Band.J));
        assertNotSuper(par10_10, J);

    }
}
