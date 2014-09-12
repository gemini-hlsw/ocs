package edu.gemini.shared.gui.calendar;

/**
 * This event represents a change in the range of a Calendar.
 */
// Inspired by some of the weird events and listeners of JTree.

public class CalendarDisplayEvent extends java.util.EventObject {

    /**
     * Constructor
     * @param source - the RangeCalendar originating the event
     */
    public CalendarDisplayEvent(Object source) {
        super(source);
    }

    /** Returns a readable representation of the event. */
    public String toString() {
        return "CalendarDisplayEvent";
    }

}
