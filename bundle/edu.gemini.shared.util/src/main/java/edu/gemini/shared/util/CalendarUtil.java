// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
//

package edu.gemini.shared.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.DateFormatSymbols;

/**
 * This class contains general utility functions for dealing with Calendars.
 * Amazingly, the Calendar class is void of concepts such as before and after.
 * The before and after methods of Calendar apply only to the time portion
 * of the value and do not take the date into account.
 * Note that in Calendar, month numberss are zero-based and day-of-week
 * and day-of-month numbers are 1-based.
 * This class is NOT MT-safe!
 */
public class CalendarUtil {

    // Many methods need a Calendar so they can access non-static
    // Calendar methods, but they don't care about the date.
    // Save time by having a static instance already created.
    // Get at this instance using _getStaticCalendarInstance().
    // No client should rely on any particular value being in this instance.
    private static Calendar _staticCalendar = null;

    // Many methods need a DateFormatSymbols so they can access non-static
    // methods.
    // Save time by having a static instance already created.
    // Get at this instance using _getStaticDateFormatSymbolsInstance()
    // No client should rely on any particular value being in this instance.
    private static DateFormatSymbols _staticDateFormatSymbols;

    private CalendarUtil() {
    }

    /**
     * A handy way to get a hold of a Calendar.  Some Calendar methods are
     * begging to be static, but are not.  So you need a Calendar instance
     * to access them.
     * Any method using this is not MT-safe.
     */
    public static Calendar getStaticCalendarInstance() {
        if (_staticCalendar == null) {
            _staticCalendar = new GregorianCalendar();  // don't care about the date
        }
        return _staticCalendar;
    }

    /**
     * A handy way to get a hold of a DateFormatSymbols instance.
     * A client could conserve resources by using this instance.
     * Any method using this is not MT-safe.
     */
    public static DateFormatSymbols getStaticDateFormatSymbolsInstance() {
        if (_staticDateFormatSymbols == null) {
            _staticDateFormatSymbols = new DateFormatSymbols();
        }
        return _staticDateFormatSymbols;
    }

    /**
     * This allows comparison of Calendars and can be used in other
     * compareTo() methods to support sorting by date and implementing Comparable.
     */
    public static int compareTo(Calendar c1, Calendar c2) {
        if (before(c1, c2))
            return -1;
        if (after(c1, c2))
            return 1;
        return 0;
    }

    /**
     * Returns true if timestamp represented by c1 is after c2.
     * Note it returns false if they are equal.
     * Don't mix the types of the Calendars, results would be undefined.
     */
    public static boolean after(Calendar c1, Calendar c2) {
        return (c1.getTime().getTime() > c2.getTime().getTime());
    }

    /**
     * Returns true if timestamp represented by c1 is before c2.
     * Note it returns false if they are equal.
     * Don't mix the types of the Calendars, results would be undefined.
     */
    public static boolean before(Calendar c1, Calendar c2) {
        return (c1.getTime().getTime() < c2.getTime().getTime());
    }

    /**
     * Returns true if timestamp represented by c1 is equal to c2
     * down the the millisecond level.
     * Don't mix the types of the Calendars, results would be undefined.
     * Could just compare Dates or total milliseconds.
     */
    public static boolean equals(Calendar c1, Calendar c2) {
        return (c1.getTime().getTime() == c2.getTime().getTime());
    }

    /**
     * Returns the smaller of the two Calendars.  If they are equal, returns c1.
     */
    public static Calendar min(Calendar c1, Calendar c2) {
        return after(c1, c2) ? c2 : c1;
    }

    /**
     * Returns the larger of the two Calendars.  If they are equal, returns c1.
     */
    public static Calendar max(Calendar c1, Calendar c2) {
        return before(c1, c2) ? c2 : c1;
    }

