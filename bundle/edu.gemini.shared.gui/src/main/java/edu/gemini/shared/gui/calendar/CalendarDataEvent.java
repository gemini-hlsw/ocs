package edu.gemini.shared.gui.calendar;

/**
 * This event represents a change in the range of a Calendar.
 */

public class CalendarDataEvent extends java.util.EventObject {

    private int _startYear;

    private int _startMonth;

    private int _endYear;

    private int _endMonth;

    /**
     * Constructor
     * @param source - the RangeCalendar originating the event
     * @param startYear - year of start of range
     * @param startMonth - month of start of range
     * @param endYear - year of end of range
     * @param endMonth - month of end of range
     */
    public CalendarDataEvent(Object source, int startYear, int startMonth, int endYear, int endMonth) {
        super(source);
        _startYear = startYear;
        _startMonth = startMonth;
        _endYear = endYear;
        _endMonth = endMonth;
    }

    public CalendarDataEvent(Object source) {
        this(source, 0, 0, 0, 0);
    }

    /** Gets year of start of range. */
    public int getStartYear() {
        return _startYear;
    }

    /** Sets year of start of range. */
    public void setStartYear(int year) {
        _startYear = year;
    }

    /** Gets month of start of range. */
    public int getStartMonth() {
        return _startMonth;
    }

    /** Sets month of start of range. */
    public void setStartMonth(int month) {
        _startMonth = month;
    }

    /** Gets year of end of range. */
    public int getEndYear() {
        return _endYear;
    }

    /** Sets year of end of range. */
    public void setEndYear(int year) {
        _endYear = year;
    }

    /** Gets month of end of range. */
    public int getEndMonth() {
        return _endMonth;
    }

    /** Sets month of end of range. */
    public void setEndMonth(int month) {
        _endMonth = month;
    }

    /** Returns a readable representation of the event. */
    public String toString() {
        String s = "CalendarDataEvent: (" + _startYear + "," + _startMonth;
        s += ") - (" + _endYear + "," + _endMonth + ")";
        return s;
    }

}
