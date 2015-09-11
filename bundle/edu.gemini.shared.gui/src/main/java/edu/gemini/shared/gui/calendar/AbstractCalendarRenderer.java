// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: AbstractCalendarRenderer.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.gui.calendar;

import edu.gemini.shared.gui.ThinBorder;

import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.SystemColor;
import java.util.Calendar;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

/**
 * This class supplies some color-setting and alignment services used by
 * concrete renderers.
 */
public abstract class AbstractCalendarRenderer extends JLabel implements CalendarCellRenderer {

    // extra margins between border and rendered object
    private static final int DEFAULT_X_MARGIN = 3;

    private static final int DEFAULT_Y_MARGIN = 3;

    /** Months can have alternate background colors. */
    public static final Color DEFAULT_MULTI_MONTH_BACKGROUND1 = new Color(0xffffff);

    /** Months can have alternate background colors. */
    public static final Color DEFAULT_MULTI_MONTH_BACKGROUND2 = new Color(0xffffdd);

    /** Intended as default background for single-month calendars. */
    public static final Color DEFAULT_BACKGROUND = Color.lightGray;

    /** Intended as default background for selected cells w/out focus. */
    public static final Color DEFAULT_SELECTED_BACKGROUND = Color.lightGray;

    /** Intended as default background for selected cells with focus. */
    public static final Color DEFAULT_SELECTED_FOCUS_BACKGROUND = new Color(0x8888ff);

    /** Color used to render text. */
    public static final Color DEFAULT_FOREGROUND = Color.black;

    /** Color used to render text in selected cells w/out focus. */
    public static final Color DEFAULT_SELECTED_FOREGROUND = Color.black;

    /** Color used to render text in selected cells w/out focus. */
    public static final Color DEFAULT_SELECTED_FOCUS_FOREGROUND = Color.white;

    /** Color used for days not in months shown. */
    public static final Color DEFAULT_BACKDROP = null;

    /** A default borders around cells. */
    protected static final Border RAISED = new ThinBorder(ThinBorder.RAISED);

    /** A default borders around cells. */
    protected static final Border LOWERED = new ThinBorder(ThinBorder.LOWERED);

    /** The default size of rendered object as a fraction of the cell size. */
    protected static final double DEFAULT_X_FRACTION = .35;

    /** The default size of rendered object as a fraction of the cell size. */
    protected static final double DEFAULT_Y_FRACTION = .35;

    // These are settable properties
    // Many get their default values from GCalCalendar.
    private Color _background1 = DEFAULT_MULTI_MONTH_BACKGROUND1;

    private Color _background2 = DEFAULT_MULTI_MONTH_BACKGROUND2;

    private Color _backgroundSelected = DEFAULT_SELECTED_BACKGROUND;

    private Color _backgroundSelectedFocus = DEFAULT_SELECTED_FOCUS_BACKGROUND;

    private Color _foregroundSelected = DEFAULT_SELECTED_FOREGROUND;

    private Color _foregroundSelectedFocus = DEFAULT_SELECTED_FOCUS_FOREGROUND;

    private Color _backdrop = DEFAULT_BACKDROP;

    private Border _selectedBorder = LOWERED;

    private Border _unselectedBorder = RAISED;

    /** Default alignment */
    protected static final int DEFAULT_HOR_ALIGNMENT = SwingConstants.LEFT;

    /** Default alignment */
    protected static final int DEFAULT_VER_ALIGNMENT = SwingConstants.TOP;

    /** Default alignment */
    protected static final int DEFAULT_HOR_SYMBOL_ALIGNMENT = SwingConstants.RIGHT;

    /** Default alignment */
    protected static final int DEFAULT_VER_SYMBOL_ALIGNMENT = SwingConstants.TOP;

    private int _horAlignment = DEFAULT_HOR_ALIGNMENT;

    private int _verAlignment = DEFAULT_VER_ALIGNMENT;

    private double _horizontalFraction = DEFAULT_X_FRACTION;

    private double _verticalFraction = DEFAULT_Y_FRACTION;

    private int _horSymbolAlignment = DEFAULT_HOR_SYMBOL_ALIGNMENT;

    private int _verSymbolAlignment = DEFAULT_VER_SYMBOL_ALIGNMENT;

    private double _horizontalSymbolFraction = DEFAULT_X_FRACTION;

    private double _verticalSymbolFraction = DEFAULT_Y_FRACTION;

