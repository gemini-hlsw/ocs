// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DMS.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.system;

/**
 * Class <code>DMS</code> is used to represent one of the components of
 * the position of a celestial object.  This object provides
 * support for converting angles in degrees:minutes:seconds format over the
 * range +90 to -90 degrees.
 * <p>
 * An <code>DMS</code> is a {@link CoordinateParam};
 * therefore, in knows not only its value, but its possible units
 * {@link #getUnitOptions} and its current unit
 * {@link CoordinateParam#getUnits getUnits}.
 * <p>
 * An <code>DMS</code> uses an {@link DMSFormat} object to parse and
 * format its value.  For efficiency, a class variable is used to hold
 * one <code>DMSFormat</code> for all <code>DMS</code> instances.  However,
 * for special cases, one can use a different <code>DMSFormat</code>, setting
 * it with the {@link #setFormat setFormat} method. That way, only
 * special cases pay for lugging around an extra object.
 */
public class DMS extends CoordinateParam
        implements Cloneable {
    /* These are the units that DMS can use for conversions, get,
     * and set.  By aggreement, the zero member is the default
     * units.
     */
    public static final Units[] UNITS = {
        Units.DEGREES,
        Units.HMS,
    };

    /**
     * The default, class <code>DMSFormat</code> object
     * used for parse and format operations for all instances when
     * a <code>DMSFormat</code> has not be set with {@link #setFormat
     * setFormat}.
     */
    static public DMSFormat DEFAULT_FORMAT = new DMSFormat();

    // The instance's format is set to null, unless it uses a special format.
    // Otherwise, it uses the class format.
    private CoordinateFormat _f = null;

    /**
     * Default constructor.  Initialize to zero using the default units.
     */
    public DMS() {
        super(0.0, UNITS[0]);
    }

    /**
     * Construct with constituent pieces.
     */
    public DMS(int degrees, int minutes, double seconds) {
        super(0.0, UNITS[0]);
        setValue(getFormat().parse(degrees, minutes, seconds));
    }

    /**
     * Create a new DMS from a double.  This assumes that the double
     * value is in degrees.
     */
    public DMS(double value) {
        super(0.0, UNITS[0]);
        setValue(value);
    }

    /**
     * Initialize from a String and a
     * {@link CoordinateParam.Units}.
     * This constructor can fail if the value can not be converted to the
     * requested type.
     *
     * @throws IllegalArgumentException if the given <code>units</code> are
     * not permitted
     */
    public DMS(double value, CoordinateParam.Units units)
            throws IllegalArgumentException {
        super(0.0, units);
        // Make sure the units are okay
        checkUnits(units);
        setValue(AngleMath.convertToDegrees(value, units));
    }

    /**
     * Copy one DMS state to another.
     */
    public void copy(DMS src) {
        super.copy(src);
        // Copy a dedicated formatter, if present
        setFormat(src.getFormat());
    }

    /**
     * Initialize from a String.
     */
    public DMS(String value) {
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
     * Set a new DMSFormat object for this instance of DMS.
     * <p>
     * Note that if parameter f is set to null, the format is replaced with
     * the default format since one is required.
     */
    public void setFormat(CoordinateFormat f) {
        if (f == null) f = DEFAULT_FORMAT;
        _f = f;
    }


    /**
     * Return the <code>DMS</code> value formatted as a String.
     */
    public String toString() {
        return getFormat().format(_getValue());
    }

    // This protected method returns a value normalized between +/- 90.
    protected double _getValue() {
        return AngleMath.normalizeDec(getValue());
    }

    /**
     *Return the number of hours as an integer number of hours.
     */
    public int getDegrees() {
        return (int) _getValue();
    }


    /*
     * Return the number of minutes (not including hours or seconds).
     */
    public int getMinutes() {
        return (int) ((_getValue() - (double) getDegrees()) * 60.0);
    }


    /*
     * Return the number of seconds (not including hours and minutes).
     */
    public double getSeconds() {
        double tmp = _getValue();

        int dd = (int) tmp;
        tmp = (tmp - (double) dd) * 60.0;
        int mm = (int) tmp;
        return (tmp - (double) mm) * 60.0;
    }

    /*
     * Return the coordinate value by specifying the desired units.
     * @throws IllegalArgumentException if the given <code>units</code> are
     * not permitted.
     */
    public double getAs(Units units)
            throws IllegalArgumentException {
        // Check the units first to make sure convert will succeed.
        checkUnits(units);
        return AngleMath.convertFromDegrees(_getValue(), units);
    }

    /*
     * Set the coordinate value with a value and units.
     * @throws IllegalArgumentException if the given <code>units</code> are
     * not permitted.
     */
    public void setAs(double value, Units units) {
        // Check the units first to make sure convert will succeed.
        checkUnits(units);
        setUnits(UNITS[0]);
        // Convert to internal degrees first
        setValue(AngleMath.convertToDegrees(value, units));
    }

    /**
     * Set the DMS value using a string of the form DDxMMxSS.SSS,
     * where x is a separator.
     */
    public void setValue(String value) {
        // Set the units to the default.
        setUnits(UNITS[0]);
        setValue(getFormat().parse(value));
    }

    // Private method to set the value and normalize it before storing.
    protected void _setValue(double value) {
        /*      value = _longFlag ?
                AngleMath.normalizeRa(value) : AngleMath.normalizeDec(value);
        */
        value = AngleMath.normalizeDec(value);
        setValue(value);
    }


    /**
     * Provide the Units that DMS support for get/set methods.
     */
    public Units[] getUnitOptions() {
        return UNITS;
    }

    /**
     * Overrides <code>equals</code> to return true if the object is
     * the right type and the values are the same.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof DMS)) {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * Provides clone support.
     */
    public Object clone() {
        return (DMS) super.clone();
    }

}
