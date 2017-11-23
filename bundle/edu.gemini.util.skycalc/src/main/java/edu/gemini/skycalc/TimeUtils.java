package edu.gemini.skycalc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Some constants and methods for dealing with times in ms.
 * @author rnorris
 */
public class TimeUtils {

    public static final int START_OF_DAY_HR = 14;
	public static final long MS_PER_SECOND = 1000;
	public static final long MS_PER_MINUTE = MS_PER_SECOND * 60;
	public static final long MS_PER_HOUR = MS_PER_MINUTE * 60;
	public static final long MS_PER_DAY = MS_PER_HOUR * 24;
	
	public static String msToHHMM(long span) {
		boolean neg = span < 0;
		span = Math.abs(span);
		long hh = span / MS_PER_HOUR;
		long mm = (span % MS_PER_HOUR) / MS_PER_MINUTE;
		return (neg ? "-" : "") + (hh < 10 ? "0" : "") + hh + ":" + (mm < 10 ? "0" : "") + mm;
	}

	public static String msToHMM(long span) {
		boolean neg = span < 0;
		span = Math.abs(span);
		long hh = span / MS_PER_HOUR;
		long mm = (span % MS_PER_HOUR) / MS_PER_MINUTE;
		return (neg ? "-" : "") + hh + ":" + (mm < 10 ? "0" : "") + mm;
	}

	public static String msToHHMMSS(long span) {
		boolean neg = span < 0;
		span = Math.abs(span);
		long hh = span / MS_PER_HOUR;
		long mm = (span % MS_PER_HOUR) / MS_PER_MINUTE;
		long ss = (span % MS_PER_MINUTE) / MS_PER_SECOND;
		return  (neg ? "-" : "") + hh + ":" + (mm < 10 ? "0" : "") + mm + ":" + (ss < 10 ? "0" : "") + ss;
	}

	public static String msToMMSS(long span) {
		boolean neg = span < 0;
		span = Math.abs(span);
		long mm = span / MS_PER_MINUTE;
		long ss = (span % MS_PER_MINUTE) / MS_PER_SECOND;
		return  (neg ? "-" : "") + mm + ":" + (ss < 10 ? "0" : "") + ss;
	}

	public static String hoursToHHMMSS(double hours) {
		return msToHHMM((long) (MS_PER_HOUR * hours));
	}

    /**
     * Creates a duration in milliseconds given a number of seconds.
     * @param seconds
     * @return
     */
    public static long seconds(long seconds) {
        return seconds * MS_PER_SECOND;
    }

    /**
     * Creates a duration in milliseconds given a number of minutes.
     * @param minutes
     * @return
     */
    public static long minutes(long minutes) {
        return minutes * MS_PER_MINUTE;
    }

    /**
     * Creates a duration in milliseconds given a number of hours.
     * @param hours
     * @return
     */
    public static long hours(long hours) {
        return hours * MS_PER_HOUR;
    }

    /**
     * Gets the number of hours represented by this amount of time.
     * @param time
     * @return
     */
    public static double asHours(long time) {
        return ((double)time) / MS_PER_HOUR;
    }

    /**
     * Creates a duration in milliseconds given a number of days.
     * @param days
     * @return
     */
    public static long days(long days) {
        return days * MS_PER_DAY;
    }

    /**
     * Gets the number of days represented by this amount of time.
     * @param time
     * @return
     */
    public static double asDays(long time) {
        return ((double)time) / MS_PER_DAY;
    }

    /**
     * Creates a duration in milliseconds given a number of days.
     * This is the duration of a mean sidereal day.
     * @param days
     * @return
     */
    public static long siderealDays(long days) {
        return days * (TimeUtils.hours(23) + TimeUtils.minutes(56) + TimeUtils.seconds(4));
    }

    /**
     * Creates a duration in milliseconds given a number of weeks.
     * @param weeks
     * @return
     */
    public static long weeks(long weeks) {
        return weeks * days(7);
    }

    /**
     * Creates an instance in time given a date, a time and a time zone.
     * NOTE: Month is 1 based (i.e. 1 = Jan, 2 = Feb, .. 12 = Dec)!
     * @param year
     * @param month
     * @param day
     * @param hour
     * @param minute
     * @return
     */
    public static long time(int year, int month, int day, int hour, int minute, TimeZone timeZone) {
        GregorianCalendar g = new GregorianCalendar(timeZone);
        g.set(year, month - 1, day, hour, minute, 0);
        return g.getTimeInMillis();
    }

