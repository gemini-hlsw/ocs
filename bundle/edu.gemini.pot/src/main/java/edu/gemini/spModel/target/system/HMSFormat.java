// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: HMSFormat.java 21620 2009-08-20 19:41:32Z swalker $
//
package edu.gemini.spModel.target.system;

import java.util.StringTokenizer;

/**
 * <code>HMSFormat</code> is a support class that localizes the reading
 * and writing of angles in hours/minutes/seconds.
 * Support for converting angles in hours:minutes:seconds format over the
 * circular range 0, 24 hours.
 */
public final class HMSFormat extends CoordinateFormat
        implements Cloneable {
    // For external users who wish to know what precision to use.
    public static final int DEFAULT_PRECISION = CoordinateFormat.DEFAULT_HMS_PRECISION;

    /**
     * Default constructor.  Initialize to zeros.
     */
    public HMSFormat() {
        super(DEFAULT_SEPARATOR, DEFAULT_PRECISION);
    }

    /**
     * Construct with a specific separator and precision.
     */
    public HMSFormat(FormatSeparator s, int precision) {
        super(s, precision);
    }

    /**
     * Construct with a specific precision and the default separator.
     */
    public HMSFormat(int precision) {
        super(DEFAULT_SEPARATOR, precision);
    }

    /**
     * Construct with a specific separator and default precision.
     */
    public HMSFormat(FormatSeparator s) {
        super(s, DEFAULT_PRECISION);
    }

    /**
     * Provides clone support.
     */
    public Object clone() {
        return super.clone();
    }

    /**
     * Format an HMS in degrees to a String in HH:MM:SS.SS format.
     */
    public String format(double degrees) {
        // Make sure the angle is between 0 (inclusive) and 360 (exclusive)
        degrees = AngleMath.normalizeRa(degrees);

        double tmp = degrees / 15.0;
        int hh = (int) tmp;
        tmp = (tmp - (double) hh) * 60.0;
        int mm = (int) tmp;
        double ss = (tmp - (double) mm) * 60.0;

        // correct for formating errors caused by rounding
        if (ss > 59.99999) {
            ss = 0;
            mm += 1;
            if (mm >= 60) {
                mm = 0;
                hh += 1;
                if (hh >= 24) hh -= 24.0;
            }
        }

        tmp = hh * 10000 + mm * 100 + ss;

        //System.out.println("--> " + hh + ", " + mm + ", " + ss + "--> " + tmp);
        // Clear the global StringBuffer
        StringBuffer out = new StringBuffer();

        // Check to see if we need any changes in the output
        // Output assumes COLON, the most common
        out = getDecimalFormat().format(tmp, out, _zero);
        switch (getSeparator().getTypeCode()) {
            case FormatSeparator._SPACES:
                out.setCharAt(2, FormatSeparator.SPACE_SEPARATOR);
                out.setCharAt(5, FormatSeparator.SPACE_SEPARATOR);
                break;
            case FormatSeparator._LETTERS:
                out.setCharAt(2, FormatSeparator.HOUR_LETTER_SEPARATOR);
                out.setCharAt(5, FormatSeparator.MINUTE_LETTER_SEPARATOR);
                out.append(FormatSeparator.SECOND_LETTER_SEPARATOR);
                break;
        }
        //System.out.println("[d->s:" + degrees + "/" + _out + "]");
        return out.toString();
    }

    /**
     * Check that the input string matches the internal pattern.
     * Convert from an RA in HH:MM:SS string format to degrees.
     */
    public double parse(String s)
            throws NumberFormatException {
        if (s == null) throw new NumberFormatException(s);

        // Determine the sign from the (trimmed) string
        s = s.trim();
        if (s.length() == 0) throw new NumberFormatException(s);

        s = s.replace(",", "."); // allow "," in place of "."

        // Shouldn't really have to deal with this.
        int sign = 1;
        if (s.charAt(0) == '-') {
            sign = -1;
            s = s.substring(1);
        }
        if (s.length() != 0 && s.charAt(0) == '+') { // length could have been set to 0 above
            s = s.substring(1);
        }

        // Parse the string into values for hours, min, and sec
        double[] vals = {0.0, 0.0, 0.0};
        String toks = ":hmsd ";
        StringTokenizer tok = new StringTokenizer(s, toks);
        for (int i = 0; i < 3 && tok.hasMoreTokens(); i++) {
            vals[i] = Double.valueOf(tok.nextToken());
        }

        // Convert HH:MM:SS to degrees
        return _hmsToDouble(sign, vals[0], vals[1], vals[2]);
        //System.out.println("[s->d:" + s + "/" + out + "]");
    }

    /**
     * Create a double from a value for hours, minutes, and seconds.
     */
    public double parse(int hours, int minutes, double seconds) {
        //Convert HH:MM:SS to degrees
        int sign = 1;
        if (hours < 0.0) sign = -1;
        return _hmsToDouble(sign, hours, minutes, seconds);
    }

    // Internal implementation method to convert
    private double
            _hmsToDouble(int sign, double hours, double minutes, double seconds) {
        //Convert HH:MM:SS to degrees
        double out = sign * (hours + minutes / 60.0 + seconds / 3600.0) * 15.0;
        return AngleMath.normalizeRa(out);
    }

    /**
     * Overrides <code>equals</code> to return true if the object is
     * the right type and the values are the same.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof HMSFormat)) return false;
        return super.equals(obj);
    }
}
