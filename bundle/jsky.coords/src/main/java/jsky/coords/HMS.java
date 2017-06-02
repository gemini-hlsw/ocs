package jsky.coords;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Class representing a value of the form "hours:min:sec".
 *
 * @version $Revision: 7518 $
 * @author Allan Brighton
 */
public class HMS implements Serializable {

    /** number of hours */
    private int hours;

    /** number of minutes */
    private int min;

    /** number of seconds */
    private double sec;

    /** value converted to decimal */
    private double val;

    /** set to 1 or -1 */
    private byte sign = 1;

    /** Used to format values as strings with decimal places. */
    private static NumberFormat nf_frac = NumberFormat.getInstance(Locale.US);

    static {
        nf_frac.setMinimumIntegerDigits(2);
        nf_frac.setMaximumFractionDigits(3);
    }

    /** Used to format values as strings with no fractional part. */
    private static NumberFormat nf_noFrac = NumberFormat.getInstance(Locale.US);

    static {
        nf_noFrac.setMinimumIntegerDigits(2);
        nf_noFrac.setMaximumFractionDigits(0);
    }

    /** On the handling of -0: from the javadoc for Double.equals():
     * "If d1 represents +0.0 while d2 represents -0.0, or vice versa,
     * the equal test has the value false, even though +0.0==-0.0 has the
     * value true."
     * The test for 0.0 != -0.0 only works with Double.equals(minusZero).
     * This case shows up in HMS values with zero hours and negative values,
     * such as "-00 24 32"
     */
    private static final Double minusZero = -0.0;

    /* true if value has been initialized */
    private boolean initialized = false;


    /** Default constructor: initialize to null values */
    public HMS() {
    }

    /**
     * Initialize with the given hours, minutes and seconds.
     */
    public HMS(double hours, int min, double sec) {
        set(hours, min, sec);
    }

    /**
     * Initialize from a decimal hours value and calculate H:M:S.sss.
     */
    public HMS(double val) {
        setVal(val);
    }

    /**
     * Copy constructor
     */
    public HMS(HMS hms) {
        setVal(hms.val);
    }

    /**
     * Initialize from a string value, in format H:M:S.sss, hh, or H M
     * S.  If the value is not in H:M:S and is not an integer (has a
     * decimal point), assume the value is in deg convert to hours by
     * dividing by 15. (Reason: some catalog servers returns RA in h:m:s
     * while others return it in decimal deg.)
     */
    public HMS(String s) {
        this(s, false);
    }

    /**
     * Initialize from a string value, in format H:M:S.sss, hh, or
     * H M S.  If the value is not in H:M:S and is not an
     * integer (has a decimal point), and hflag is true,
     * assume the value is in deg and convert to hours by dividing by 15.
     *
     * @param s the RA string
     * @param hflag if true, assume RA is always in hours, otherwise, if it has a decimal point,
     *              assume deg
     */
    public HMS(String s, boolean hflag) {
        s = s.replace(",", "."); // Treat ',' like '.', by request
        double[] vals = {0.0, 0.0, 0.0};
        StringTokenizer tok = new StringTokenizer(s, ": ");
        int n = 0;
        while (n < 3 && tok.hasMoreTokens()) {
            vals[n++] = Double.valueOf(tok.nextToken());
        }

        if (n >= 2) {
            set(vals[0], (int) vals[1], vals[2]);
        } else if (n == 1) {
            if (!hflag && s.indexOf('.') != -1)
                setVal(vals[0] / 15.);
            else
                setVal(vals[0]);
        } else {
            throw new RuntimeException("Expected a string of the form hh:mm:ss.sss, but got: '" + s + "'");
        }
    }

    /**
     * Set the hours, minutes and seconds.
     */
    public void set(double hours, int min, double sec) {
        this.hours = (int) hours;
        this.min = min;
        this.sec = sec;

        val = (sec / 60.0 + min) / 60.0;

        if (hours < 0.0 || new Double(hours).equals(minusZero)) {
            val = hours - val;
            this.hours = -this.hours;
            sign = -1;
        } else {
            val = this.hours + val;
            sign = 1;
        }
        initialized = true;
    }

    /**
     * Set from a decimal value (hours) and calculate H:M:S.sss.
     */
    public void setVal(double val) {
        this.val = val;

        double v = val; // check also for neg zero
        if (v < 0.0 || new Double(v).equals(minusZero)) {
            sign = -1;
            v = -v;
        } else {
            sign = 1;
        }

        double dd = v + 0.0000000001;
        hours = (int) dd;
        double md = (dd - hours) * 60.;
        min = (int) md;
        sec = (md - min) * 60.;
        initialized = true;
    }

    /**
     * Return the value as a String in the form hh:mm:ss.sss.
     * Seconds are formatted with leading zero if needed.
     * The seconds are formatted with 3 digits of precision.
     */
    public String toString() {
        return toString(true);
    }

    /**
     * Return the value as a String in the form hh:mm:ss.sss,
     * or if showSeconds is false, hh:mm.
     */
    public String toString(boolean showSeconds) {
        return toString(showSeconds, true);
    }

    public String toString(boolean showSeconds, boolean showFractionalSeconds) {
        final NumberFormat nf = showFractionalSeconds ? nf_frac : nf_noFrac;
        return (sign == -1 ? "-1" : "")
                + nf_noFrac.format(hours)
                + ":"
                + nf_noFrac.format(min)
                + (showSeconds ? (":" + nf.format(sec)) : "");
    }

    /** Return true if this object has been initialized with a valid value */
    public boolean isInitialized() {
        return initialized;
    }

    /** Return the number of hours (not including minutes or seconds) */
    public int getHours() {
        return hours;
    }

    /** Return the number of minutes (not including hours or seconds) */
    public int getMin() {
        return min;
    }

    /** Return the number of seconds (not including hours and minutes) */
    public double getSec() {
        return sec;
    }

    /** Return the value (fractional number of hours) as a double */
    public double getVal() {
        return val;
    }

    /** Return the sign of the value */
    public byte getSign() {
        return sign;
    }

    /** Define equality based on the value */
    public boolean equals(Object obj) {
        return (val == ((HMS) obj).val);
    }

    @Override
    public int hashCode() {
    	return (int) val;
    }
}
