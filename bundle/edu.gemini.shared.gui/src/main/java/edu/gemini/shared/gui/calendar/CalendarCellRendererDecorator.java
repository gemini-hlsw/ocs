package edu.gemini.shared.gui.calendar;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Decorator base class in pattern for decorating CalendarCellRenderer
 * This is a deviation from textbook Decorator pattern forced by
 * the Swing-style architecture.
 * Note here "component" is the name of the role in the pattern,
 * not a Java Component.
 * In textbook Decorator, the method(s) in the "component" interface
 * DO SOMETHING.  In Swing, they return
 * something that does something.  This extra layer requires a scramble.
 * In particular it requires this class to implement the paint() method
 * which is not in the "component" interface (CalendarCellRenderer interface)
 * but rather is in the Component interface of the object returned
 * by getCalendarCellRendererComponent().
 */
public class CalendarCellRendererDecorator extends JLabel implements CalendarCellRenderer {

    CalendarCellRenderer _component;  // name of the role in GoF Patterns

    Component _componentRenderer;

    // These values are passed to the getComponent() method prior to
    // painting a cell.
    protected Component parent;

    protected Object value;

    protected boolean even;

    protected boolean isSelected;

    protected boolean hasFocus;

    private class ResizeListener extends ComponentAdapter {

        public void componentResized(ComponentEvent e) {
            ((JComponent) _component).setPreferredSize(CalendarCellRendererDecorator.this.getSize());
        }
    }

    public CalendarCellRendererDecorator(CalendarCellRenderer component) {
        _component = component;
        // This listener will watch for resize events and recalculate
        // cached paint values.
        addComponentListener(new ResizeListener());
    }

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
    public Component getCalendarCellRendererComponent(Component parent, Object value, boolean even, boolean isSelected, boolean hasFocus) {
        // Store data needed for an upcoming paint() call.
        // Have to squirrel these away now because the paint() method has
        // a fixed signature.  Won't see these params again.
        this.parent = parent;
        this.value = value;
        this.even = even;
        this.isSelected = isSelected;
        this.hasFocus = hasFocus;

        // Must call this to configure the component for painting.
        // Store away the returned component.
        _componentRenderer = _component.getCalendarCellRendererComponent(parent, value, even, isSelected, hasFocus);
        return this;
    }

    /**
     * Here is a deviation from textbook Decorator pattern.
     * This method is not in the "component" interface.  Here "component"
     * is the name of the role in the pattern.
     */
    // Overriding paintComponent() does not give the desired result.
    public void paint(Graphics g) {
        _componentRenderer.paint(g);
    }

    /**
     * Returns the color used for calendar cells out of range.
     */
    public Color getBackdrop() {
        return _component.getBackdrop();
    }

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
    public void cachePaintValues(Graphics g, CalendarMonth calendarMonth) {
        _component.cachePaintValues(g, calendarMonth);
    }

}