    /**
     * Sets the precision of the time represented in a Calendar object.
     * @param c The Calendar.
     * @param calendarField All fields of the calendar below this
     * are cleared.  For example if calendarField = Calendar.MONTH,
     * then DAY_OF_MONTH, HOUR, MINUTE, SECOND and MILLISECOND are cleared.
     * Supported fields are YEAR, MONTH, DAY_OF_MONTH, HOUR, MINUTE,
     * SECOND, MILLISECOND.
     * If field value is not recognized, the whole Calendar will be cleared.
     */
    public static Calendar setPrecision(Calendar c, int calendarField) {
        Calendar clone = (Calendar) c.clone();
        c.clear();
        c.set(c.YEAR, clone.get(c.YEAR));
        if (calendarField == Calendar.YEAR)
            return c;
        c.set(c.MONTH, clone.get(c.MONTH));
        if (calendarField == Calendar.MONTH)
            return c;
        c.set(c.DAY_OF_MONTH, clone.get(c.DAY_OF_MONTH));
        if (calendarField == Calendar.DAY_OF_MONTH)
            return c;
        c.set(c.HOUR, clone.get(c.HOUR));
        if (calendarField == Calendar.HOUR)
            return c;
        c.set(c.MINUTE, clone.get(c.MINUTE));
        if (calendarField == Calendar.MINUTE)
            return c;
        c.set(c.SECOND, clone.get(c.SECOND));
        if (calendarField == Calendar.SECOND)
            return c;
        c.set(c.MILLISECOND, clone.get(c.MILLISECOND));
        if (calendarField == Calendar.MILLISECOND)
            return c;
        // Unrecognized Calendar field.  Just return the calendar like it was.
        return c;
    }

    /**
     * Sets the precision of the time represented in a Date object.
     * @param d The Date.
     * @param calendarField All fields of the calendar below this
     * are cleared.  For example if calendarField = Calendar.MONTH,
     * then DAY_OF_MONTH, HOUR, MINUTE, SECOND and MILLISECOND are cleared.
     * Supported fields are YEAR, MONTH, DAY_OF_MONTH, HOUR, MINUTE,
     * SECOND, MILLISECOND.
     * If field value is not recognized, the whole Calendar will be cleared.
     */
    public static Date setPrecision(Date d, int calendarField) {
        setPrecision(d, calendarField, TimeZone.getDefault());
        return d;
    }

    /**
     * Sets the precision of the time represented in a Date object.
     * @param d The Date.
     * @param calendarField All fields of the calendar below this
     * @param tz The time zone used to interpret the date
     * are cleared.  For example if calendarField = Calendar.MONTH,
     * then DAY_OF_MONTH, HOUR, MINUTE, SECOND and MILLISECOND are cleared.
     * Supported fields are YEAR, MONTH, DAY_OF_MONTH, HOUR, MINUTE,
     * SECOND, MILLISECOND.
     * If field value is not recognized, the whole Calendar will be cleared.
     */
    public static Date setPrecision(Date d, int calendarField, TimeZone tz) {
        GregorianCalendar g = new GregorianCalendar();
        g.setTimeZone(tz);
        g.setTime(d);
        setPrecision(g, calendarField);
        d.setTime(g.getTime().getTime());
        return d;
    }

    /**
     * A default constructed Calendar has the current date and time.
     * If you fill in the year, month and day, it will still have an
     * odd hour, min, sec, etc.
     * This method allows you to create a Calendar representing a date
     * with the time portion zeroed out.
     * Month is zero-based.
     * TimeZone of this calendar is the default time zone.
     */
    public static GregorianCalendar newGregorianCalendarInstance(int year, int month, int day) {
        GregorianCalendar g = new GregorianCalendar();
        g.clear();
        g.set(year, month, day);
        return g;
    }

    /**
     * A default constructed Calendar has the current date and time.
     * If you fill in the year, month and day, it will still have an
     * odd hour, min, sec, etc.
     * This method allows you to create a Calendar representing a date
     * with the time portion zeroed out.
     * Month is zero-based.
     * TimeZone of this calendar is the default time zone.
     */
    public static GregorianCalendar newGregorianCalendarInstance(int year, int month, int day, TimeZone timeZone) {
        GregorianCalendar g = new GregorianCalendar(timeZone);
        g.clear();
        g.set(year, month, day);
        return g;
    }

    /**
     * Constructs a Date object representing specified time with a
     * precision of days in default time zone.
     */
    public static Date newDate(int year, int month, int day) {
        GregorianCalendar g = new GregorianCalendar();
        g.clear();
        g.set(year, month, day);
        return g.getTime();
    }

