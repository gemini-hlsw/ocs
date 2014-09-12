package edu.gemini.shared.gui.calendar;

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

import edu.gemini.shared.gui.ThinBorder;
import edu.gemini.shared.util.DiscreteRangeModel;

/**
 * Default implementation of the CalendarCellRender interface
 * suitable for range calendars.
 * This renderer assumes that the parent object is a RangeCalendar.
 */
public class DefaultRangeCellRenderer extends DefaultCalendarCellRenderer {

    public static final Color[] DEFAULT_RANGE_COLORS = {new Color(0xff2222), new Color(0x22ff22), new Color(0x2222ff)};

    public static final Color DEFAULT_SELECTED_RANGE_COLOR = Color.black;

    private static final double ARROW_WIDTH_FRACTION = 0.15;

    private static final double ARROW_HEIGHT_FRACTION = 0.10;

    private double _arrowWidthFraction = ARROW_WIDTH_FRACTION;

    private double _arrowHeightFraction = ARROW_HEIGHT_FRACTION;

    /** Color used to show range selections */
    private Color[] _rangeColors;

    private Color[] _rangeHighlightColors;

    private Color _selectedRangeColor = DEFAULT_SELECTED_RANGE_COLOR;

    private int _rangeCount;  // number of ranges

    private RangeCalendar _parent;

    // This is the pixel heights of range zone boundaries relative
    // to the bottom of a cell.  Example:
    //
    // +-----------+              e.g.  bndry 2 < y           -> no zone
    // |           |                    bndry 1 < y < bndry 2 -> zone 1
    // |- - - - - -| bndry 2            bndry 0 < y < bndry 1 -> zone 0
    // |  zone 1   |                              y < bndry 0 -> no zone
    // |- - - - - -| bndry 1
    // |  zone 0   |
    // |- - - - - -| bndry 0
    // +-----------+

    private int[] _rangeZoneHeights;  // ht. to draw line rel to cell bottom

    //protected RangeCalendar parent;  // cached in cachePaintValues()

    /**
     * Constructs a DefaultRangeCellRenderer.
     */
    public DefaultRangeCellRenderer(int rangeCount) {
        super();
        _rangeCount = rangeCount;
        _rangeColors = new Color[rangeCount];
        _rangeHighlightColors = new Color[rangeCount];
        _rangeColors = new Color[rangeCount];
        _rangeHighlightColors = new Color[rangeCount];
        for (int i = 0; i < rangeCount; ++i) {
            if (i < DEFAULT_RANGE_COLORS.length) {
                setRangeColor(i, DEFAULT_RANGE_COLORS[i]);
            } else {
                setRangeColor(i, Color.lightGray);
            }
        }
        _rangeZoneHeights = new int[getRangeCount()];
    }

    /**
     * Returns the number of ranges configured for this object.
     */
    public int getRangeCount() {
        return _rangeCount;
    }

    /**
     * Gets the color used to render the specified RangeCollection.
     */
    public Color getRangeColor(int range) {
        if (range < 0 || range >= getRangeCount())
            return null;
        return _rangeColors[range];
    }

    /**
     * Sets the color used to render the specified RangeCollection.
     * Note that this also sets the associated highlight color.
     * Client can override that with a call to setHighlightRangeColor()
     */
    public void setRangeColor(int range, Color c) {
        if (range < 0 || range >= 3)
            return;
        _rangeColors[range] = c;
        // highlight color will always be a brighter shade of range color
        _rangeHighlightColors[range] = new Color(c.brighter().getRGB());
    }

    /**
     * Gets the color used to render selected ranges in the
     * specified RangeCollection.
     * The same color is used for all range collections.
     */
    public Color getSelectedRangeColor() {
        return _selectedRangeColor;
    }

    /**
     * Sets the color used to render selected ranges in the
     * specified RangeCollection.
     * The same color is used for all range collections.
     */
    public void setSelectedRangeColor(Color c) {
        _selectedRangeColor = c;
    }

    /**
     * Gets the color used to render selections for the specified RangeCollection.
     */
    public Color getRangeHighlightColor(int range) {
        if (range < 0 || range >= _rangeCount)
            return null;
        return _rangeHighlightColors[range];
    }

    /**
     * Sets the color used to render the selections for specified RangeCollection.
     */
    public void setRangeHighlightColor(int range, Color c) {
        if (range < 0 || range >= getRangeCount())
            return;
        _rangeHighlightColors[range] = c;
    }

    /**
     * Gets the fraction of cell width taken up by range boundary marker.
     */
    public double getArrowWidthFraction() {
        return _arrowWidthFraction;
    }

    /**
     * Sets the fraction of cell width taken up by range boundary marker.
     */
    public void setArrowWidthFraction(double fraction) {
        _arrowWidthFraction = fraction;
    }

