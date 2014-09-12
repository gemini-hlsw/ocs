//
// $
//

package edu.gemini.skycalc;

/**
 * Offset coordinates expressed as angular separations between two points, a
 * base position and a second position.
 */
public final class Offset {
    private static final Angle ZERO_ARCSECS = new Angle(0, Angle.Unit.ARCSECS);

    /**
     * An Offset of (0, 0) arcsecs.
     */
    public static final Offset ZERO_OFFSET = new Offset(ZERO_ARCSECS, ZERO_ARCSECS);

    private final Angle p;
    private final Angle q;

    public Offset(Angle p, Angle q) {
        if (p == null) throw new NullPointerException("p is null");
        if (q == null) throw new NullPointerException("q is null");

        this.p = p;
        this.q = q;
    }

    public Angle p() {
        return p;
    }

    public Angle q() {
        return q;
    }

    /**
     * Calculates the distance between the base position and the offset
     * position, expressed in the units of p.
     *
     * @return angular separation between base position and offset position
     */
    public Angle distance() {
        return distance(Offset.ZERO_OFFSET);
    }

    /**
     * Calculates the absolute distance between this Offset position and the
     * given position, expressed in the units of this offset's p.
     *
     * @param other distance computed is relative to the given offset
     *
     * @return distance, expressed in the units of this offset's p, between
     * this offset and the given <code>other</code> offset
     */
    public Angle distance(Offset other) {
        Angle.Unit unit = p.getUnit();

        double thisP = p.getMagnitude();
        double thisQ = q.convertTo(unit).getMagnitude();

        double thatP = other.p().convertTo(unit).getMagnitude();
        double thatQ = other.q().convertTo(unit).getMagnitude();

        double p = thisP - thatP;
        double q = thisQ - thatQ;

        double d = Math.sqrt(p*p + q*q);
        return new Angle(d, unit);
    }

    public String toString() {
        return String.format("%f x %f", p.getMagnitude(), q.getMagnitude());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Offset that = (Offset) o;

        if (!p.equals(that.p)) return false;
        return q.equals(that.q);
    }

    @Override
    public int hashCode() {
        int result = p.hashCode();
        result = 31 * result + q.hashCode();
        return result;
    }
}
