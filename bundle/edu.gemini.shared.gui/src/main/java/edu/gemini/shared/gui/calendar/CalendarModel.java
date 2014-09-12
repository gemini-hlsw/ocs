package edu.gemini.shared.gui.calendar;

/**
 * The CalendarModel interface specifies the methods that
 * the CalendarWidget will use to interrogate the model.
 * The model conceptualy models a range of time between
 * two months (inclusive).  The start and end month could be the same.
 * Since this class is closely related to java.util.Calendar and
 * clients might be used to dealing with GregorianCalendar the month numbering
 * scheme will be 0-based to match Calendar.
 * e.g. January = 0.
 * Following the example of ListModel, properties are not bound.
 */

public interface CalendarModel {

    /**
     * Sets the span of time served by this model.
     * @param starYear year for start
     * @param startMonth 0-base month number for start
     * @param endYear year for end
     * @param endMonth 0-base month number for end
     */
    public void setRange(int startYear, int startMonth, int endYear, int endMonth);

    // These methods are not necessary, but insulate a client from having
    // to deal with the unfamiliar class MonthYear if they don't want to.
    /** Get year of beginning of time range. */
    public int getStartYear();

    /** Get month of beginning of time range. */
    public int getStartMonth();

    /** Get year of end of time range. */
    public int getEndYear();

    /** Get month of end of time range. */
    public int getEndMonth();

    /**
     * Gets the start of the time range served by the model.
     * The client is free to alter this retured object.
     */
    public MonthYear getStart();

    /**
     * Gets the end of the time range served by the model.
     * The client is free to alter this retured object.
     */
    public MonthYear getEnd();

    /**
     * Returns the number of months in the range of this model.
     */
    public int getSize();  // to follow naming scheme of ListModel

    /**
     * Add a listener to the calendar that's notified each time a change
     * to the data model occurs.
     */
    public void addCalendarDataListener(CalendarDataListener l);

    /**
     * Remove a listener from the calendar that's notified each time a change
     * to the data model occurs.
     */
    public void removeCalendarDataListener(CalendarDataListener l);

}

