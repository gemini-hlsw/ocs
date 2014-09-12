package edu.gemini.skycalc;

import java.io.Serializable;

/**
 * An angle and its measurement unit.
 */
public final class Angle implements Serializable {

    public static final Angle ANGLE_0DEGREES   = new Angle(0, Unit.DEGREES);

    public static final Angle ANGLE_2PI        = new Angle(0, Unit.RADIANS);
    public static final Angle ANGLE_PI_OVER_2  = new Angle(Math.PI/2, Unit.RADIANS);
    public static final Angle ANGLE_PI         = new Angle(Math.PI, Unit.RADIANS);
    public static final Angle ANGLE_3PI_OVER_2 = new Angle(3*Math.PI/2, Unit.RADIANS);

    public static Angle milliarcsecs(double d) { return new Angle(d, Unit.MILLIARCSECS); }
    public static Angle arcsecs(double d)      { return new Angle(d, Unit.ARCSECS); }
    public static Angle arcmins(double d)      { return new Angle(d, Unit.ARCMINS); }
    public static Angle degrees(double d)      { return new Angle(d, Unit.DEGREES); }
    public static Angle seconds(double d)      { return new Angle(d, Unit.SECONDS); }
    public static Angle minutes(double d)      { return new Angle(d, Unit.MINUTES); }
    public static Angle hours(double d)        { return new Angle(d, Unit.HOURS);   }
    public static Angle radians(double d)      { return new Angle(d, Unit.RADIANS); }

    /**
     * Angle unit options.
     */
    public enum Unit {
        MILLIARCSECS(360 * 60 * 60 * 1000, "mas"),
        ARCSECS(360 * 60 * 60, "arcsec"),
        ARCMINS(360 * 60, "arcmin"),
        DEGREES(360, "deg"),
        SECONDS(24 * 60 * 60, "sec"),
        MINUTES(24 * 60, "min"),
        HOURS(24,"hour"),
        RADIANS(2 * Math.PI, "rad");

        private final double circle;
        private final String abbreviation;

        Unit(double circle, String abbreviation) {
            this.circle       = circle;
            this.abbreviation = abbreviation;
        }

        public double convert(double theta, Unit unit) {
            if (unit == this) return theta;
            return theta/circle * unit.circle;
        }

        public double toMilliarcsecs(double theta) {
            return convert(theta, MILLIARCSECS);
        }

        public double toArcsecs(double theta) {
            return convert(theta, ARCSECS);
        }

        public double toArcmins(double theta) {
            return convert(theta, ARCMINS);
        }

        public double toDegrees(double theta) {
            return convert(theta, DEGREES);
        }

        public double toSeconds(double theta) {
            return convert(theta, SECONDS);
        }

        public double toMinutes(double theta) {
            return convert(theta, MINUTES);
        }

        public double toHours(double theta) {
            return convert(theta, HOURS);
        }

        public double toRadians(double theta) {
            return convert(theta, RADIANS);
        }

        public String toString() {
            return abbreviation;
        }
    }

    private final double theta;
    private final Unit unit;

    /**
     * Constructs and angle with a magnitude and its {@link Unit}s.  The
     * magnitude is normalized so that it falls within the range of
     * <code>min < magnitude < max</code> where min and max depend upon the
     * units.  For example, if the units are degrees, min is -360 and max is
     * 360.  So
     *
     * <pre>
     *    (new Angle(370, DEGREES)).getMagnitude() == 10.0
     * </pre>
     *
     * and
     *
     * <pre>
     *    (new Angle(360, DEGREES)).getMagnitude() == 0.0
     * </pre>
     *
     * while
     *
     * <pre>
     *    (new Angle(-370, DEGREES)).getMagnitude() == -10.0
     * </pre>
     *
     * @param magnitude magnitude of the angle relative to the provided units;
     * may not be infinite or NaN
     * @param unit units by which to interpret the angle; may not be
     * <code>null</code>
     */
    public Angle(double magnitude, Unit unit) {
        if (unit == null) throw new NullPointerException("missing units");
        if (Double.isInfinite(magnitude)) throw new IllegalArgumentException("infinite magnitude");
        if (Double.isNaN(magnitude)) throw new IllegalArgumentException("infinite NaN");

        // Normalize the angle.  Don't want multiples of a circle.
        if (Math.abs(magnitude) >= unit.circle) {
            magnitude = Math.IEEEremainder(magnitude, unit.circle);
        }

        this.theta = magnitude;
        this.unit  = unit;
    }

    /**
     * Gets the magnitude of the angle relative to its units.  Angles can be
     * specified with negative magnitudes, in which the result will be negative.
     * Call {@link #toPositive()}.getMagnitude() to get the equivalent angle
     * as a positive number.
     *
     * <pre>
     *    (new Angle(-10, DEGREES)).toPositive().getMagitude() == 350.0
     * </pre>
     */
    public double getMagnitude() {
        return theta;
    }

