//
// $Id: TimeAmountFormatter.java 6807 2005-12-05 17:12:50Z shane $
//

package edu.gemini.spModel.time;

/**
 * A utility for formatting time amounts (not clock times).  The time amount
 * should be expressed as hours, minutes, and seconds where the number of
 * hours might be more than 24.  In other words, the times are indicating how
 * long was spent doing something, not when something happened.
 */
public final class TimeAmountFormatter {

    private static final int HOUR = 3600000;
    private static final int MIN  =   60000;
    private static final int SEC  =    1000;

    private TimeAmountFormatter() {
    }

    private static long[] _computeTimes(long time) {

        long hours = time / HOUR;  // hours ignoring fractional part min & sec

        time = time - (hours * HOUR); // get just the fractional part

        long minutes = time/MIN;      // minutes ignoring seconds

        time = time - (minutes * MIN);

        long seconds = Math.round((double)time/SEC);

        if (seconds == 60) {
            seconds = 0;
            minutes += 1;
            if (minutes == 60) {
                minutes = 0;
                hours += 1;
            }
        }

        return new long[] {hours, minutes, seconds};
    }

    private static long[] _computeMilliTimes(long time) {

        long hours = time / HOUR;  // hours ignoring fractional part min & sec

        time = time - (hours * HOUR);  // get just the fractional part

        long minutes = time/MIN;       // minutes ignoring seconds

        time = time - (minutes * MIN); // get just the fractional part.

        long seconds = time/SEC;       // seconds ignoring milliseconds

        long ms = time - (seconds * SEC);

        return new long[] {hours, minutes, seconds, ms};
    }

    /**
     * Formats a time amount to [-]HH:MM:SS.  Note that if the hour amount
     * requires more than two digits, it will take as many as required.
     */
    public static String getHMSFormat(long time) {
        long absTime = Math.abs(time);
        long[] res = _computeTimes(absTime);

        StringBuilder buf = new StringBuilder();
        // append a - sign if negative but something that won't be rounded to 0
        if ((absTime != time) && (absTime > 500)) {
            buf.append("-");
        }
        buf.append(String.format("%02d:%02d:%02d", res[0], res[1], res[2]));
        return buf.toString();
    }

    public static String getHMSMilliFormat(long time) {
        long absTime = Math.abs(time);
        long[] res = _computeMilliTimes(absTime);

        StringBuilder buf = new StringBuilder();

        // append a - sign if negative but something that won't be rounded to 0
        if (absTime != time) buf.append("-");
        buf.append(String.format("%02d:%02d:%02d.%03d", res[0], res[1], res[2], res[3]));
        return buf.toString();
    }

    private static void _appendTime(long time, String units, StringBuffer buf) {
        if (time == 0) return;
        buf.append(time).append(" ").append(units);
        if (time > 1) buf.append("s");
        buf.append(" ");
    }

    /**
     * Formats a time amount to a String of the form: "H hours M mins S secs".
     */
    public static String getDescriptiveFormat(long time) {
        long absTime = Math.abs(time);
        long[] res = _computeTimes(absTime);

        StringBuffer buf = new StringBuffer();
        if ((absTime != time) && (absTime > 500)) {
            buf.append("-");
        }

        // Append the hours, if any.
        _appendTime(res[0], "hour", buf);

        // Append the minutes, if any.
        _appendTime(res[1], "min", buf);

        // Append the seconds, if any.
        _appendTime(res[2], "sec", buf);

        if (buf.length() == 0) {
            buf.append("0 secs");
        }

        return buf.toString();
    }
}
