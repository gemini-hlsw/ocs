package edu.gemini.catalog.api;

import edu.gemini.skycalc.Angle;
import static edu.gemini.skycalc.Angle.Unit.ARCMINS;
import edu.gemini.skycalc.CoordinateDiff;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.util.immutable.PredicateOp;

/**
 * Describes limits for catalog cone search radius values.
 * See OT-17.
 */
@Deprecated
public final class RadiusLimits {
    public static final RadiusLimits EMPTY = new RadiusLimits(Angle.ANGLE_0DEGREES.toArcmins(), Angle.ANGLE_0DEGREES.toArcmins());

    private final Angle maxLimit;
    private final Angle minLimit;

    public RadiusLimits(Angle maxLimit, Angle minLimit) {
        this.maxLimit = maxLimit;
        this.minLimit = minLimit;
    }

    public Angle getMaxLimit() {
        return maxLimit;
    }

    public Angle getMinLimit() {
        return minLimit;
    }

    /**
     * If there is an offset but there isn't a posAngle, then we have to adjust the
     * search radius to take into account any position angle. That means the
     * outer limit increases by the distance from the base to the offset and the
     * inner limit decreases by the same distance (never less than 0 though).
     *
     * @return a new (possibly ) adjusted radius limits
     */
    public RadiusLimits adjust(Offset offset) {
        Angle d = offset.distance();
        Angle max = maxLimit.add(d);
        Angle min = new Angle(Math.max(minLimit.toArcmins().getMagnitude() - d.toArcmins().getMagnitude(), 0.), ARCMINS);
        return new RadiusLimits(max, min);
    }

    public PredicateOp<SkyObject> skyObjectFilter(final Coordinates base) {
        return skyObject -> {
            HmsDegCoordinates coords = skyObject.getCoordinates().toHmsDeg(0);
            Angle ra  = coords.getRa();
            Angle dec = coords.getDec();
            Coordinates c = new Coordinates(ra, dec);
            CoordinateDiff cd = new CoordinateDiff(base, c);

            Angle distance = cd.getDistance();
            return (minLimit.compareToAngle(distance) <= 0) &&
                   (maxLimit.compareToAngle(distance) >= 0);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final RadiusLimits that = (RadiusLimits) o;
        if (!maxLimit.equals(that.maxLimit)) return false;
        return minLimit.equals(that.minLimit);
    }

    @Override
    public int hashCode() {
        int result = maxLimit.hashCode();
        result = 31 * result + minLimit.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RadiusLimits{" +
                "maxLimit=" + maxLimit +
                ", minLimit=" + minLimit +
                '}';
    }

}
