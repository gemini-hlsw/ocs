// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DMSFormat.java 21620 2009-08-20 19:41:32Z swalker $
//
package edu.gemini.spModel.target.system;

import java.util.StringTokenizer;

/**
 * <code>DMSFormat</code> is a support class that localizes the reading
 * and writing of angles in degrees/minutes/seconds.
 * Support for converting angles in degrees:minutes:seconds format over the
 * circular range +/-1000 degrees is provided.
 *<p>
 * It is assumed that classes using <code>DMSFormat</code> will clamp
 * the values to appropriate values before handing them to
 * <code>DMSFormat</code>.
 */
public final class DMSFormat extends CoordinateFormat
        implements Cloneable {
    // For external users who wish to know what precision to use.
    public static final int DEFAULT_PRECISION = CoordinateFormat.DEFAULT_DMS_PRECISION;

    /**
     * Default constructor.  Initialize to zeros.
     * The precision of the DMSFormat should be equivalent to that
     * used for HMSFormat.
     */
    public DMSFormat() {
        super(DEFAULT_SEPARATOR, DEFAULT_PRECISION);
    }

    /**
     * Construct with a specific separator and precision.
     */
    public DMSFormat(FormatSeparator s, int precision) {
        super(s, precision);
    }

    /**
     * Construct with a specific precision and the default separator.
     */
    public DMSFormat(int precision) {
        super(DEFAULT_SEPARATOR, precision);
    }

    /**
     * Construct with a specific separator and default precision.
     */
    public DMSFormat(FormatSeparator s) {
        super(s, DEFAULT_PRECISION);
    }

    /**
     * Format an DMS in degrees to a String in HH:MM:SS.SS format.
     */
    public String format(double degrees) {
        // Flag for noting a negative value
        boolean negative = degrees < 0;
        // Make positive if needed
        if (negative) degrees *= -1;

        int dd = (int) degrees;
        double tmp = (degrees - (double) dd) * 60.0;
        int mm = (int) tmp;
        double ss = (tmp - (double) mm) * 60.0;

        // correct for formating errors caused by rounding
        if (ss > 59.99999) {
            ss = 0;
            mm += 1;
            if (mm >= 60) {
                mm = 0;
                dd += 1;
            }
        }

        tmp = dd * 10000 + mm * 100 + ss;
        if (negative) tmp *= -1;

        //System.out.println("--> " + dd + ", " + mm + ", " + ss + "--> " + tmp);
        // Clear the global StringBuffer
        StringBuffer out = new StringBuffer();

        // Check to see if we need any changes in the output
        // Output assumes COLON, the most common
        out = getDecimalFormat().format(tmp, out, _zero);

        // Reverse the string so it's easier to work with patching
        out.reverse();
        //System.out.println("[Rd->s:" + degrees + "/" + _out + "]");
        // Special hack for degree values greater than +/- 99.  The
        // DecimalFormat only handles one grouping (in our case 2) so
        // the formatting of degrees greater than 99 fails.  I've arbitrarily
        // decided to patch this in order to continue to use the consistent
        // rounding of DecimalFormat.  This just marches through and removes
        // separator colon in the degrees area.  Note the hardcoded, but
        // reasonably hard coded (I think) magic numbers, which are locations
        // of key separators.
        int precision = getPrecision();
        if (dd > 99) {
            // First colon loc in reversed output is at
            int colonLoc = precision + 9;
            while (colonLoc < out.length()) {
                // Check for negative
                if (out.charAt(colonLoc) == FormatSeparator.COLON_SEPARATOR) {
                    out.deleteCharAt(colonLoc);
                }
                colonLoc += 2;
            }
        }
        switch (getSeparator().getTypeCode()) {
            case FormatSeparator._SPACES:
                out.setCharAt(precision + 3, FormatSeparator.SPACE_SEPARATOR);
                out.setCharAt(precision + 6, FormatSeparator.SPACE_SEPARATOR);
                break;
            case FormatSeparator._LETTERS:
                // Note reverse!
                out.setCharAt(precision + 3, FormatSeparator.MINUTE_LETTER_SEPARATOR);
                out.setCharAt(precision + 6, FormatSeparator.DEGREE_LETTER_SEPARATOR);
                out.insert(0, FormatSeparator.SECOND_LETTER_SEPARATOR);
                break;
        }
        // Now reverse for correct order
        out.reverse();
        //System.out.println("[d->s:" + degrees + "/" + _out + "]");
        return out.toString();
    }

    /**
     * Check that the input string matches the internal pattern.
     * Convert from an dec-like value in DD:MM:SS string format to degrees.
     */
    public double parse(String s)
            throws NumberFormatException {
        if (s == null) throw new NumberFormatException(s);

        // Determine the sign from the (trimmed) string
        s = s.trim();
        if (s.length() == 0) throw new NumberFormatException(s);

        s = s.replace(",", ".");

        // Decs can have a - or even a +
        int sign = 1;
        if (s.charAt(0) == '-') {
            sign = -1;
            s = s.substring(1);
        }
        if (s.charAt(0) == '+') {
            s = s.substring(1);
        }

        // Parse the string into values for hours, min, and sec
        double[] vals = {0.0, 0.0, 0.0};
        String toks = ":hmsd ";
        StringTokenizer tok = new StringTokenizer(s, toks);
        for (int i = 0; i < 3 && tok.hasMoreTokens(); i++) {
            vals[i] = Double.valueOf(tok.nextToken());
        }

        // Convert DDD:MM:SS to degrees
        return _dmsToDouble(sign, vals[0], vals[1], vals[2]);
    }

    public double parse(int degrees, int minutes, double seconds) {
        //Convert DD:MM:SS to degrees
        int sign = 1;
        if (degrees < 0.0) {
            sign = -1;
            degrees = sign * degrees;
        }

        return _dmsToDouble(sign, degrees, minutes, seconds);
    }

    // Internal implementation method to convert
    private double
            _dmsToDouble(int sign, double degrees, double minutes, double seconds) {
        //Convert HH:MM:SS to degrees
        return sign * (degrees + minutes / 60.0 + seconds / 3600.0);
    }

    /**
     * Overrides <code>equals</code> to return true if the object is
     * the right type and the values are the same.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof DMSFormat)) return false;
        return super.equals(obj);
    }
}