    /**
     * Constructs a GregorianCalendar object representing specified time with a
     * precision of days in default time zone.
     */
    public static GregorianCalendar newGregorianCalendarInstance(int year, int month) {
        GregorianCalendar g = new GregorianCalendar();
        g.clear();
        g.set(year, month, getStaticCalendarInstance().getMinimum(Calendar.DAY_OF_MONTH));
        return g;
    }

    /**
     * Constructs a Date object representing specified time with a
     * precision of months in default time zone.
     */
    public static Date newDate(int year, int month) {
        GregorianCalendar g = new GregorianCalendar();
        g.clear();
        g.set(year, month, getStaticCalendarInstance().getMinimum(Calendar.DAY_OF_MONTH));
        return g.getTime();
    }

    /**
     * Creates a calendar in default time zone from specified Date.
     */
    public static Calendar newGregorianCalendarInstance(Date d) {
        return newGregorianCalendarInstance(d,TimeZone.getDefault());
    }

    /**
     * Creates a calendar in the given time zone from specified Date.
     */
    public static Calendar newGregorianCalendarInstance(Date d, TimeZone tz) {
        GregorianCalendar g = new GregorianCalendar(tz);
        g.setTime(d);
        return g;
    }

    /**
     * Rolls the day of the month to the first day of month.
     */
    public static void rollToBeginningOfMonth(Calendar c) {
        c.set(Calendar.DAY_OF_MONTH, getStaticCalendarInstance().getMinimum(Calendar.DAY_OF_MONTH));
    }