    private int _x_margin = DEFAULT_X_MARGIN;

    private int _y_margin = DEFAULT_Y_MARGIN;

    private int _fontStyle = Integer.MIN_VALUE;

    // These values are passed to the getComponent() method prior to
    // painting a cell.
    protected Component parent;

    protected Object value;

    protected boolean even;

    protected boolean isSelected;

    protected boolean hasFocus;

    public AbstractCalendarRenderer() {
        Color c = new Color(0xcccccc);  // approximates java gray
        setBackground(c);
        setBackground1(c);
        setBackground2(c);
        setOpaque(true);
        setForeground(DEFAULT_FOREGROUND);
    }

    /**
     * This method is called on the renderer by the drawing calendar to
     * configure the renderer appropriately before drawing a cell.
     * Returns the component used for drawing.
     * Concrete renderers can probably use this default implementation.
     * @param parent - the Object wishing to draw this cell
     * @param value - Object indicating what to draw
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
        setBorder(isSelected ? LOWERED : RAISED);
        return this;
    }

    /**
     * Overrides JComponent.setBackground to specify the background color
     * to use on on unselected cells, both even and odd months.
     */
    /*public void
       setBackground(Color c)
    {
       setBackground1(c);
       setBackground2(c);
       }*/

    /**
     * Get the background color to use on on unselected cells of even months.
     * This color is used by calendars rendering an even month
     * wrt the model range.
     */
    public Color getBackground1() {
        return _background1;
    }

    /**
     * Sets the background color to use on on unselected cells of even months.
     * This color is used by calendars rendering an even month
     * wrt the model range.
     */
    public void setBackground1(Color c) {
        _background1 = (c == null) ? null : new Color(c.getRGB());
    }

    /**
     * Get the background color to use on on unselected cells of odd months.
     * This color is used by calendars displaying multi-month mode and
     * rendering an odd month.
     */
    public Color getBackground2() {
        return _background2;
    }

    /**
     * Sets the background color to use on on unselected cells of odd months.
     * This color is used by calendars displaying multi-month mode and
     * rendering an odd month.
     */
    public void setBackground2(Color c) {
        _background2 = (c == null) ? null : new Color(c.getRGB());
    }

    /**
     * Get the background color to use on on selected cells
     * that do not have focus, both even/odd months.
     */
    public Color getSelectedBackground() {
        return _backgroundSelected;
    }

    /**
     * Specify the background color to use on on selected cells
     * that do not have focus, both even/odd months.
     */
    public void setSelectedBackground(Color c) {
        _backgroundSelected = (c == null) ? null : new Color(c.getRGB());
    }

    /**
     * Get the background color to use on on selected cells
     * that do have focus, both even/odd months.
     */
    public Color getSelectedFocusBackground() {
        return _backgroundSelectedFocus;
    }

    /**
     * Specify the background color to use on on selected cells
     * that do have focus, both even/odd months.
     */
    public void setSelectedFocusBackground(Color c) {
        _backgroundSelectedFocus = (c == null) ? null : new Color(c.getRGB());
    }

    /**
     * Gets color to use for text in selected cells.
     */
    public Color getSelectedForeground() {
        return _foregroundSelected;
    }

    /**
     * Sets color to use for text in selected cells.
     */
    public void setSelectedForeground(Color c) {
        _foregroundSelected = new Color(c.getRGB());
    }

    /**
     * Gets color to use for text in selected cells with focus.
     */
    public Color getSelectedFocusForeground() {
        return _foregroundSelectedFocus;
    }

    /**
     * Sets color to use for text in selected cells with focus.
     */
    public void setSelectedFocusForeground(Color c) {
        _foregroundSelectedFocus = new Color(c.getRGB());
    }

    /**
     * Returns the color used for calendar cells out of range.
     */
    public Color getBackdrop() {
        return _backdrop;
    }

    /**
     * Specify the background color to use on on unselected cells.
     * This color is used by calendars displaying multi-month mode and
     * rendering an odd month.
     */
    public void setBackdrop(Color c) {
        _backdrop = new Color(c.getRGB());
    }

    /**
     * Gets the border used for selected cells.
     */
    public Border getSelectedBorder() {
        return _selectedBorder;
    }

    /**
     * Sets the border used for selected cells.
     */
    public void setSelectedBorder(Border border) {
        _selectedBorder = border;
    }

    /**
     * Gets the border used for unselected cells.
     */
    public Border getUnselectedBorder() {
        return _unselectedBorder;
    }

