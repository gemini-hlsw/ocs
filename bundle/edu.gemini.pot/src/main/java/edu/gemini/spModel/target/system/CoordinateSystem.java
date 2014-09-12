// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: CoordinateSystem.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.system;

import java.io.Serializable;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

/**
 * The base class for target coordinate systems.  Since the various
 * types of coordinate systems differ greatly, the exact positional
 * elements are left to the subclasses.  This class contains higher
 * level methods for which a useful interpretation could be applied to any
 * particular system.
 *
 * <p>
 * Subclasses must implement three methods:
 * <ul>
 *   <li>{@link #getSystemName}
 *   <li>{@link #getPosition}
 *   <li>{@link #getSystemOptions}
 * </ul>
 *
 * <p>
 * Within any given system, there are multiple options that are all
 * described with the same positional information.  For instance,
 * systems specified with two positions C1 and C2, both in degrees,
 * minutes, and seconds could be "altaz", "galactic", or even altaz
 * "mount" coordinates.  This class provides access to these (sub)
 * options, which must be specified as {@link TypeBase} instances.
 *
 * No syncrhonization, immutability or properties are supported in
 * classes based upon <code>CoordinateSystem</code>.  For these
 * features use the appropriate wrapper classes.
 *
 * @author      Shane Walker (Phase 1 Version)
 * @author      Kim Gillies (SP Version)
 */
public abstract class CoordinateSystem
        implements Cloneable, Serializable {
    // coordinate system option
    private transient TypeBase _systemOption;

    /**
     * Provides clone support.
     */
    public Object clone() {
        CoordinateSystem result;
        try {
            result = (CoordinateSystem) super.clone();
        } catch (CloneNotSupportedException ex) {
            // Shouldn't ever happen.
            System.err.println("BUG: clone() called on " + getClass().getName() +
                               " but clone() is not supported.");
            ex.printStackTrace();
            System.exit(-1);
            return null; // fool the compiler
        }

        // Want to just keep the bitwise copy of the _systemOption
        // reference since it is an immutable.
        return result;
    }

    /**
     * Constructs with the system option.
     *
     * @throws IllegalArgumentException if the given <code>systemOption</code>
     * is not permitted
     */
    public CoordinateSystem(TypeBase systemOption)
            throws IllegalArgumentException {
        _setSystemOption(systemOption);
    }

    /**
     * Gets the system's name.  This method must be implemented by a
     * subclass.
     */
    public abstract String getSystemName();

    /**
     * Gets a short description of the position.  For instance, the
     * {@link HmsDegTarget} might return its RA and Dec in a
     * formated String such as "RA=12:34:56 Dec=00:11:22".  To actually
     * use a coordinate system's position, the client will have to use its
     * particular interface rather than this method.
     */
    public abstract String getPosition();

    /**
     * Gets the available types, or options within the system.  For instance,
     * the {@link HmsDegTarget} has options for J2000, B1950, apparent, etc.
     */
    public abstract TypeBase[] getSystemOptions();

    /**
     * Gets the currently selected option (for instance "J2000").
     */
    public TypeBase getSystemOption() {
        return _systemOption;
    }

    /**
     * Sets the system option, provided the given value is legal.
     *
     * @exception IllegalArgumentException if the given
     * <code>systemOption</code> are not permitted (in other words,
     * would not be among those returned by {@link #getSystemOptions})
     */
    protected void _setSystemOption(TypeBase systemOption)
            throws IllegalArgumentException {
        // First make sure there is any work to do.
        if (systemOption == _systemOption) {
            return;
        }

        checkSystemOption(systemOption);
        _systemOption = systemOption;
    }


    /**
     * Sets the coordinate system (sub)option.
     *
     * @throws IllegalArgumentException if <code>type</code> is an unknown
     * type; in other words, if it would not have been returned by the
     * {@link #getSystemOptions} method
     */
    public void setSystemOption(TypeBase newValue)
            throws IllegalArgumentException {
        _setSystemOption(newValue);
    }


    /**
     * Check the given system option to make sure it is acceptable.  If not
     * known then throw the IllegalArgumentException.
     * This is provided for subclass implementations.
     */
    protected void checkSystemOption(TypeBase systemOption)
            throws IllegalArgumentException {
        // Check whether the given systemOption is supported.  If not, throw
        // an exception.
        boolean ok = false;
        TypeBase[] tbA = getSystemOptions();
        for (int i = 0; i < tbA.length; ++i) {
            if (tbA[i] == systemOption) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new IllegalArgumentException("Coordinate System Option `"
                                               + systemOption.getName() + "' not supported.");
        }
    }

    /**
     * Overrides writeObject() to serialize the coordinate system option.
     * Required in order to ensure that that one object exists for each
     * SystemOption.
     */
    private void writeObject(ObjectOutputStream stream)
            throws IOException {
        // Write the target type code.
        stream.writeInt(_systemOption.getTypeCode());
    }

    /**
     * Overrides readObject() to deserialize the system option.
     */
    private void readObject(ObjectInputStream stream)
            throws IOException {
        // Perform default reading first.
        try {
            stream.defaultReadObject();
        } catch (ClassNotFoundException ex) {
            throw new IOException();
        }

        // Read and set the target type, provided it is valid.
        int code = stream.readInt();
        TypeBase[] tbA = getSystemOptions();
        if (code > tbA.length) {
            throw new IOException("Invalid System Option code: " + code);
        }
        _systemOption = tbA[code];
    }

}
