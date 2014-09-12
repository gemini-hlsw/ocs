// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ICoordinate.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.system;

import edu.gemini.spModel.target.system.CoordinateParam.Units;

/**
 * Interface <code>ICoordinate</code> defines one of the components of
 * the position of a celestial object in a Coordinate System.  The
 * interface provides a number of methods that work no matter what
 * the coordinate may refer to.  For instance, it may be a
 * position in hours, minutes, seconds, or degrees, minutes, seconds.
 */
public interface ICoordinate {

    /*
     * Return the coordinate value by specifying the desired units.
     * @throws IllegalArgumentException if the given <code>units</code> are
     * not permitted.
     */
    public double getAs(Units units)
            throws IllegalArgumentException;

    /*
     * Set the coordinate value with a value and units.
     * @throws IllegalArgumentException if the given <code>units</code> are
     * not permitted.
     */
    public void setAs(double value, Units units)
            throws IllegalArgumentException;

    /**
     * Set the <code>ICoordinate</code> value using a string of the
     * form XXxMMxSS.SSS where x is a separator.
     */
    public void setValue(String value);

    /**
     * Return the <code>ICoordinate</code> value formatted as a String.
     */
    public String toString();

    /**
     * Sets a {@link CoordinateFormat} for the {@link ICoordinate}.
     *
     * @throws IllegalArgumentException The <code>ICoordinate</code> may
     * throw this exception if it feels the <code>CoordinateFormat</code> is
     * inappropriate.
     */
    public void setFormat(CoordinateFormat f)
            throws IllegalArgumentException;

    /**
     * Provide the Units that <code>ICoordinate</code> support for
     * get/set methods.
     */
    public Units[] getUnitOptions();

}
