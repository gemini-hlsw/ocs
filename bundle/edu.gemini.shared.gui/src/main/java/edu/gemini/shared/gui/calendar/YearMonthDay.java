package edu.gemini.shared.gui.calendar;

import java.util.Calendar;
import java.util.Date;

import edu.gemini.shared.util.CalendarUtil;

/**
 * This class represents a day within a month within a year.  It allows
 * treatment of dates without complexity of time zones.
 * It is basically an ordered triple of integers.
 * Note it has a 0-based month to correspond with GregorianCalendar.
 * It makes life easier when you are working with time at the level
 * of months.
 * Day is 1-based day of month.
 */

public class YearMonthDay implements Comparable {

    static final int YEAR = 0;

    static final int MONTH = 1;

    static final int DAY = 2;

    static final int MONTHS_IN_YEAR = 12;

    public int year;

    public int month;  // 0-based month

    public int day;    // 1-based day of month

    public YearMonthDay() {
        this(0, 0, 1);
    }

    /**
     * Constructs a YearMonthDay for the month containing specified date.
     */
    public YearMonthDay(Date d) {
        this(CalendarUtil.newGregorianCalendarInstance(d));
    }

    /**
     * Constructs a YearMonthDay for the month containing specified date.
     */
    public YearMonthDay(Calendar c) {
        this(c.get(c.YEAR), c.get(c.MONTH), c.get(c.DAY_OF_MONTH));
    }

    public YearMonthDay(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public YearMonthDay(YearMonthDay m) {
        this.year = m.year;
        this.month = m.month;
        this.day = m.day;
    }

    /**
     * Override the clone() method to allow cloning.
     */
    public Object clone() {
        return new YearMonthDay(year, month, day);
    }

    /**
     * Returns true if a equals b.
     */
    public boolean equals(Object o) {
        if (!(o instanceof YearMonthDay))
            return false;
        YearMonthDay arg = (YearMonthDay) o;
        return (year == arg.year && month == arg.month && day == arg.day);
    }

    /**
     * Implements the Comparable interface.
     */
    public int compareTo(Object o) {
        if (!(o instanceof YearMonthDay))
            return -1;
        YearMonthDay arg = (YearMonthDay) o;
        if (year > arg.year)
            return 1;
        if (year < arg.year)
            return -1;
        if (month > arg.month)
            return 1;
        if (month < arg.month)
            return -1;
        if (day > arg.day)
            return 1;
        if (day < arg.day)
            return -1;
        return 0;  // they are equal
    }

    /**
     * Returns true if this is after arg.
     */
    public boolean after(YearMonthDay arg) {
        if (year > arg.year)
            return true;
        if (year < arg.year)
            return false;
        if (month > arg.month)
            return true;
        if (month < arg.month)
            return false;
        if (day > arg.day)
            return true;
        if (day < arg.day)
            return false;
        return false;  // they are equal
    }

    /**
     * Returns true if this is before arg.
     */
    public boolean before(YearMonthDay arg) {
        if (year < arg.year)
            return true;
        if (year > arg.year)
            return false;
        if (month < arg.month)
            return true;
        if (month > arg.month)
            return false;
        if (day < arg.day)
            return true;
        if (day > arg.day)
            return false;
        return false;  // they are equal
    }

    public String toString() {
        return new String(day + "-" + CalendarUtil.getMonthName(month) + "-" + year);
    }

}

