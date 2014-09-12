package edu.gemini.shared.gui.calendar;

import java.util.Calendar;

import edu.gemini.shared.util.CalendarUtil;

/**
 * This class represents a month within a year.  It allows for date
 * arithmetic in an easy way.
 * Note it has a 0-based month to correspond with GregorianCalendar.
 * It makes life easier when you are working with time at the level
 * of months.
 */

public class MonthYear {

    static final int YEAR = 0;

    static final int MONTH = 1;

    static final int MONTHS_IN_YEAR = 12;

    public int year;

    public int month;  // 0-based month

    public MonthYear() {
        this(0, 0);
    }

    /**
     * Constructs a MonthYear for the month containing specified date.
     */
    public MonthYear(Calendar c) {
        this(c.get(Calendar.YEAR), c.get(Calendar.MONTH));
    }

    public MonthYear(int year, int month) {
        this.year = year;
        this.month = month;
    }

    public MonthYear(MonthYear m) {
        this.year = m.year;
        this.month = m.month;
    }

    /**
     * Adds the specified amount of time to this MonthYear.
     */
    public void add(int field, int amount) {
        if (field == YEAR) {
            year += amount;
        } else if (field == MONTH) {
            if (amount > 0) {
                while (month + amount >= MONTHS_IN_YEAR) {
                    amount -= (12 - month);
                    month = 0;
                    year++;
                }
                month += amount;
            } else if (amount < 0) {
                amount = -amount;
                while (month - amount < 0) {
                    amount -= month + 1;
                    month = MONTHS_IN_YEAR - 1;
                    year--;
                }
                month -= amount;
            }
        }
    }

    /**
     * Returns true if a equals b.
     */
    public boolean equals(MonthYear arg) {
        return (year == arg.year && month == arg.month);
    }

    /**
     * Returns true if this is after arg.
     */
    public boolean after(MonthYear arg) {
        if (year > arg.year)
            return true;
        if (year < arg.year)
            return false;
        if (month > arg.month)
            return true;
        if (month < arg.month)
            return false;
        return false;  // they are equal
    }

    /**
     * Returns true if this is before arg.
     */
    public boolean before(MonthYear arg) {
        if (year < arg.year)
            return true;
        if (year > arg.year)
            return false;
        if (month < arg.month)
            return true;
        if (month > arg.month)
            return false;
        return false;  // they are equal
    }

    /**
     * Returns a MonthYear representing a span of time between the
     * two arguments.  They can be in any order.
     */
    public static MonthYear difference(MonthYear a, MonthYear b) {
        MonthYear m1, m2;
        if (a.before(b)) {
            m1 = a;
            m2 = b;
        } else {
            m2 = a;
            m1 = b;
        }
        // m1 < m2
        MonthYear returnValue = new MonthYear();
        returnValue.year = m2.year - m1.year;
        if (m2.month >= m1.month) {
            returnValue.month = m2.month - m1.month;
        } else {
            returnValue.year--;
            returnValue.month = MONTHS_IN_YEAR - 1 - (m1.month - m2.month);
        }
        return returnValue;
    }

    /**
     * Treats the MonthYear as a time interval and returns the number of
     * months in the interval.  i.e. 12 * month + year
     */
    public int getMonths() {
        return (MONTHS_IN_YEAR * year + month);
    }

    public String toString() {
        return new String(CalendarUtil.getMonthName(month) + ", " + year);
    }

}

