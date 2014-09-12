package edu.gemini.shared.gui.calendar;

import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

import edu.gemini.shared.util.CalendarUtil;

/**
 * This event represents a selection of a date on a calendar widget.
 * Note that calendar widgets do not have a concept of time zone.
 * So a selection is really just a year, month and day.
 */

public class CalendarSelectionEvent extends java.util.EventObject {

    private int _year;

    private int _month;

    private int _day;

    /**
     * Constructor
     * @param source - the RangeCalendar originating the event
     * @param date - the selected date
     */
    public CalendarSelectionEvent(Object source, int year, int month, int day) {
        super(source);
        _year = year;
        _month = month;
        _day = day;
    }

    /**
     * Returns the year of the selected date.
     */
    public int getYear() {
        return _year;
    }

    /**
     * Returns the month of the selected date.
     */
    public int getMonth() {
        return _month;
    }

    /**
     * Returns the day of the selected date.
     */
    public int getDay() {
        return _day;
    }

    /**
     * Returns the Date associated with this event.
     * Interprets the selected year, month and day in the default time zone.
     * It is better for the client to interpret the time zone.
     * @param timeZone
     */
    public Date getDate(TimeZone timeZone) {
        Calendar c = CalendarUtil.newGregorianCalendarInstance(_year, _month, _day, timeZone);
        return c.getTime();
    }

    /** Returns a readable representation of the event. */
    public String toString() {
        return "CalendarSelectionEvent: " + CalendarUtil.getMonthName(_month) + " " + _day + ", " + _year;
    }

}