    /**
     * Gets the units by which to interpret the angle.
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * Returns an equivalent angle but converted to the given
     * units.  If the <code>unit</code> argument is the same as
     * <code>this</code> object's units, then <code>this</code> is returned.
     *
     * @param unit units to which the angle should be converted
     *
     * @return a new Angle that is equivalent to this angle, but expressed
     * in the given units; <code>this</code> if <code>unit</code> is the
     * same as <code>this</code> angle's units
     */
    public Angle convertTo(Unit unit) {
        if (this.unit == unit) return this;
        return new Angle(this.unit.convert(theta, unit), unit);
    }

    /**
     * Returns an equivalent angle, but converted to {@link Unit#MILLIARCSECS}.
     * If <code>this</code> angle is already expressed in milliarcseconds, then
     * <code>this</code> is returned.
     *
     * <p>This method is equivalent to
     * <code>{@link #convertTo convertTo(Unit.MILLIARCSECS)}</code>
     *
     * @return a new Angle that is equivalent to this angle, but expressed
     * in {@link Unit#MILLIARCSECS}; <code>this</code> if already expressed in
     * milliarcseconds
     */
    public Angle toMilliarcsecs() { return convertTo(Unit.MILLIARCSECS); }

    /**
     * Returns an equivalent angle, but converted to {@link Unit#ARCSECS}.  If
     * <code>this</code> angle is already expressed in arcseconds, then
     * <code>this</code> is returned.
     *
     * <p>This method is equivalent to
     * <code>{@link #convertTo convertTo(Unit.ARCSECS)}</code>
     *
     * @return a new Angle that is equivalent to this angle, but expressed
     * in {@link Unit#ARCSECS}; <code>this</code> if already expressed in
     * arcseconds
     */
    public Angle toArcsecs() { return convertTo(Unit.ARCSECS); }

    /**
     * Returns an equivalent angle, but converted to {@link Unit#ARCMINS}.  If
     * <code>this</code> angle is already expressed in arcmins, then
     * <code>this</code> is returned.
     *
     * <p>This method is equivalent to
     * <code>{@link #convertTo convertTo(Unit.ARCMINS)}</code>
     *
     * @return a new Angle that is equivalent to this angle, but expressed
     * in {@link Unit#ARCMINS}; <code>this</code> if already expressed in
     * arcmins
     */
    public Angle toArcmins() { return convertTo(Unit.ARCMINS); }

    /**
     * Returns an equivalent angle, but converted to {@link Unit#DEGREES}.  If
     * <code>this</code> angle is already expressed in degrees, then
     * <code>this</code> is returned.
     *
     * <p>This method is equivalent to
     * <code>{@link #convertTo convertTo(Unit.DEGREES)}</code>
     *
     * @return a new Angle that is equivalent to this angle, but expressed
     * in {@link Unit#DEGREES}; <code>this</code> if already expressed in
     * degrees
     */
    public Angle toDegrees() { return convertTo(Unit.DEGREES); }

    /**
     * Returns an equivalent angle, but converted to {@link Unit#SECONDS} of
     * time.  If <code>this</code> angle is already expressed in seconds, then
     * <code>this</code> is returned.
     *
     * <p>This method is equivalent to
     * <code>{@link #convertTo convertTo(Unit.SECONDS)}</code>
     *
     * @return a new Angle that is equivalent to this angle, but expressed
     * in {@link Unit#SECONDS}; <code>this</code> if already expressed in
     * seconds
     */
    public Angle toSeconds() { return convertTo(Unit.SECONDS); }

    /**
     * Returns an equivalent angle, but converted to {@link Unit#MINUTES} of
     * time.  If <code>this</code> angle is already expressed in minutes, then
     * <code>this</code> is returned.
     *
     * <p>This method is equivalent to
     * <code>{@link #convertTo convertTo(Unit.MINUTES)}</code>
     *
     * @return a new Angle that is equivalent to this angle, but expressed
     * in {@link Unit#MINUTES}; <code>this</code> if already expressed in
     * minutes
     */
    public Angle toMinutes() { return convertTo(Unit.MINUTES); }

    /**
     * Returns an equivalent angle, but converted to {@link Unit#HOURS}.  If
     * <code>this</code> angle is already expressed in hours, then
     * <code>this</code> is returned.
     *
     * <p>This method is equivalent to
     * <code>{@link #convertTo convertTo(Unit.HOURS)}</code>
     *
     * @return a new Angle that is equivalent to this angle, but expressed
     * in {@link Unit#HOURS}; <code>this</code> if already expressed in
     * hours
     */
    public Angle toHours()   { return convertTo(Unit.HOURS); }

    /**
     * Returns an equivalent angle, but converted to {@link Unit#RADIANS}.  If
     * <code>this</code> angle is already expressed in radians, then
     * <code>this</code> is returned.
     *
     * <p>This method is equivalent to
     * <code>{@link #convertTo convertTo(Unit.RADIANS)}</code>
     *
     * @return a new Angle that is equivalent to this angle, but expressed
     * in {@link Unit#RADIANS}; <code>this</code> if already expressed in
     * radians
     */
    public Angle toRadians() { return convertTo(Unit.RADIANS); }

