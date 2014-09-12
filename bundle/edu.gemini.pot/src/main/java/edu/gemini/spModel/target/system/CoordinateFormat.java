// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: CoordinateFormat.java 21620 2009-08-20 19:41:32Z swalker $
//
package edu.gemini.spModel.target.system;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;

/**
 * <code>AbstractPostitionFormat</code> is an abstract base
 * class to be extended by formatters of sexigesimal coordinates like an RA
 * 10:22:33.2 or declination, which can be negative showing a - sign.
 * <p>
 * This class is modeled after and uses a {@link java.text.DecimalFormat}
 * object to format the output string.  The support for handling
 * precision of the seconds output is provided by that class.
 * <p>
 * Concrete subclass must implement two methods:
 * {@link CoordinateFormat#parse} and
 * {@link CoordinateFormat#format}.  The parse method takes a
 * String that should be in sexigesimal format.  The format method
 * handles a double in whatever units are appropriate as determined
 * by the subclass.
 *
 * @version $Id: CoordinateFormat.java 21620 2009-08-20 19:41:32Z swalker $
 * @author Kim Gillies
 */
public abstract class CoordinateFormat
        implements Serializable, Cloneable {
    // Current precision, which is the number of digits to the right
    // of the decimal in the seconds.
    protected static final int DEFAULT_DMS_PRECISION = 2;
    protected static final int DEFAULT_HMS_PRECISION = 3;
    protected static final FormatSeparator DEFAULT_SEPARATOR =
            FormatSeparator.COLON;

    // This pattern handles positive and negative values.
    private static final String PATTERN = "00,00,00.00;-#00,00,00.00";

    // Required to write the DecimalFormat
    protected static FieldPosition _zero =
            new FieldPosition(NumberFormat.INTEGER_FIELD);

    // Current Separator choice
    private final int precision;
    private FormatSeparator separator;

    /**
     * The default constructor uses
     */
    protected CoordinateFormat() {
        this(DEFAULT_SEPARATOR, DEFAULT_HMS_PRECISION);
    }

    /**
     * Construct with a specific separator and precision.
     * In this case, the precsion is the number of decimal places visible
     * to the right of the decimal in the seconds (time or angular) when
     * the position is formatted.
     */
    protected CoordinateFormat(FormatSeparator s, int precision) {
        this.separator = s;
        this.precision = precision;
    }

    /**
     * Provides clone support.
     */
    public Object clone() {
        CoordinateFormat result;
        try {
            result = (CoordinateFormat) super.clone();
        } catch (CloneNotSupportedException ex) {
            // Shouldn't ever happen.
            System.err.println("BUG: clone() called on " + getClass().getName() +
                               " but clone() is not supported.");
            ex.printStackTrace();
            System.exit(-1);
            return null; // fool the compiler
        }

        // Separator is immutable
        return result;
    }

    protected DecimalFormat getDecimalFormat() {
        DecimalFormat res = new DecimalFormat(PATTERN);
        DecimalFormatSymbols syms = res.getDecimalFormatSymbols();
        syms.setGroupingSeparator(':');
        res.setDecimalFormatSymbols(syms);
        //  _df.setGroupingSize(2);
        res.setMaximumFractionDigits(precision);
        res.setMinimumFractionDigits(precision);
        return res;
    }


    /**
     * Fetch the the current {@link FormatSeparator} for this position.
     */
    public FormatSeparator getSeparator() {
        return separator;
    }

    /**
     * Returns the current seconds precision value.
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * Convert from in XX:MM:SS string format to the appropriate units.
     * This must be implemented by a subclass.
     * @throws NumberFormatException if the parse can not produce an
     * acceptable double from the input String.
     */
    public abstract double parse(String s)
            throws NumberFormatException;

    /**
     * Convert from separate value, minutes, and seconds, where value
     * is in the units appropriate for the subclass.
     * @throws NumberFormatException if the parse can not produce an
     * acceptable double from the three values.
     */
    public abstract double parse(int value, int minutes, double seconds)
            throws NumberFormatException;

    /**
     * Format the output in the way determined by a subclass.
     * This must be implemented by a subclass.
     */
    public abstract String format(double degrees);

    /**
     * Overrides <code>equals</code> to return true if the object is
     * the right type and the values are the same.
     */
    public boolean equals(Object obj) {
        // Easy tests
        if (obj == null) return false;
        if (this == obj) return true;

        if (!(obj instanceof CoordinateFormat)) return false;

        CoordinateFormat that = (CoordinateFormat) obj;

        // Now check the separator
        if (separator != that.separator) return false;
        return precision == that.precision;
    }

    /**
     * Overrides writeObject() to serialize the separator type.
     */
    private void writeObject(ObjectOutputStream stream)
            throws IOException {
        // Perform default writing first.
        stream.defaultWriteObject();

        // Write the separator type code.
        stream.writeInt(separator.getTypeCode());
    }

    /**
     * Overrides readObject() to deserialize the separator type.
     * This ensures that the _separator code points to the one unique
     * instance of the object.
     */
    private void readObject(ObjectInputStream stream)
            throws IOException {
        // Perform default reading first.
        try {
            stream.defaultReadObject();
        } catch (ClassNotFoundException ex) {
            throw new IOException();
        }

        // Read and set the separator to be the globoal instance
        int code = stream.readInt();
        if (code > FormatSeparator.TYPES.length) {
            throw new IOException("Invalid separator code: " + code);
        }
        separator = FormatSeparator.TYPES[code];
    }
}
