/**
 * $Id: CalendarUtil.java 8039 2007-08-07 20:32:06Z swalker $
 */

package jsky.plot.util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * Utility methods for using the {@link java.util.Calendar} class.
 */
public class CalendarUtil {

    /**
     * Set the time of day from decimal hours (0..24). If next day is true, use the next day's date.
     */
    public static void setHours(Calendar cal, double hours, boolean nextDay) {
        int h = (int) hours;
        double md = (hours - h) * 60.;
        int min = (int) md;
        double sd = (md - min) * 60.;
        int sec = (int)sd;
        int ms = (int)((sd - sec) * 1000);

        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, sec);
        cal.set(Calendar.MILLISECOND, ms);

        if (nextDay) {
            cal.add(Calendar.HOUR_OF_DAY, 24);
        }
    }

    /**
     * Format the given calendar's date and return it.
     */
    public static String toString(Calendar cal) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        dateFormat.setTimeZone(cal.getTimeZone());
        return dateFormat.format(cal.getTime());
    }

    /**
     * Format the given date in the named timezone and return it.
     */
    public static String toString(Date date, String timezoneName) {
        TimeZone tz = TimeZone.getTimeZone(timezoneName);
        Calendar cal = Calendar.getInstance(tz);
        cal.setTime(date);
        return toString(cal);
    }

    /**
     * Format the given time value (seconds since 1970...) in the named timezone and return it.
     */
    public static String toString(long time, String timezoneName) {
        return toString(new Date(time), timezoneName);
    }

    /** Test main. */
    public static void main(String[] args) {
        Calendar cal = Calendar.getInstance();
        CalendarUtil.setHours(cal, 23.750001, false);
        System.out.println(CalendarUtil.toString(cal));
    }
}