    /**
     * Rolls the day of the month to the last day of month.
     */
    public static void rollToEndOfMonth(Calendar c) {
        c.set(Calendar.DAY_OF_MONTH, daysInMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH)));
    }

    /**
     * 1-based day number of first day of specified zero-based month.
     */
    public static int dayOfWeekOfFirstDayOfMonth(int year, int month) {
        GregorianCalendar c = newGregorianCalendarInstance(year, month, getStaticCalendarInstance().getMinimum(Calendar.DAY_OF_MONTH));
        return c.get(Calendar.DAY_OF_WEEK);
    }

    /**
     * 1-based day number of last day of specified zero-based month.
     */
    public static int dayOfWeekOfLastDayOfMonth(int year, int month) {
        GregorianCalendar c = newGregorianCalendarInstance(year, month, getStaticCalendarInstance().getMinimum(Calendar.DAY_OF_MONTH));
        c.add(Calendar.MONTH, 1);
        c.add(Calendar.DAY_OF_MONTH, -1);
        return c.get(Calendar.DAY_OF_WEEK);
    }

    /**
     * Number of days in specified zero-based month.
     */
    public static int daysInMonth(int year, int month) {
        GregorianCalendar c = newGregorianCalendarInstance(year, month, getStaticCalendarInstance().getMinimum(Calendar.DAY_OF_MONTH));
        c.add(Calendar.MONTH, 1);   // go to first of next month
        c.add(Calendar.DAY_OF_MONTH, -1);    // go back one day to end of month
        return c.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Returns the name of the month corresponding to the 0-base monthNumber.
     */
    public static String getMonthName(int monthNumber) {
        // getMonths is 0-based!!
        return getStaticDateFormatSymbolsInstance().getMonths()[monthNumber];
    }

    /**
     * Returns the name of the month implied by the calendar.
     */
    public static String getMonthName(Calendar c) {
        return getMonthName(c.get(Calendar.MONTH));
    }

    /**
     * Returns the short name of the month corresponding to the 0-base monthNumber.
     */
    public static String getShortMonthName(int monthNumber) {
        // getMonths is 0-based!!
        return getStaticDateFormatSymbolsInstance().getShortMonths()[monthNumber];
    }

    /**
     * Returns the name of the month implied by the calendar.
     */
    public static String getShortMonthName(Calendar c) {
        return getShortMonthName(c.get(Calendar.MONTH));
    }

    /**
     * Returns the name of the day corresponding to the 1-base dayNumber.
     * e.g. 4 = "Wednesday"
     */
    public static String getDayName(int dayNumber) {
        // getWeekdays is 1-based!!
        return getStaticDateFormatSymbolsInstance().getWeekdays()[dayNumber];
    }

    /**
     * Returns the short name of the day corresponding to the 1-base dayNumber.
     * e.g. 4 = "Wed"
     */
    public static String getShortDayName(int dayNumber) {
        // getShortWeekdays is 1-based!!
        return getStaticDateFormatSymbolsInstance().getShortWeekdays()[dayNumber];
    }

    /**
     * Returns the first letter of the day corresponding to the 1-base dayNumber.
     * e.g. 4 = "W"
     */
    public static String getDayFirstLetter(int dayNumber) {
        // getShortWeekdays is 1-based!!
        String s = getShortDayName(dayNumber);
        return s.substring(0, 1);
    }

    /**
     * Returns a string representation of the calendar showing all fields.
     * This is useful for debugging.
     */
    public static String toDebugString(Date d) {
        return toDebugString(newGregorianCalendarInstance(d));
    }

    /**
     * Returns a string representation of the calendar showing all fields.
     * This is useful for debugging.
     */
    public static String toDebugString(Calendar c) {
        String s = "year: " + c.get(c.YEAR) + "\n";
        s += "month: " + c.get(c.MONTH) + "\n";
        s += "day_of_month: " + c.get(c.DAY_OF_MONTH) + "\n";
        s += "hour: " + c.get(c.HOUR_OF_DAY) + "\n";
        s += "minute: " + c.get(c.MINUTE) + "\n";
        s += "second: " + c.get(c.SECOND) + "\n";
        s += "millisecond: " + c.get(c.MILLISECOND) + "\n";
        s += "total milliseconds: " + c.getTime().getTime();
        return s;
    }

    /**
     * Returns a readable representation of a Calendar.
     * Format looks like 04-Oct-1998 09:04:38
     */
    public static String toString(Calendar c) {
        if (c == null)
            return "";
        return toDateString(c) + " " + toTimeString(c);
    }

    /**
     * Returns a readable representation of a Date.
     * Format looks like 04-Oct-1998 09:04:38
     */
    public static String toString(Date d) {
        if (d == null)
            return "";
        return toDateString(d) + " " + toTimeString(d);
    }

    /**
     * Returns a readable representation of a Calendar year, month and day.
     * Format looks like 04-Oct-1998.
     */
    public static String toDateString(Calendar c) {
        if (c == null)
            return "";
        return _twoDigitString(c.get(Calendar.DAY_OF_MONTH)) + "-" + getShortMonthName(c) + "-" + c.get(Calendar.YEAR);
    }

    /**
     * Returns a readable representation of a Date year, month and day.
     * WARNING: uses a Calendar in the local TimeZone to convert the Date
     * to year, month, day.
     * Format looks like 04-Oct-1998.
     */
    public static String toDateString(Date d) {
        if (d == null)
            return "";
        Calendar c = newGregorianCalendarInstance(d);
        return _twoDigitString(c.get(Calendar.DAY_OF_MONTH)) + "-" + getShortMonthName(c) + "-" + c.get(Calendar.YEAR);
    }

    /**
     * Returns a readable representation of a Calendar's time (i.e. ignores
     * year, month, day) in hh:mm:ss format.  e.g. like 09:04:34.
     */
    public static String toTimeString(Calendar c) {
        if (c == null)
            return "";
        return _twoDigitString(c.get(Calendar.HOUR_OF_DAY)) + ":" + _twoDigitString(c.get(Calendar.MINUTE)) + ":" + _twoDigitString(c.get(Calendar.SECOND));
    }

    /**
     * Returns a readable representation of a Date's time (i.e. ignores
     * year, month, day) in hh:mm:ss format.  e.g. like 09:04:34.
     * WARNING: uses a Calendar in the local TimeZone to convert the Date.
     */
    public static String toTimeString(Date d) {
        if (d == null)
            return "";
        Calendar c = newGregorianCalendarInstance(d);
        return _twoDigitString(c.get(Calendar.HOUR_OF_DAY)) + ":" + _twoDigitString(c.get(Calendar.MINUTE)) + ":" + _twoDigitString(c.get(Calendar.SECOND));
    }

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

    // Returns a string representing the value i in two digits.
    // i.e. 0 -> 00  4 -> 04
    private static String _twoDigitString(int i) {
        if (i < 0 || i > 99)
            return "**";
        if (i == 0)
            return "00";
        if (i < 10)
            return "0" + i;
        return "" + i;
    }

}
