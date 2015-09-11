// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: CalendarCellRenderer.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.gui.calendar;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Color;

/**
 * Defines the interface of a renderer for calendar cells.
 */
public interface CalendarCellRenderer {

    /**
     * This method is called on the renderer by the drawing calendar to
     * configure the renderer appropriately before drawing a cell.
     * Return the component used for drawing.
     * @param parent - the Component wishing to draw
     * @param value - Object indicating what to draw.
     * @param even - whether the month is even wrt start of model interval
     * @param isSelected - true if this date is selected
     * @param hasFocus - true if parent has focus
     * @return - a component capable of drawing a calendar cell in its
     * paint() method.
     */
    public abstract Component getCalendarCellRendererComponent(Component parent, Object value, boolean even, boolean isSelected, boolean hasFocus);

    /**
     * Returns the color used for calendar cells out of range.
     */
    public Color getBackdrop();

    /**
     * This method is called by the calendar once before a paint cycle
     * as a hint to cache values that apply to all cells of the calendar.
     * The calendar has already cached its information for the paint cycle.
     * If you are implementing this interface you can always just implement
     * this method with an empty body if you don't want to bother with it.
     * The call is purely for the benefit of the renderer to speed up rendering.
     * @param g - the Graphics context
     * @param parent - the CalendarMonth that wants to be painted.  The renderer
     * can query the calendar for any values it needs to paint cells.
     */
    public abstract void cachePaintValues(Graphics g, CalendarMonth calendarMonth);

}