    /**
     * Returns an equivalent angle, but converted to a positive magnitude.
     * If <code>this</code> angle is already expressed in a positive
     * magnitude, then <code>this</code> is returned.
     *
     * <p>For example,
     * <code>(new Angle(-10, Unit.DEGREES)).toPositive().getMagnitude() == 350.0</code>
     *
     * <p>whereas given <code>Angle a10 = new Angle(10, Unit.DEGREES);</code>,
     * then <code>a10.toPositive() == a10</code>.
     *
     * @return a new Angle that is equivalent to this angle, but expressed
     * as a positive angle; <code>this</code> if already expressed as a
     * positive angle
     */
    public Angle toPositive() {
        if (theta >= 0) return this;
        return new Angle(unit.circle + theta, unit);
    }

    /**
     * Returns an equivalent angle, but converted to a negative magnitude.
     * If <code>this</code> angle is already expressed as a negative
     * magnitude, then <code>this</code> is returned.
     *
     * <p>For example,
     * <code>(new Angle(10, Unit.DEGREES)).toNegative().getMagnitude() == -350.0</code>
     *
     * <p>whereas given <code>Angle minus10 = new Angle(-10, Unit.DEGREES);</code>,
     * then <code>minus10.toNegative() == minus10</code>.
     *
     * @return a new Angle that is equivalent to this angle, but expressed
     * as a negative angle; <code>this</code> if already expressed as a
     * negative angle
     */
    public Angle toNegative() {
        if (theta <= 0) return this;
        return new Angle(theta - unit.circle, unit);
    }

    // Converts to radians, in order to compute the trigonometic functions.
    private double radians() {
        return unit.convert(theta, Unit.RADIANS);
    }

    /**
     * Computes the trigometric sine of the angle.
     */
    public double sin() {
        return Math.sin(radians());
    }

    /**
     * Computes the trigometric cosine of the angle.
     */
    public double cos() {
        return Math.cos(radians());
    }

    /**
     * Computes the trigometric tangent of the angle.
     */
    public double tan() {
        return Math.tan(radians());
    }

    /**
     * Computes the arc sine of the angle.
     */
    public double asin() {
        return Math.asin(radians());
    }

    /**
     * Computes the arc cosine of the angle.
     */
    public double acos() {
        return Math.acos(radians());
    }

    /**
     * Computes the arc tangent of the angle.
     */
    public double atan() {
        return Math.atan(radians());
    }

    // Adjust the angle "that" to be expressed in the same units as this
    // angle, and as a positive angle if this angle is positive (or negative
    // if this angle is negative).  This makes comparing the two angles or
    // adding them together possible.
    private Angle adjust(Angle that) {
        that = that.convertTo(unit);
        return (theta < 0) ? that.toNegative() : that.toPositive();
    }

    /**
     * Adds the given angle to this angle and returns a new angle that
     * contains the result expressed in this angle's units.
     *
     * @param that angle to add to this angle
     *
     * @return new angle that represents the sum of <code>this</code> angle and
     * <code>that</code> angle expressed in <code>this</code> angle's units
     */
    public Angle add(Angle that) {
        that = adjust(that);
        return new Angle(theta + that.theta, unit);
    }

    /**
     * Adds the given amount to this angle and returns a new angle that
     * contains the result expressed in this angle's units.  This method is
     * equivalent to
     * <code>{@link #add(Angle) add(new Angle(theta, unit))</code>.
     *
     * @param theta angle to add to this angle, expressed in terms of the given
     * units
     * @param units in which to interpret the angle <code>theta</code>
     *
     * @return new angle that represents the sum of <code>this</code> angle and
     * the given angle <code>theta</code> expressed in <code>this</code> angle's
     * units
     */
    public Angle add(double theta, Unit unit) {
        return add(new Angle(theta, unit));
    }

    /**
     * Compares two angles taking into account the units in which they are
     * expressed.  For example, 90 degrees is less than PI radians but more
     * than 100 arcsec.  Note, this is not an implementation of the Java
     * <code>Comparable</code> interface because it is not consistent with
     * <code>equals</code>.  Two angles that are equivalent, but expressed
     * in different units would not be considered <code>equals</code> but
     * would be compared as equivalent by this method.
     *
     * @param that the angle to compare to <code>this</code> angle
     *
     * @return return -1 if <code>this</code> angle is less than
     * <code>that</code> angle, 0 if they represent the same angle, and 1 if
     * <code>that</code> is bigger than <code>this</code>
     */
    public int compareToAngle(Angle that) {
        return Double.compare(theta, adjust(that).theta);
    }

    /**
     * Computes equality based upon the magnitude of the angle and its units.
     * Two angles that are equivalent but expressed in different units would
     * not be considered equals by this method.  That test can be performed
     * by <code>{@link #compareToAngle(Angle)} compareToAngle(that) == 0}</code>.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Angle angle = (Angle) o;

        if (Double.compare(angle.theta, theta) != 0) return false;
        return unit == angle.unit;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = theta != +0.0d ? Double.doubleToLongBits(theta) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + (unit != null ? unit.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%f %s", theta, unit.abbreviation);
    }
}