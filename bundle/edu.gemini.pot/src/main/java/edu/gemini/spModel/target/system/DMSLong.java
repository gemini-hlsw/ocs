// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DMSLong.java 18053 2009-02-20 20:16:23Z swalker $
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
public final class DMSLong extends DMS {
    /**
     * Default constructor.  Initialize to zero using the default units.
     */
    public DMSLong() {
        super();
    }

    /**
     * Construct with constituent pieces.
     */
    public DMSLong(int degrees, int minutes, double seconds) {
        super(degrees, minutes, seconds);
    }

    /**
     * Create a new DMS from a double.  This assumes that the double
     * value is in degrees.
     */
    public DMSLong(double value) {
        super(value);
    }

    /**
     * Initialize from a String and a
     * {@link CoordinateParam.Units CoordinateParam.Units}.
     * This constructor can fail if the value can not be converted to the
     * requested type.
     *
     * @throws IllegalArgumentException if the given <code>units</code> are
     * not permitted
     */
    public DMSLong(double value, CoordinateParam.Units units)
            throws IllegalArgumentException {
        super(value, units);
    }

    /**
     * Initialize from a String.
     */
    public DMSLong(String value) {
        super(value);
    }


    // This method is called when the value is requested.  Making
    // the value appear between the longitude limits.
    protected double _getValue() {
        return AngleMath.normalizeRa(getValue());
    }


}
