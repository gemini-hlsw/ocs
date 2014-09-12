package edu.gemini.shared.gui.calendar;

public interface CalendarDataListener extends java.util.EventListener {

    /**
     * Sent when the time range in a CalendarModel has changed.
     */
    public abstract void rangeChanged(CalendarDataEvent e);

}

