package edu.gemini.shared.gui.calendar;

import javax.swing.event.EventListenerList;

/**
 * This class provides default implementations for the methods of the
 * CalendarModel interface.
 */

public abstract class AbstractCalendarModel implements CalendarModel {

    private CalendarDataEvent _calendarDataEvent;

    protected EventListenerList listenerList = new EventListenerList();

    /**
     * Add a listener to the list that's notified each time a change
     * to the data model occurs.
     * @param l the CalendarDataListener
     */
    public void addCalendarDataListener(CalendarDataListener l) {
        listenerList.add(CalendarDataListener.class, l);
    }

    /**
     * Remove a listener from the list that's notified each time a
     * change to the data model occurs.
     * @param l the CalendarDataListener
     */
    public void removeCalendarDataListener(CalendarDataListener l) {
        listenerList.remove(CalendarDataListener.class, l);
    }

    /**
     * AbstractCalendarModel subclasses must call this method <b>after</b>
     * one or more elements of the range change.  The elements of the
     * new range are specified by the starting and ending year and month.
     *
     * @param source The CalendarModel that changed, typically "this".
     * @param startYear One end of the new range.
     * @param startMonth One end of the new range.
     * @param endYear The other end of the new range.
     * @param endMonth The other end of the new range.
     * @see EventListenerList
     * @see DefaultCalendarModel
     */
    protected void fireRangeChanged(Object source, int startYear, int startMonth, int endYear, int endMonth) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CalendarDataListener.class) {
                if (_calendarDataEvent == null) {
                    _calendarDataEvent = new CalendarDataEvent(this);
                }
                _calendarDataEvent.setStartYear(startYear);
                _calendarDataEvent.setStartMonth(startMonth);
                _calendarDataEvent.setEndYear(endYear);
                _calendarDataEvent.setEndMonth(endMonth);
                ((CalendarDataListener) listeners[i + 1]).rangeChanged(_calendarDataEvent);
            }
        }
    }

}
