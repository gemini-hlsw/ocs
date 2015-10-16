package edu.gemini.spModel.target.system;

/**
 * A container class for localizing the various parameter types used by
 * the target coordinate systems.
 *
 * @author      Shane Walker
 * @author      Kim Gillies (modified for SP)
 */
public final class CoordinateTypes {
    // Don't allow instances of this class to be created.  It's really just
    // a scope for the specific coordinate params.
    private CoordinateTypes() { }

    /**
     * The PM1 (proper motion of c1) parameter type.
     */
    public static final class PM1 extends CoordinateParam {
        public static final Units[] UNITS = {
            Units.MILLI_ARCSECS_PER_YEAR,
            Units.ARCSECS_PER_YEAR,
            Units.SECS_PER_YEAR,
        };

        public Object clone() {
            return super.clone();
        }

        public PM1() {
            super(0.0, UNITS[0]);
        }

        public PM1(String value) {
            super(value, UNITS[0]);
        }

        public PM1(String value, Units units) {
            super(value, units);
        }

        public PM1(double value) {
            super(value, UNITS[0]);
        }

        public PM1(double value, Units units) {
            super(value, units);
        }

        public Units[] getUnitOptions() {
            return UNITS;
        }
    }

    /**
     * The PM2 (proper motion of c2) parameter type.
     */
    public static final class PM2 extends CoordinateParam {
        public static final Units[] UNITS = {
            Units.MILLI_ARCSECS_PER_YEAR,
            Units.ARCSECS_PER_YEAR,
        };

        public Object clone() {
            return super.clone();
        }

        public PM2() {
            super(0.0, UNITS[0]);
        }

        public PM2(String value) {
            super(value, UNITS[0]);
        }

        public PM2(String value, Units units) {
            super(value, units);
        }

        public PM2(double value) {
            super(value, UNITS[0]);
        }

        public PM2(double value, Units units) {
            super(value, units);
        }

        public Units[] getUnitOptions() {
            return UNITS;
        }
    }

    /**
     * The Parallax parameter type.
     */
    public static final class Parallax extends CoordinateParam {
        public static final Units[] UNITS = {Units.MILLI_ARCSECS, Units.ARCSECS};

        public Object clone() {
            return super.clone();
        }

        public Parallax() {
            super(0.0, UNITS[0]);
        }

        public Parallax(String value) {
            super(value, UNITS[0]);
        }

        public Parallax(String value, Units units) {
            super(value, units);
        }

        public Parallax(double value) {
            super(value, UNITS[0]);
        }

        public Parallax(double value, Units units) {
            super(value, units);
        }

        public Units[] getUnitOptions() {
            return UNITS;
        }

        public Double arcsecs() {
            return (getUnits() == Units.MILLI_ARCSECS) ? getValue()/1000.0 : getValue();
        }

        public Double mas() {
            return (getUnits() == Units.ARCSECS) ? getValue() * 1000.0 : getValue();
        }
    }

    /**
     * The longitude of the ascending node.
     */
    public static final class ANode extends CoordinateParam {
        public static final Units[] UNITS = {Units.DEGREES, };

        public Object clone() {
            return super.clone();
        }

        public ANode() {
            super(0.0, UNITS[0]);
        }

        public Units[] getUnitOptions() {
            return UNITS;
        }
    }

    /**
     * The mean distance (a) or perihelion distance (q).
     */
    public static final class AQ extends CoordinateParam {
        public static final Units[] UNITS = {Units.AU, };

        public Object clone() {
            return super.clone();
        }

        public AQ() {
            super(0.0, UNITS[0]);
        }

        public AQ(String value) {
            super(value, UNITS[0]);
        }

        public AQ(String value, Units units) {
            super(value, units);
        }

        public AQ(double value) {
            super(value, UNITS[0]);
        }

        public AQ(double value, Units units) {
            super(value, units);
        }

        public Units[] getUnitOptions() {
            return UNITS;
        }
    }

    /**
     * The Epoch parameter type.
     */
    public static final class Epoch extends CoordinateParam {
        public static final Units[] UNITS = {Units.YEARS, Units.JD};

        public Object clone() {
            return super.clone();
        }

        public Epoch() {
            super(2000.0, UNITS[0]);
        }

        public Epoch(String value) {
            super(value, UNITS[0]);
        }

        public Epoch(String value, Units units) {
            super(value, units);
        }

        public Epoch(double value) {
            super(value, UNITS[0]);
        }

        public Epoch(double value, Units units) {
            super(value, units);
        }

        public Units[] getUnitOptions() {
            return UNITS;
        }
    }

    /**
     * The Inclination of the orbit (i).
     */
    public static final class Inclination extends CoordinateParam {
        public static final Units[] UNITS = {Units.DEGREES, };

        public Object clone() {
            return super.clone();
        }

        public Inclination() {
            super(0.0, UNITS[0]);
        }

        public Inclination(String value) {
            super(value, UNITS[0]);
        }

        public Inclination(String value, Units units) {
            super(value, units);
        }

        public Inclination(double value) {
            super(value, UNITS[0]);
        }

        public Inclination(double value, Units units) {
            super(value, units);
        }

        public Units[] getUnitOptions() {
            return UNITS;
        }
    }

    /**
     * The longitude (L) or mean anomaly (M).
     */
    public static final class LM extends CoordinateParam {
        public static final Units[] UNITS = {Units.DEGREES, };

        public Object clone() {
            return super.clone();
        }

        public LM() {
            super(0.0, UNITS[0]);
        }

        public Units[] getUnitOptions() {
            return UNITS;
        }
    }

    /**
     * The mean daily motion.
     */
    public static final class N extends CoordinateParam {
        public static final Units[] UNITS = {Units.DEGREES_PER_DAY, };

        public Object clone() {
            return super.clone();
        }

        public N() {
            super(0.0, UNITS[0]);
        }

        public N(String value) {
            super(value, UNITS[0]);
        }

        public N(String value, Units units) {
            super(value, units);
        }

        public N(double value) {
            super(value, UNITS[0]);
        }

        public N(double value, Units units) {
            super(value, units);
        }

        public Units[] getUnitOptions() {
            return UNITS;
        }
    }

    /**
     * The argument of perihelion (Omega).
     */
    public static final class Perihelion extends CoordinateParam {
        public static final Units[] UNITS = {Units.DEGREES, };

        public Object clone() {
            return super.clone();
        }

        public Perihelion() {
            super(0.0, UNITS[0]);
        }

        public Perihelion(String value) {
            super(value, UNITS[0]);
        }

        public Perihelion(String value, Units units) {
            super(value, units);
        }

        public Perihelion(double value) {
            super(value, UNITS[0]);
        }

        public Perihelion(double value, Units units) {
            super(value, units);
        }

        public Units[] getUnitOptions() {
            return UNITS;
        }
    }

}
