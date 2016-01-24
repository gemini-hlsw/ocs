package edu.gemini.shared.util;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple data type for expressing an amount of time.  The time
 * amount is represented as a float and associated with a (time) Units
 * object.  This object is immutable.
 */
public final class TimeValue implements Cloneable, Comparable<TimeValue>, Serializable {
    /**
     * The number of hours are in a "night".
     */
    public static final int HOURS_PER_NIGHT = 8;

    /**
     * Time units.
     */
    public enum Units {
        nights(HOURS_PER_NIGHT * 60 * 60 * 1000),
        hours(60 * 60 * 1000),
        minutes(60 * 1000),
        seconds(1000);

        /**
         * The default time unit, which is {@link #hours}.
         */
        public static final Units DEFAULT = hours;

        private long _milliseconds;

        Units(long milliseconds) {
            _milliseconds = milliseconds;
        }

        public double convertFromMilliseconds(long millisec) {
            return millisec / (double) _milliseconds;
        }

        public long convertToMilliseconds(double timeAmount) {
            return Math.round(timeAmount * _milliseconds);
        }
    }

    /** Used to format values as strings. */
    private static NumberFormat _nf = NumberFormat.getInstance();

    /**
     * Converts the given number of <code>milliseconds</code> to a TimeValue
     * with the given <code>units</code>.
     */
    public static TimeValue millisecondsToTimeValue(long milliseconds, Units units) {
        return new TimeValue(milliseconds, units);
    }

    private static final Pattern PAT = Pattern.compile("(\\S+)\\s(\\w+)");

    public static TimeValue parse(String time) {
        if ((time == null) || "".equals(time.trim())) {
            return new TimeValue(0L, Units.DEFAULT);
        }

        time = time.trim();

        // See if the value can be parsed as a double.
        try {
            double val = Double.valueOf(time);
            return new TimeValue(val, Units.DEFAULT);
        } catch (NumberFormatException ex) {
            // didn't parse, assume it has units
        }

        // Parse the string as a double with units
        Matcher mat = PAT.matcher(time);
        if (!mat.matches()) {
            throw new IllegalArgumentException(time + " does not parse as a TimeValue");
        }

        double val;
        try {
            val = Double.valueOf(mat.group(1));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(time + " does not parse as a TimeValue");
        }

        Units units;
        try {
            units = Units.valueOf(mat.group(2));
        } catch (Exception ex) {
            units = Units.DEFAULT; // keeping the old behavior
        }
        return new TimeValue(val, units);
    }

    public static final TimeValue ZERO_NIGHTS = new TimeValue(0L, Units.nights);
    public static final TimeValue ZERO_HOURS  = new TimeValue(0L, Units.hours);

    private final long _milliseconds;
    private final Units _units;

    /**
     * Constructs with the given time amount and units.
     */
    public TimeValue(double timeAmount, Units timeUnits) {
        _milliseconds = timeUnits.convertToMilliseconds(timeAmount);
        _units        = timeUnits;
    }

    /**
     * Constructs with the given number of milliseconds and units.
     */
    private TimeValue(long milliseconds, Units timeUnits) {
        _milliseconds = milliseconds;
        _units        = timeUnits;
    }


    /**
     * Gets the time amount apart from its units.
     */
    public double getTimeAmount() {
        return _units.convertFromMilliseconds(_milliseconds);
    }

    /**
     * Gets the time in milliseconds corresponding to this TimeValue.
     */
    public long getMilliseconds() {
        return _milliseconds;
    }

    /**
     * Gets the time units for this time object.
     */
    public Units getTimeUnits() {
        return _units;
    }


    /**
     * Gets a time amount equal to the time amount represented by the TimeValue
     * object, but in the given units.  This method is similar to the
     * {@link #convertTo} method, but <em>no</em> objects are constructed as a
     * side-effect.
     *
     * <p>This method does not modify this object in any way, it simply returns
     * its time amount converted to the given <code>timeUnits</code>.
     */
    public double convertTimeAmountTo(Units timeUnits) {
        return timeUnits.convertFromMilliseconds(_milliseconds);
    }


    /**
     * Returns a TimeValue instance with an equivalent amount of time expressed
     * in the given units.
     *
     * <p>This method does not modify this object in any way, it simply returns
     * a TimeValue instance representing the same amount of time expressed in
     * the given <code>timeUnits</code>.
     */
    public TimeValue convertTo(Units timeUnits) {
        if (timeUnits == _units) return this;
        return new TimeValue(_milliseconds, timeUnits);
    }

    /**
     * Combines (adds) the given TimeValue to this one, returning a result that
     * has the combined time amount and the Units of this TimeValue.
     *
     * @param that TimeValue to add to this TimeValue
     *
     * @return combined TimeValue using this TimeValue's Units
     */
    public TimeValue add(TimeValue that) {
        long ms = _milliseconds + that._milliseconds;
        return new TimeValue(ms, _units);
    }

    /**
     * Compares this TimeValue to the given TimeValue.  The absolute amount
     * of time represented by each TimeValue is the primary discriminator
     * between two time values (with smaller time periods being sorted before
     * longer time periods).  If the two TimeValues represent the same amount
     * of time, then larger {@link Units} are sorted before smaller Units.
     * For example, 1 minute sorts before 60 seconds.
     *
     * @param that the other TimeValue to compare against
     */
    @Override
    public int compareTo(TimeValue that) {
        if (_milliseconds != that._milliseconds) {
            return _milliseconds < that._milliseconds ? -1 : 1;
        }
        return _units.compareTo(that._units);
    }

    /**
     * Makes a clone of this object.
     */
    @SuppressWarnings({"CloneDoesntCallSuperClone"})
    public Object clone() {
        // This object is immutable
        return this;
    }

    /**
     * Overrides to display the time amount and units.
     */
    public String toString() {
        return getTimeAmount() + " " + _units.name();
    }

    /**
     * Returns the time amount and units as a string with the given number of decimal
     * places.
     */
    public String toString(int decimalPlaces) {
        _nf.setMaximumFractionDigits(decimalPlaces);
        return _nf.format(getTimeAmount()) + " " + _units.name();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof TimeValue)) {
            return false;
        }
        TimeValue that = (TimeValue) obj;

        if (_milliseconds != that._milliseconds) return false;
        return _units == that._units;
    }

    public int hashCode() {
        int res = (int) (_milliseconds ^ (_milliseconds >>> 32));
        res = 37*res + _units.hashCode();
        return res;
    }

}
