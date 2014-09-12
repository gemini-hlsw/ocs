//
// $
//

package edu.gemini.skycalc;

import static edu.gemini.skycalc.Angle.Unit.ARCSECS;
import static edu.gemini.skycalc.Angle.Unit.DEGREES;

/**
 * A class that represents the difference between two sky coordinates, a base
 * position and a second position.  The difference includes the angular
 * separation between the two coordinates (roughly speaking, the "distance"
 * between them), the position angle formed in degrees east of north, and an
 * offset in p and q (angular separations).
 *
 * <p>(Based on the C version from A. P. Martinez)
 */
public final class CoordinateDiff {
    private final Angle posAngleDeg;
    private final Angle distanceArcsec;

    public CoordinateDiff(Coordinates base, Coordinates point) {
        this(base.getRaDeg(), base.getDecDeg(), point.getRaDeg(), point.getDecDeg());
    }

    public CoordinateDiff(Angle baseRa, Angle baseDec, Angle ra, Angle dec) {
        this(baseRa.toDegrees().getMagnitude(), baseDec.toDegrees().getMagnitude(),
             ra.toDegrees().getMagnitude(), dec.toDegrees().getMagnitude());
    }

    public CoordinateDiff(double baseRaDeg, double baseDecDeg, double raDeg, double decDeg) {
        double alf, alf0, del, del0, phi;
        double sd, sd0, cd, cd0, cosda, cosd, sind, sinpa, cospa;
        double radian = 180.0 / Math.PI;

        // coo transformed to radians
        alf  = raDeg      / radian;
        alf0 = baseRaDeg  / radian;
        del  = decDeg     / radian;
        del0 = baseDecDeg / radian;

        sd0 = Math.sin(del0);
        sd = Math.sin(del);
        cd0 = Math.cos(del0);
        cd = Math.cos(del);
        cosda = Math.cos(alf - alf0);
        cosd = sd0 * sd + cd0 * cd * cosda;
        double dist = Math.acos(cosd);
        if (Double.isNaN(dist)) dist = 0;
        phi = 0.0;
        if (dist > 0.0000004) {
            sind = Math.sin(dist);
            cospa = (sd * cd0 - cd * sd0 * cosda) / sind;
            if (Math.abs(cospa) > 1.0) {
                // 2005-06-02: fix from awicenec@eso.org
                cospa=cospa/Math.abs(cospa);
            }
            sinpa = cd * Math.sin(alf - alf0) / sind;
            phi = Math.acos(cospa);
            if (sinpa < 0.0) phi = (Math.PI*2) - phi;
        }
        dist *= radian;
        dist *= 60.0;

        posAngleDeg    = new Angle(phi * radian, DEGREES);
        distanceArcsec = new Angle(dist * 60.0, ARCSECS);
    }

    /**
     * Gets the difference between the two points expressed as an angular
     * separation.  Together with the {@link #getPositionAngle() position angle},
     * this can be used to calculate the {@link #getOffset offset} coordinates.
     *
     * @return Angle (in arcsec) representing the angular separation between
     * the two coordinates
     */
    public Angle getDistance() {
        return distanceArcsec;
    }

    /**
     * Gets the position angle, in degrees east of north, between the two
     * coordinates.  Together with the {@link #getDistance distance}, this can
     * be used to calculate the {@link #getOffset offset} coordinates.
     *
     * @return position angle in degrees east of north
     */
    public Angle getPositionAngle() {
        return posAngleDeg;
    }

    /**
     * Gets the offset coordinates relative to the base position.
     */
    public Offset getOffset() {
        Angle phi = posAngleDeg.toRadians();
        double  h = distanceArcsec.getMagnitude();

        // Position angle is east of north, or relative to 90 degrees.
        // Swapping sin and cos here to compensate.
        Angle p = new Angle(h * phi.sin(), ARCSECS);
        Angle q = new Angle(h * phi.cos(), ARCSECS);
        return new Offset(p, q);
    }

    public String toString() {
        return String.format("%f arcsec, %f deg E of N",
                distanceArcsec.getMagnitude(), posAngleDeg.getMagnitude());
    }
}