    /**
     * Gets the 14:00 local start of day for the given time where 14hrs refers to local time of the given time zone.
     * @param t
     * @param timeZone
     * @return
     */
    public static long startOfDay(long t, TimeZone timeZone) {
        GregorianCalendar g = new GregorianCalendar(timeZone);
        g.setTimeInMillis(t - TimeUtils.hours(START_OF_DAY_HR));
        g.set(Calendar.HOUR_OF_DAY, START_OF_DAY_HR);
        g.set(Calendar.MINUTE, 0);
        g.set(Calendar.SECOND, 0);
        g.set(Calendar.MILLISECOND, 0);
        return g.getTimeInMillis();
    }

    /**
     * Gets the 14:00 local end of day for the given time where 14hrs refers to local time of the given time zone.
     * @param t
     * @param timeZone
     * @return
     */
    public static long endOfDay(long t, TimeZone timeZone) {
        long start = startOfDay(t, timeZone);
        GregorianCalendar g = new GregorianCalendar(timeZone);
        g.setTimeInMillis(start);
        g.add(Calendar.DAY_OF_MONTH, 1);
        return g.getTimeInMillis();
    }

    /**
     * Gets the millisecond of the day of the given timestamp in the given timezone.
     * @param t
     * @param timeZone
     * @return
     */
    public static long millisecondOfDay(long t, TimeZone timeZone) {
        Calendar c = calendar(t, timeZone);
        return hour(c)*MS_PER_HOUR + minute(c)*MS_PER_MINUTE + second(c)*MS_PER_SECOND;
    }

    /**
     * Gets the 14:00 local start of day for a date represented by a string formatted according to RFC3339 (yyyy-MM-dd).
     * Parse exceptions are wrapped in an unchecked IllegalArgumentException.
     * @param dateString
     * @param timeZone
     * @return
     */
    public static long startOfDay(String dateString, TimeZone timeZone) {
        try {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        f.setTimeZone(timeZone);
        return startOfDay(f.parse(dateString).getTime() + TimeUtils.hours(START_OF_DAY_HR), timeZone);
        } catch (ParseException e) {
            throw new IllegalArgumentException("invalid time format, expected yyyy-MM-dd, received " + dateString, e);
        }
    }

    /**
     * Gets the 14:00 local end of day for a date represented by a string formatted according to RFC3339 (yyyy-MM-dd).
     * Parse exceptions are wrapped in an unchecked IllegalArgumentException.
     * @param dateString
     * @param timeZone
     * @return
     */
    public static long endOfDay(String dateString, TimeZone timeZone) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            f.setTimeZone(timeZone);
            return endOfDay(f.parse(dateString).getTime(), timeZone);
        } catch (ParseException e) {
            throw new IllegalArgumentException("invalid time format, expected yyyy-MM-dd, received " + dateString, e);
        }
    }

    /** Gets a calendar object for given time and zone */
    public static Calendar calendar(long t, TimeZone timeZone) {
        GregorianCalendar g = new GregorianCalendar(timeZone);
        g.setTimeInMillis(t);
        return g;
    }

    public static int year(Calendar g) { return g.get(Calendar.YEAR); }
    public static int month(Calendar g) { return g.get(Calendar.MONTH); }
    public static int day(Calendar g) { return g.get(Calendar.DAY_OF_MONTH); }
    public static int hour(Calendar g) { return g.get(Calendar.HOUR_OF_DAY); }
    public static int minute(Calendar g) { return g.get(Calendar.MINUTE); }
    public static int second(Calendar g) { return g.get(Calendar.SECOND); }

    /**
     * Prints a calendar time with the calendars timezone and the given format.
     * @param g
     * @param format
     * @return
     */
    public static String print(Calendar g, String format) {
        return print(g.getTimeInMillis(), g.getTimeZone(), format);
    }

    /**
     * Prints the given time for the given timezone and format.
     * @param time
     * @param timeZone
     * @param format
     * @return
     */
    public static String print(Long time, TimeZone timeZone, String format) {
        SimpleDateFormat f = new SimpleDateFormat(format);
        f.setTimeZone(timeZone);
        return f.format(new Date(time));
    }

    /**
     * Prints the given time for the given time zone with a default format.
     * @param time
     * @param timeZone
     * @return
     */
    public static String print(Long time, TimeZone timeZone) {
        return print(time, timeZone, "yyyy-MM-dd HH:mm:ss");
    }
}
