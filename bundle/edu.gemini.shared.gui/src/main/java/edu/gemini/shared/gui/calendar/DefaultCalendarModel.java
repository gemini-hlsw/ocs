package edu.gemini.shared.gui.calendar;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.TimeZone;

import edu.gemini.shared.util.CalendarUtil;

/**
 * This class provides default implementations for the methods of the
 * CalendarModel interface.
 */

public class DefaultCalendarModel extends AbstractCalendarModel {

    // This is the time range served by this model.
    private MonthYear _start;

    private MonthYear _end;

    /**
     * Constructs a model with the specified start and end months.
     * @param startYear - full 4-digit year
     * @param startMonth - one-based month number (e.g. March = 3)
     * @param endYear - full 4-digit year
     * @param endMonth - one-based month number (e.g. March = 3)
     */
    public DefaultCalendarModel(int startYear, int startMonth, int endYear, int endMonth) {
        setRange(startYear, startMonth, endYear, endMonth);
    }

    /**
     * Constructs a model serving the one month period containing current date.
     */
    public DefaultCalendarModel() {
        this(new GregorianCalendar());
    }

    /**
     * Constructs a model serving the one month period containing specified date.
     */
    public DefaultCalendarModel(Date d) {
        this(CalendarUtil.newGregorianCalendarInstance(d));
    }

    /**
     * Constructs a model serving the one month period containing specified date.
     */
    public DefaultCalendarModel(Date d, TimeZone tz) {
        this(CalendarUtil.newGregorianCalendarInstance(d, tz));
    }

    /**
     * Constructs a model serving the one month period containing specified date.
     */
    public DefaultCalendarModel(Calendar c) {
        this(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.YEAR), c.get(Calendar.MONTH));
    }

    /**
     * Sets the span of time served by this model.
     */
    public void setRange(int startYear, int startMonth, int endYear, int endMonth) {
        _start = new MonthYear(startYear, startMonth);
        _end = new MonthYear(endYear, endMonth);

        // make sure they are in order
        if (_start.after(_end)) {
            MonthYear temp = _start;
            _start = _end;
            _end = temp;
        }
        fireRangeChanged(this, getStartYear(), getStartMonth(), getEndYear(), getEndMonth());
    }

    // These methods are not necessary, but insulate a client from having
    // to deal with the unfamiliar class MonthYear if they don't want to.
    /** Get year of beginning of time range. */
    public int getStartYear() {
        return _start.year;
    }

    /** Get month of beginning of time range. */
    public int getStartMonth() {
        return _start.month;
    }

    /** Get year of end of time range. */
    public int getEndYear() {
        return _end.year;
    }

    /** Get month of end of time range. */
    public int getEndMonth() {
        return _end.month;
    }

    /**
     * Gets the start of the time range served by the model.
     * The client is free to alter this retured object.
     */
    public MonthYear getStart() {
        return new MonthYear(_start);
    }

    /**
     * Gets the end of the time range served by the model.
     * The client is free to alter this retured object.
     */
    public MonthYear getEnd() {
        return new MonthYear(_end);
    }

    /**
     * Returns the number of months in the range of this model.
     */
    public int getSize() {
        return MonthYear.difference(_end, _start).getMonths() + 1;
    }

    public String toString() {
        String s = "DefaultCalendarModel ";
        s += getStart().toString() + " to " + getEnd().toString();
        return s;
    }

}
