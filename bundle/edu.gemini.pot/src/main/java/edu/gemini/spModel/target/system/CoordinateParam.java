// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: CoordinateParam.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.system;

import edu.gemini.spModel.pio.Param;
import edu.gemini.spModel.pio.PioFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 * The base class for target coordinate parameters and their associated
 * types.
 * <p>
 * Coordinate systems are defined by many parameters.  This class serves as the
 * base class for a single paramter.  A parameter is a combination of
 * a value and its units.  Since many parameters share the same types of
 * units, they are collected in the {@link TypeBase TypeBase}
 * subclass {@link CoordinateParam.Units Units}.  The
 * particular subclasses of CoordinateParam each define the
 * acceptable units among the options in Units by defining the
 * {@link #getUnitOptions} method which is abstract in this class.
 *
 * @author      Shane Walker
 * @author      Kim Gillies (modified for SP)
 */
public abstract class CoordinateParam implements Cloneable, Serializable {
    // for serialization
    private static final long serialVersionUID = 1L;

    /**
     * Various options for coordinate units.
     */
    public static final class Units extends TypeBase {
        // for serialization
        private static final long serialVersionUID = 1L;

        public static final int _ANGSTROMS = 0;
        public static final int _ARCSECS = 1;
        public static final int _ARCSECS_PER_YEAR = 2;
        public static final int _AU = 3;
        public static final int _DEGREES = 4;
        public static final int _DEGREES_PER_DAY = 5;
        public static final int _HMS = 6;
        public static final int _KM_PER_SEC = 7;
        public static final int _MICRONS = 8;
        public static final int _RADIANS = 9;
        public static final int _SECS_PER_YEAR = 10;
        public static final int _YEARS = 11;
        public static final int _MILLI_ARCSECS_PER_YEAR = 12;
        public static final int _JD = 13;

        public static final Units ANGSTROMS =
                new Units(_ANGSTROMS, "angstroms");

        public static final Units ARCSECS =
                new Units(_ARCSECS, "arcsecs");

        public static final Units ARCSECS_PER_YEAR =
                new Units(_ARCSECS_PER_YEAR, "arcsecs/year");

        public static final Units AU =
                new Units(_AU, "au");

        public static final Units DEGREES =
                new Units(_DEGREES, "degrees");

        public static final Units DEGREES_PER_DAY =
                new Units(_DEGREES_PER_DAY, "degrees/day");

        public static final Units HMS =
                new Units(_HMS, "hours/minutes/seconds");

        public static final Units KM_PER_SEC =
                new Units(_KM_PER_SEC, "km/sec");

        public static final Units MICRONS =
                new Units(_MICRONS, "microns");

        public static final Units RADIANS =
                new Units(_RADIANS, "radians");

        public static final Units SECS_PER_YEAR =
                new Units(_SECS_PER_YEAR, "seconds/year");

        public static final Units YEARS =
                new Units(_YEARS, "years");

        public static final Units MILLI_ARCSECS_PER_YEAR =
                new Units(_MILLI_ARCSECS_PER_YEAR, "milli-arcsecs/year");

        public static final Units JD =
                new Units(_JD, "JD");

        public static final Units[] TYPES = new Units[]{
            ANGSTROMS,
            ARCSECS,
            ARCSECS_PER_YEAR,
            AU,
            DEGREES,
            DEGREES_PER_DAY,
            HMS,
            KM_PER_SEC,
            MICRONS,
            RADIANS,
            SECS_PER_YEAR,
            YEARS,
            MILLI_ARCSECS_PER_YEAR,
            JD,
        };

        private Units(int type, String name) {
            super(type, name);
        }

        /**
         * Given a String as a Units name, return a Units object if
         * one exists.
         */
        static public Units fromString(String unitsString) {
            for (int i = 0; i < TYPES.length; i++) {
                if (unitsString.equals(TYPES[i].getName())) {
                    return TYPES[i];
                }
            }
            return null;
        }
    }


    // parameter value
    private double _value = Double.NaN;

    // parameter units
    private Units _units;


    /**
     * Provides clone support for <code>CoordinateParam</code>.
     */
    public Object clone() {
        CoordinateParam cp;
        try {
            cp = (CoordinateParam) super.clone();
        } catch (CloneNotSupportedException ex) {
            // Shouldn't ever happen.
            System.err.println("BUG: clone() called on " + getClass().getName() +
                               " but clone() is not supported.");
            ex.printStackTrace();
            System.exit(-1);
            return null; // fool the compiler
        }

        // _value is a double (immutable)
        // _units is immutable
        return cp;
    }


    /**
     * Constructs with a double value and units.
     *
     * @throws IllegalArgumentException if the given <code>units</code> are
     * not permitted
     */
    public CoordinateParam(double value, Units units)
            throws IllegalArgumentException {
        setValue(value);
        setUnits(units);
    }

    /**
     * Constructs with a String value and units.
     *
     * @throws IllegalArgumentException if the given <code>units</code> are
     * not permitted
     */
    public CoordinateParam(String value, Units units)
            throws IllegalArgumentException {
        setValue(value);
        setUnits(units);
    }

    /**
     * Copies the state of one CoordinateParam to another.
     */
    public void copy(CoordinateParam src) {
        setValue(src.getValue());
        setUnits(src.getUnits());
    }

    /**
     * Gets the current value of the parameter as a <code>String</code>.
     */
    public String getStringValue() {
        return Double.toString(_value);
    }

    /**
     * Gets the current value of the parameter as a <code>double</code>.
     */
    public double getValue() {
        return _value;
    }

    /**
     * Sets the current value of the parameter with a <code>double</code>.
     */
    public void setValue(double newValue) {
        _value = newValue;
    }

    /**
     * Sets the current value of the parameter using a <code>String</code>.
     */
    public void setValue(String newValue)
            throws NumberFormatException {
        _value = Double.parseDouble(newValue);
    }

    /**
     * Gets the units of which the parameter is expressed.
     */
    public Units getUnits() {
        return _units;
    }

    /**
     * Sets the units, provided the given value is legal.
     *
     * @throws IllegalArgumentException if the given <code>units</code>
     * are not permitted (in other words, would not be among those returned
     * by {@link #getUnitOptions})
     */
    public void setUnits(Units units)
            throws IllegalArgumentException {
        // First make sure there is any work to do.
        if (units == _units) {
            return;
        }

        checkUnits(units);
        _units = units;
    }

    /**
     * Check the given units to make sure it is acceptable.  If not known,
     * then throw the IllegalArgumentException.
     * <p>
     * This method is protected for use by concrete subclasses.
     */
    protected void checkUnits(Units units)
            throws IllegalArgumentException {
        // Check whether the given units are supported.  If not,
        // throw an exception.
        boolean ok = false;
        Units[] uA = getUnitOptions();
        for (int i = 0; i < uA.length; ++i) {
            if (uA[i] == units) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new IllegalArgumentException("Units `" + units.getName() +
                                               "' not supported.");
        }
    }


    /**
     * Gets the unit options that are legal for this parameter.  Subclasses
     * must implement this method.
     */
    public abstract Units[] getUnitOptions();


    /**
     * Override equals() to return true if both parameters have the same value
     * and units.
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof CoordinateParam)) {
            return false;
        }

        CoordinateParam cp = (CoordinateParam) obj;
        if (Double.doubleToLongBits(getValue()) !=
                Double.doubleToLongBits(cp.getValue())) {
            return false;
        }

        if (getUnits() != cp.getUnits()) {
            return false;
        }

        return true;
    }

    /**
     * This is a helper method to read the Stringified version of a
     * CoordinateParam, as specified in <code>{@link #toString}</code>
     * and initialize the value and Units.
     * <p>
     * The format assumed is value[units].  If the [units] is missing
     * an attempt is made to set only the value from the complete
     * String.  The <code>{@link #setValue(String)}</code> method is
     * used which can throw an exception.
     *
     * @throws IllegalArgumentException if the value is not valid or if the
     * Units are not appropriate for this parameter.
     */
    public void fromString(String newValue)
            throws IllegalArgumentException {
        // First check for value by looking for [
        int openBracket = newValue.indexOf('[');
        int closeBracket = newValue.indexOf(']');
        if (openBracket == -1 || closeBracket == -1) {
            //
            setValue(newValue);
            return;
        }
        // Use the positions of the []
        String value = newValue.substring(0, openBracket);
        String units = newValue.substring(openBracket + 1, closeBracket);

        // First set the value
        setValue(value);
        // Find the appropriate Units by name
        Units u = Units.fromString(units);
        if (u == null) {
            throw new IllegalArgumentException("Improper Units for parameter.");
        }
        // Can also throw IllegalArgumentException if Units are wrong for param
        setUnits(u);
    }

    /**
     * Print out a the exportable version of the parameter.
     */
    public String exportString() {
        return getStringValue() + "[" + getUnits().getName() + "]";
    }

    /**
     * Print out a friendly version of the parameter.
     */
    public String toString() {
        return exportString();
    }

    /**
     * Override hashCode() to match the definition of equals().
     * hashCode taken from java.lang.Double.
     */
    public int hashCode() {
        long bits = Double.doubleToLongBits(_value);
        int result = (int) (bits ^ (bits >> 32));

        Units u = getUnits();
        result = 37 * result + (u == null ? 0 : u.hashCode());

        return result;
    }

    /**
     * Overrides writeObject() to serialize the target type.
     */
    private void writeObject(ObjectOutputStream stream)
            throws IOException {
        // Perform default writing first.
        stream.defaultWriteObject();

        // Write the units type code.
        stream.writeInt(_units.getTypeCode());
    }

    /**
     * Overrides readObject() to deserialize the units type.
     * This ensures that the _units code points to the one unique
     * instance of the Unit.
     */
    private void readObject(ObjectInputStream stream)
            throws IOException {
        // Perform default reading first.
        try {
            stream.defaultReadObject();
        } catch (ClassNotFoundException ex) {
            throw new IOException();
        }

        // Read and set the units type, provided it is valid.
        int code = stream.readInt();
        if (code > Units.TYPES.length) {
            throw new IOException("Invalid Units code: " + code);
        }
        _units = Units.TYPES[code];
    }

    /** Return a Param describing this object. */
    public Param getParam(PioFactory factory, String name) {
        Param param = factory.createParam(name);
        param.setValue(Double.toString(_value));
        param.setUnits(_units.getName());
        return param;
    }

    /** Initialize this object from the given Param. */
    public void setParam(Param p) {
        _value = Double.parseDouble(p.getValue());
        _units = Units.fromString(p.getUnits());
    }
}