    /**
     * Sets the border used for unselected cells.
     */
    public void setUnselectedBorder(Border border) {
        _unselectedBorder = border;
    }

    /**
     * Gets the fraction of cell width taken up by the rendered object.
     */
    public double getHorizontalFraction() {
        return _horizontalFraction;
    }

    /**
     * Sets the fraction of cell width taken up by the rendered object.
     */
    public void setHorizontalFraction(double d) {
        _horizontalFraction = d;
    }

    /**
     * Gets the fraction of cell height taken up by the rendered object.
     */
    public double getVerticalFraction() {
        return _verticalFraction;
    }

    /**
     * Sets the fraction of cell height taken up by the rendered object.
     */
    public void setVerticalFraction(double d) {
        _verticalFraction = d;
    }

    /**
     * Gets the horizontal alignment of rendered object.
     */
    public int getHorizontalAlignment() {
        return _horAlignment;
    }

    /**
     * Sets the horizontal alignment of rendered object.
     * @param alignment - one of SwingConstants.LEFT, RIGHT, CENTER
     */
    public void setHorizontalAlignment(int alignment) {
        _horAlignment = alignment;
    }

    /**
     * Gets the vertical alignment of rendered object.
     */
    public int getVerticalAlignment() {
        return _verAlignment;
    }

    /**
     * Sets the vertical alignment of rendered object.
     * @param alignment - one of SwingConstants.TOP, BOTTOM, CENTER
     */
    public void setVerticalAlignment(int alignment) {
        _verAlignment = alignment;
    }

    /**
     * Gets the fraction of cell width taken up by the rendered symbol.
     */
    public double getHorizontalSymbolFraction() {
        return _horizontalSymbolFraction;
    }

    /**
     * Sets the fraction of cell width taken up by the rendered symbol.
     */
    public void setHorizontalSymbolFraction(double d) {
        _horizontalSymbolFraction = d;
    }

    /**
     * Gets the fraction of cell height taken up by the rendered symbol.
     */
    public double getVerticalSymbolFraction() {
        return _verticalSymbolFraction;
    }

    /**
     * Sets the fraction of cell height taken up by the rendered symbol.
     */
    public void setVerticalSymbolFraction(double d) {
        _verticalSymbolFraction = d;
    }

    /**
     * Gets the horizontal alignment of rendered symbol.
     */
    public int getHorizontalSymbolAlignment() {
        return _horSymbolAlignment;
    }

    /**
     * Sets the horizontal alignment of rendered symbol.
     * @param alignment - one of SwingConstants.LEFT, RIGHT, CENTER
     */
    public void setHorizontalSymbolAlignment(int alignment) {
        _horSymbolAlignment = alignment;
    }

    /**
     * Gets the vertical alignment of rendered symbol.
     */
    public int getVerticalSymbolAlignment() {
        return _verSymbolAlignment;
    }

    /**
     * Sets the vertical alignment of rendered symbol.
     * @param alignment - one of SwingConstants.TOP, BOTTOM, CENTER
     */
    public void setVerticalSymbolAlignment(int alignment) {
        _verSymbolAlignment = alignment;
    }

    /**
     * Gets the margin between rendered object and border.
     */
    public int getX_Margin() {
        return _x_margin;
    }

    /**
     * Sets the margin between rendered object and border.
     */
    public void setX_Margin(int margin) {
        _x_margin = margin;
    }

    /**
     * Gets the margin between rendered object and border.
     */
    public int getY_Margin() {
        return _y_margin;
    }

    /**
     * Sets the margin between rendered object and border.
     */
    public void setY_Margin(int margin) {
        _y_margin = margin;
    }

    /**
     * If unset, the font style rendered will be the font this component
     * returns with getFont().  This is a convenient way to get the
     * style without dealing heavily in Font.
     */
    public int getFontStyle() {
        if (_fontStyle == Integer.MIN_VALUE) {
            return getFont().getStyle();
        } else {
            return _fontStyle;
        }
    }

    /**
     * If unset, the font style rendered will be the font this component
     * returns with getFont().  This is a convenient way to set the
     * style without dealing heavily in Font.
     */
    public void setFontStyle(int style) {
        _fontStyle = style;
    }

    /**
     * Swing documents suggest renderers override this method to
     * unconditionally return true.  So here it is.
     */
    public boolean isShowing() {
        return true;
    }
}

