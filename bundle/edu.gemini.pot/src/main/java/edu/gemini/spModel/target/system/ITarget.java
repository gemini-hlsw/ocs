// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ITarget.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.system;

import edu.gemini.spModel.target.system.CoordinateTypes.Epoch;

import java.io.Serializable;

/**
 * This interface describes methods that must be implemented by
 * all coordinate systems.  A coordinate system consists of a
 * set of parameters that describe the position of a celestial object.
 *
 * @author      Kim Gillies
 */
public interface ITarget extends Serializable {
    /**
     * Gets the coordinate's system name as a String.
     */
    String getSystemName();

    /**
     * Returns a short one word name for the coordinate system name.
     */
    String getShortSystemName();

    /**
     * Returns an optional name for the target.
     */
    String getName();

    /**
     * Sets an optional name for the target.
     */
    void setName(String name);

    /**
     * Returns an optional brightness for the target.
     */
    String getBrightness();

    /**
     * Sets an optional brightness for the target.
     */
    void setBrightness(String brightness);

    /**
     * Gets a short description of the position.  For instance, the
     * {@link HmsDegTarget} might return its RA and Dec in a
     * formated String such as "RA=12:34:56 Dec=00:11:22".  To actually
     * use a coordinate system's position, the client will have to use its
     * particular interface rather than this method.
     */
    String getPosition();

    /**
     * Returns the position as {@link HmsDegTarget} for external use by
     * client programs.
     */
    HmsDegTarget getTargetAsJ2000();

    /**
     * Set the system as {@link HmsDegTarget}.
     * <p>
     * Note that the <code>HmsDegTarget</code> used to set must have
     * the J2000 system option.
     *
     * @throws IllegalArgumentException This can fail if the target system
     *     does not allow conversion.
     */
    void setTargetWithJ2000(HmsDegTarget system)
            throws IllegalArgumentException;

    /**
     * Set the first Coordinate using an appropriate ICoordinate.
     *
     * @throws IllegalArgumentException if the <code>ICoordinate</code> is
     * not an appropriate type.
     */
    void setC1(ICoordinate c1)
            throws IllegalArgumentException;

    /**
     * Get the first Coordinate as an {@link ICoordinate}.
     * This is generally, the internally used <code>ICoordinate</code> and
     * should be used with care.
     */
    ICoordinate getC1();

    /**
     * Set the second Coordinate using an appropriate {@link ICoordinate}.
     *
     * @throws IllegalArgumentException if the <code>ICoordinate</code> is
     * not an appropriate type.
     */
    void setC2(ICoordinate c2)
            throws IllegalArgumentException;

    /**
     * Get the second Coordinate as an {@link ICoordinate}.
     * This is generally, the internally used <code>ICoordinate</code>
     * and should be used with care.
     */
    ICoordinate getC2();

    /**
     * Set the first and second coordinates using appropriate
     * {@link ICoordinate} objects.
     */
    void setC1C2(ICoordinate c1, ICoordinate c2)
            throws IllegalArgumentException;

    /**
     * Set the first Coordinate using a String.
     *
     * @throws IllegalArgumentException if the argument can not be parsed
     * correctly.
     */
    void setC1(String c1)
            throws IllegalArgumentException;

    /**
     * Gets the first coordinate as a String.
     */
    String c1ToString();

    /**
     * Set the second Coordinate using a String.
     *
     * @throws IllegalArgumentException if the argument can not be parsed
     * correctly.
     */
    void setC2(String c2)
            throws IllegalArgumentException;

    /**
     * Gets the second coordinate as a String.
     */
    String c2ToString();

    /**
     * Set the first and second coordinates using appropriate String objects.
     *
     * @throws IllegalArgumentException if either of the arguments can not
     * be parsed correctly.
     */
    void setC1C2(String c1, String c2)
            throws IllegalArgumentException;

    /**
     * Return the Epoch of this target position.
     * @throws IllegalArgumentException if the coordinate system does not
     * support the Epoch concept.
     */
    Epoch getEpoch()
            throws IllegalArgumentException;

    /**
     * Set the Epoch of this target position.
     * @throws IllegalArgumentException if the coordinate system does not
     * support the Epoch concept.
     */
    public void setEpoch(Epoch e)
            throws IllegalArgumentException;

    /**
     * Gets the available types, or options within the system.  For instance,
     * the {@link HmsDegTarget} has options for J2000, B1950, apparent, etc.
     */
    TypeBase[] getSystemOptions();

    /**
     * Gets the currently selected option (for instance "J2000").
     */
    TypeBase getSystemOption();

    /**
     * Sets the coordinate system (sub)option.
     *
     * @throws IllegalArgumentException if <code>type</code> is an unknown
     * type; in other words, if it would not have been returned by the
     * {@link #getSystemOptions} method.
     */
    void setSystemOption(TypeBase newValue)
            throws IllegalArgumentException;

    /**
     * Provides clone support, but without Exception.
     */
    public Object clone();

    /**
     * Provides testing of equality of two targets.
     */
    public boolean equals(Object obj);

}
