// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: HMS.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.system;

/**
 * Class <code>HMS</code> is used to represent one of the components of
 * the position of a celestial object.  This object provides
 * support for converting angles in hours:minutes:seconds format over the
 * circular range 0, 24 hours.
 * <p>
 * An <code>HMS</code> is a {@link CoordinateParam};
 * therefore, in knows not only its value, but its possible units
 * {@link #getUnitOptions} and its current unit
 * {@link CoordinateParam#getUnits getUnits}.
 * <p>
 * An <code>HMS</code> uses an {@link HMSFormat} object to parse and
 * format its value.  For efficiency, a class variable is used to hold
 * one <code>HMSFormat</code> for all <code>HMS</code> instances.  However,
 * for special cases, one can use a different <code>HMSFormat</code>, setting
 * it with the {@link #setFormat setFormat} method. That way, only
 * special cases pay for lugging around an extra object.
 * <p>
 * Internally, the <code>CoordinateParam</code> is kept as degrees.
 */
public class HMS extends CoordinateParam
        implements ICoordinate, Cloneable {
    /* These are the units that HMS can use for conversions, get,
     * and set.  By aggreement, the zero member is the default
     * units.
     */
    public static final Units[] UNITS = {
        Units.DEGREES,
        Units.HMS,
    };

    /**
     * The default, class <code>HMSFormat</code> object
     * used for parse and format operations for all instances when
     * an <code>HMSFormat</code> has not be set with {@link #setFormat
     * setFormat}.
     */
    static public HMSFormat DEFAULT_FORMAT = new HMSFormat();

    // The instance's format is set to null, unless it uses a special format.
    // Otherwise, it uses the class format.
    private CoordinateFormat _f = null;

    /**
     * Default constructor.  Initialize to zero, use the default units.
     */
    public HMS() {
        super(0.0, UNITS[0]);
    }

    /**
     * Construct with constituent pieces.
     */
    public HMS(int hours, int minutes, double seconds) {
        super(0.0, UNITS[0]);
        setValue(getFormat().parse(hours, minutes, seconds));
    }

    /**
     * Create a new HMS from a double.  This assumes that the double
     * value is in time units.
     */
    public HMS(double value) {
        super(value, UNITS[0]);
    }

    /**
     * Initialize from a String and a
     * {@link CoordinateParam.Units}.
     * This method can fail if the value can not be converted to the
     * requested type.
     *
     * @throws IllegalArgumentException if the given <code>units</code> are
     * not permitted
     */
    public HMS(double value, CoordinateParam.Units units)
            throws IllegalArgumentException {
        super(0.0, units);
        // Make sure the units are okay
        checkUnits(units);
        setValue(AngleMath.convertToDegrees(value, units));
    }

    /**
     * Initialize an <code>HMS</code> from a String.  The object is
     * assumed to be in the default units.
     */
    public HMS(String value) {
        super(0.0, UNITS[0]);
        setValue(getFormat().parse(value));
    }


    // Return the formatter.  This handles the use of the static and private
    // format objects.
    public CoordinateFormat getFormat() {
        if (_f != null) return _f;
        return DEFAULT_FORMAT;
    }

    /**
     * Set a new <code>HMSFormat</code> object for this instance.
     * <p>
     * Note that if parameter f is set to null, the format is replaced with
     * the default format since one is required.
     */
    public void setFormat(CoordinateFormat f) {
        if (f == null) f = DEFAULT_FORMAT;
        _f = f;
    }


    /**
     * Return the <code>HMS</code> value formatted as a String.
     */
    public String toString() {
        return getFormat().format(getValue());
    }


    /**
     *Return the number of hours as an integer number of hours.
     */
    public int getHours() {
        return (int) (getValue() / 15.0);
    }


    /*
     * Return the number of minutes (not including hours or seconds).
     */
    public int getMinutes() {
        return (int) ((getValue() / 15.0 - (double) getHours()) * 60.0);
    }


    /*
     * Return the number of seconds (not including hours and minutes).
     */
    public double getSeconds() {
        double tmp = getValue() / 15.0;

        int hh = (int) tmp;
        tmp = (tmp - (double) hh) * 60.0;
        int mm = (int) tmp;
        return (tmp - (double) mm) * 60.0;
    }


    /**
     * Return the coordinate value by specifying the desired units.
     * @throws IllegalArgumentException if the given <code>units</code> are
     * not permitted.
     */
    public double getAs(Units units)
            throws IllegalArgumentException {
        // Check the units first to make sure convert will succeed.
        checkUnits(units);
        return AngleMath.convertFromDegrees(getValue(), units);
    }


    /**
     * Set the coordinate value with a value and units.
     * @throws IllegalArgumentException if the given <code>units</code> are
     * not permitted.
     */
    public void setAs(double value, Units units) {
        // Check the conversion units first to make sure convert will succeed.
        checkUnits(units);
        setUnits(UNITS[0]);
        setValue(AngleMath.convertToDegrees(value, units));
    }

    /**
     * Set the HMS value using a string of the form HHxMMxSS.SSS.
     */
    public void setValue(String value) {
        // Set the units to the default.
        setUnits(UNITS[0]);
        setValue(getFormat().parse(value));
    }

    /**
     * Provide the Units that HMS support for get/set methods.
     */
    public Units[] getUnitOptions() {
        return UNITS;
    }


    /**
     * Overrides <code>equals</code> to return true if the object is
     * the right type and the values are the same.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof HMS)) {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * Provides clone support.
     */
    public Object clone() {
        return (HMS) super.clone();
    }


}