    /**
     * Gets the fraction of cell height taken up by range boundary marker.
     */
    public double getArrowHeightFraction() {
        return _arrowHeightFraction;
    }

    /**
     * Sets the fraction of cell height taken up by range boundary marker.
     */
    public void setArrowHeightFraction(double fraction) {
        _arrowHeightFraction = fraction;
    }

    /**
     * The client will call this with a translated Graphics object to
     * paint a cell of a calendar.
     */
    // Overriding paintComponent() does not give the desired result.
    public void paint(Graphics g) {
        super.paint(g);
        if (value == null)
            return;
        YearMonthDay ymd = (YearMonthDay) value;
        int x1, x2, y;
        /*x1 = parent.cellLeft(rowColumn);
          x2 = parent.cellRight(rowColumn);
          y1 = parent.cellTop(rowColumn);
          y2 = parent.cellBottom(rowColumn);*/
        x1 = 0;
        Dimension d = this.getSize();
        x2 = x1 + d.width - 1;
        Insets insets = this.getInsets();
        x1 += insets.left;
        x2 -= insets.right;
        boolean selectionEmpty = _parent.isRangeSelectionModelEmpty();
        boolean selectedForDeletion = false;
        for (int i = 0; i < getRangeCount(); ++i) {
            RangeCalendar.RangeInfo rinfo = _parent.rangeQuery(i, ymd);
            if (rinfo.inRange()) {
                selectedForDeletion = false;
                if (!selectionEmpty) {
                    DiscreteRangeModel selModel = _parent.getRangeSelectionModel(i);
                    if (selModel.contains(ymd)) {
                        g.setColor(getSelectedRangeColor());
                        selectedForDeletion = true;
                    }
                }
                if (!selectedForDeletion) {
                    // set regular color.
                    if (isSelected)
                        g.setColor(getRangeHighlightColor(i));
                    else
                        g.setColor(getRangeColor(i));
                }

                // draw range line
                y = _rangeZoneHeights[i];
                g.drawLine(x1, y, x2, y);
                if (rinfo.isRightEnd()) {
                    // draw right range boundary
                    drawRightRangeBoundary(g, x2, y);
                }
                if (rinfo.isLeftEnd()) {
                    // draw left range boundary
                    drawLeftRangeBoundary(g, x1, y);
                }
            }
        }
    }

    /**
     * Draws a symbol indicating right side of range interval.
     * Typically an arrow.
     * Client must make sure color is set first.
     */
    protected void drawRightRangeBoundary(Graphics g, int x0, int y0) {
        int[] x = new int[3];
        int[] y = new int[3];
        x[0] = x0;
        y[0] = y0;
        x[1] = x0 - (int) (_parent.cellWidth() * getArrowWidthFraction());
        y[1] = y0 + (int) (_parent.cellHeight() * getArrowHeightFraction());
        x[2] = x[1];
        y[2] = y0 - (int) (_parent.cellHeight() * getArrowHeightFraction());
        g.fillPolygon(x, y, 3);
    }

    /**
     * Draws a symbol indicating right side of range interval.
     * Typically an arrow.
     * Client must make sure color is set first.
     */
    protected void drawLeftRangeBoundary(Graphics g, int x0, int y0) {
        int[] x = new int[3];
        int[] y = new int[3];
        x[0] = x0;
        y[0] = y0;
        x[1] = x0 + (int) (_parent.cellWidth() * getArrowWidthFraction());
        y[1] = y0 + (int) (_parent.cellHeight() * getArrowHeightFraction());
        x[2] = x[1];
        y[2] = y0 - (int) (_parent.cellHeight() * getArrowHeightFraction());
        g.fillPolygon(x, y, 3);
    }

    /**
     * This method is called by the calendar once before a paint cycle
     * as a hint to cache values that apply to the whole calendar.
     * The calendar has already cached its information for the paint cycle.
     * @param g - the Graphics context
     * @param calendar - the calendar that wants to be painted.  The render
     * can query the calendar for any values it needs to paint cells.
     */
    public void cachePaintValues(Graphics g, CalendarMonth parent) {
        super.cachePaintValues(g, parent);
        _parent = (RangeCalendar) parent;

        // cache zone heights
        for (int i = 0; i < getRangeCount(); ++i) {
            _rangeZoneHeights[i] = (_parent.rangeZoneBoundary(i, false) + _parent.rangeZoneBoundary(i, true)) / 2;
        }
        Color c = ColorUtil.lightBackground(_rangeColors[_parent.getActiveRangeIndex()]);
        setSelectedBackground(c);
        setSelectedFocusBackground(c);
    }

}

