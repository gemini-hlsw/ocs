// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DefaultCalendarCellRenderer.java 4392 2004-01-30 06:40:18Z gillies $
//

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
import javax.swing.Icon;
import javax.swing.border.Border;

import edu.gemini.shared.gui.ThinBorder;

/**
 * Default implementation of the CalendarCellRender interface.
 * This implementation has the capability to draw cell backgrounds,
 * date numbers, and selection state (represented in background color).
 */
public class DefaultCalendarCellRenderer extends AbstractCalendarRenderer implements CalendarCellRenderer {

    // fonts
    private static final int MIN_FONT_SIZE = 8;

    public static final String DEFAULT_FONT_NAME = "SansSerif";

    public static final int DEFAULT_FONT_STYLE = Font.PLAIN;

    private static final String FONT_TEST_STRING = "22";

    private static final int DEFAULT_HOR_DATE_ALIGNMENT = SwingConstants.LEFT;

    private static final int DEFAULT_VER_DATE_ALIGNMENT = SwingConstants.TOP;

    private int _horDateTextAlignment = DEFAULT_HOR_DATE_ALIGNMENT;

    private int _verDateTextAlignment = DEFAULT_VER_DATE_ALIGNMENT;

    // This renderer could be associated with a DateSymbolSupplier that
    // can draw symbols on certain days.
    private DateSymbolSupplier _dateSymbolSupplier = null;

    // FontMetrics uses some average width of characters to calculate
    // stringWidth() instead of using the actual characters in the string.
    // It turns out that when drawing numbers, this is juat a pixel
    // too large.  This small correction is actually visible.
    private static final int MAGIC_TEXT_X_OFFSET = 1;

    // This class stores values that are cached once before a paint cycle.
    protected class PaintValues {

        // These values are cached prior to paint.
        Font font;

        FontMetrics fontMetrics;

        int x_offset;  // location to paint text

        int y_offset;  // location to paint text

        int x_ul;  // MBR of date text

        int y_ul;  // MBR of date text

        int x_ul_ds;  // MBR of DateSymbol Icon

        int y_ul_ds;  // MBR of DateSymbol Icon

        Dimension MBR;

        Dimension cellDimension;

        MonthYear modelStart;  // start of model interval

        CalendarMonth parent;
    }

    protected PaintValues paintValues = new PaintValues();

    /**
     *
     */
    public DefaultCalendarCellRenderer() {
    }

    /**
     * Sets the DateSymbolSupplier.
     */
    public void setDateSymbolSupplier(DateSymbolSupplier d) {
        _dateSymbolSupplier = d;
    }

    /**
     * Gets the DateSymbolSupplier.
     */
    public DateSymbolSupplier getDateSymbolSupplier() {
        return _dateSymbolSupplier;
    }

    /**
     * Gets the horizontal alignment of date text within the bounding box
     * allotted for the date.
     */
    public int getHorizontalDateTextAlignment() {
        return _horDateTextAlignment;
    }

    /**
     * Sets the horizontal alignment of date text within the bounding box
     * allotted for the date.
     * @param alignment - one of SwingConstants.LEFT, RIGHT, CENTER
     */
    public void setHorizontalDateTextAlignment(int alignment) {
        _horDateTextAlignment = alignment;
    }

    /**
     * The client will call this with a translated Graphics object to
     * paint a cell of a calendar.
     */
    // Overriding paintComponent() does not give the desired result.
    public void paint(Graphics g) {
        // value is passed by the calendar in the superclass method
        // getCalendarCellRendererComponent() prior to each cell rendering.
        // value should be an instance of YearMonthDay for the cell to draw
        if (value == null) {
            // this means this cell was out of range, paint backdrop
            Color color = getBackdrop();
            if (color != null) {
                //this.setBackground(getBackdrop());
                //super.paint(g);
                //System.out.println("Painting null value");
            }
            return;
        }
        if (!(value instanceof YearMonthDay))
            return;  // this would be odd
        // We have an actual date to paint.
        YearMonthDay ymd = (YearMonthDay) value;
        setBorder(isSelected ? getSelectedBorder() : getUnselectedBorder());
        Insets insets = this.getInsets();
        int x1, x2, y1, y2;
        RowColumn rowColumn = ((CalendarMonth) parent).dayToCell(ymd);
        /*x1 = parent.cellLeft(rowColumn);
        x2 = parent.cellRight(rowColumn);
        y1 = parent.cellTop(rowColumn);
        y2 = parent.cellBottom(rowColumn);*/
        x1 = 0;
        y1 = 0;
        Dimension d = this.getSize();
        x2 = x1 + d.width - 1;
        y2 = y1 + d.height - 1;
        x1 += insets.left;
        y1 += insets.top;
        x2 -= insets.right;
        y2 -= insets.bottom;
        if (isOpaque()) {
            Color background = null;
            Color foreground = null;
            if (isSelected) {
                background = hasFocus ? getSelectedFocusBackground() : getSelectedBackground();
                foreground = hasFocus ? getSelectedFocusForeground() : getSelectedForeground();
            } else if (paintValues.parent.getDisplayMode() == CalendarMonth.SINGLE_MONTH_MODE) {
                background = getBackground();
            } else if (even) {
                background = getBackground1();
            } else {
                background = getBackground2();
            }
            if (background != null) {
                //setBackground(background);
                //super.paint(g);
                paintBorder(g);
                // why take off an extra pixel?  I don't know, but it works
                g.setColor(background);
                /*x1 = getLocation().x; // xxxx test
                y1 = getLocation().y;
                x2 = x1 + getSize().width;
                y2 = y1 + getSize().height;*/
                g.fillRect(x1 + 1, y1 + 1, x2 - x1 - 1, y2 - y1 - 1);
            } else {
                super.paint(g);
                System.out.println("null background");
            }
        }
        // translated origin is upper left corner of cell
        // x1, y1, x2, y2 define the client rectangle - the cell rectangle
        // minus insets
        paintDayNumber(g, ymd, x1, y1, x2, y2);
        if (getDateSymbolSupplier() != null)
            paintDateSymbol(g, ymd, x1, y1, x2, y2);
    }

    // This paints a single date number using values that hopefully were cached
    // in the previous call to getCalendarCellRendererComponent().
    protected void paintDayNumber(Graphics g, YearMonthDay ymd, int x1, int y1, int x2, int y2) {
        if (ymd == null)
            return;
        // praint nothing for cells outside of displayed months
        //if (!calendar.dateInRange(d)) return;

        int day = ymd.day;
        int x;
        String s = String.valueOf(day);
        if (_horDateTextAlignment == SwingConstants.RIGHT) {
            int stringWidth = paintValues.fontMetrics.stringWidth(s);
            x = paintValues.x_ul + (paintValues.MBR.width - stringWidth);
            x -= 2;  // this is a hack to make right-adjustment look better
        } else if (_horDateTextAlignment == SwingConstants.CENTER) {
            int stringWidth = paintValues.fontMetrics.stringWidth(s);
            x = paintValues.x_ul + (paintValues.MBR.width - stringWidth) / 2;
        } else {// if (_horDateTextAlignment == SwingConstants.LEFT){
            x = paintValues.x_ul;
        }
        g.setColor(getForeground());
        g.setFont(paintValues.font);
        g.drawString("" + day, x1 + x + MAGIC_TEXT_X_OFFSET, y1 + paintValues.y_offset);
        // For debugging, it is helpful to draw the MBR.
        /*CalendarMonth parent = (CalendarMonth)this.parent;
        RowColumn rowColumn = parent.dayToCell((Calendar)value);
        int xx = parent.cellLeft(rowColumn);
        int yy = parent.cellTop(rowColumn);
        System.out.println("Cell corner: " + xx + ", " + yy);
        xx += paintValues.x_ul;
        yy += paintValues.y_ul;
        Dimension MBR = paintValues.MBR;
        System.out.println("painting rectangle at " + xx + ", " + yy +
                           " size: " + MBR);
        g.setColor(Color.red);
        g.drawRect(xx, yy, MBR.width, MBR.height);*/
    }

    // This paints a single date number using values that hopefully were cached
    // in the previous call to getCalendarCellRendererComponent().
    protected void paintDateSymbol(Graphics g, YearMonthDay ymd, int x1, int y1, int x2, int y2) {
        if (ymd == null)
            return;
        // praint nothing for cells outside of displayed months
        //if (!calendar.dateInRange(d)) return;

        Icon icon = _dateSymbolSupplier.getDateSymbolIcon(ymd.year, ymd.month, ymd.day);
        if (icon == null)
            return;
        g.setColor(getForeground());
        icon.paintIcon(this, g, x1 + paintValues.x_ul_ds, y1 + paintValues.y_ul_ds);

        // For debugging, it is helpful to draw the MBR.
        /*CalendarMonth parent = (CalendarMonth)this.parent;
        RowColumn rowColumn = parent.dayToCell((Calendar)value);
        int xx = parent.cellLeft(rowColumn);
        int yy = parent.cellTop(rowColumn);
        System.out.println("Cell corner: " + xx + ", " + yy);
        xx += paintValues.x_ul;
        yy += paintValues.y_ul;
        Dimension MBR = paintValues.MBR;
        System.out.println("painting rectangle at " + xx + ", " + yy +
                           " size: " + MBR);
        g.setColor(Color.red);
        g.drawRect(xx, yy, MBR.width, MBR.height);*/
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
        int heightCell = parent.cellHeight();
        int widthCell = parent.cellWidth();
        paintValues.cellDimension = new Dimension(heightCell, widthCell);
        paintValues.parent = parent;

        // Calculate the size of the box that text will go in.
        // Max size is cell size.
        Insets insets = getInsets();
        int widthMBR = (int) (widthCell * getHorizontalFraction());
        int heightMBR = (int) (heightCell * getVerticalFraction());
        int size;
        size = widthCell - 2 * getX_Margin();  // max size allowed
        if (size < 0)
            size = 0;
        if (widthMBR > size)
            widthMBR = size;
        size = heightCell - 2 * getY_Margin();  // max size allowed
        if (size < 0)
            size = 0;
        if (heightMBR > size)
            heightMBR = size;
        paintValues.MBR = new Dimension(widthMBR, heightMBR);
        // We will calculate largest font that will fit in this box.
        // MBR might have silly aspect ratio.  We would like the MBR
        // to fit a two digit number snugly.
        // Need to calculate the font size first, then shrink the box
        // to fit the numbers exactly.

        // This object is a JComponent, someone could have set the font.
        // We will use the name and style of the font that was set, but we must
        // calculate the size to fit the box.
        Font font = getFont();
        size = TextUtil.fontSizeForDimension(g, FONT_TEST_STRING, paintValues.MBR, font.getName(), font.getStyle());
        if (size < MIN_FONT_SIZE)
            size++;
        if (size < MIN_FONT_SIZE - 1)
            size++;
        if (size < MIN_FONT_SIZE - 1)
            size++;
        int fontStyle = (getFontStyle() == Integer.MIN_VALUE) ? font.getStyle() : getFontStyle();
        paintValues.font = new Font(font.getName(), fontStyle, size);
        g.setFont(paintValues.font);
        paintValues.fontMetrics = g.getFontMetrics();

        // Now that we know how big the font is, we can shrink the MBR
        // to fit the widest numbers.
        paintValues.MBR.width = paintValues.fontMetrics.stringWidth(FONT_TEST_STRING);
        // These are all numbers so ignore the descent.
        paintValues.MBR.height = paintValues.fontMetrics.getAscent();
        if (getHorizontalAlignment() == SwingConstants.RIGHT) {
            paintValues.x_ul = widthCell - getX_Margin() - paintValues.MBR.width;
        } else if (getHorizontalAlignment() == SwingConstants.CENTER) {
            paintValues.x_ul = (widthCell - paintValues.MBR.width) / 2;
        } else // if (getHorizontalAlignment() == SwingConstants.LEFT)
        {
            paintValues.x_ul = getX_Margin();
        }

        // Remember these are all numbers with no descent.
        if (getVerticalAlignment() == SwingConstants.BOTTOM) {
            paintValues.y_ul = heightCell - getY_Margin() - paintValues.MBR.height;
        } else if (getVerticalAlignment() == SwingConstants.CENTER) {
            paintValues.y_ul = (heightCell - paintValues.MBR.height) / 2;
        } else // if (getVerticalAlignment() == SwingConstants.TOP)
        {
            paintValues.y_ul = getY_Margin();
        }
        paintValues.y_offset = paintValues.y_ul + paintValues.fontMetrics.getAscent();
        paintValues.modelStart = new MonthYear(parent.getModel().getStartYear(), parent.getModel().getStartMonth());
        if (_dateSymbolSupplier == null)
            return;

        // Calculate the size of the box that DateSymbol Icon will go in.
        // Max size is cell size.
        widthMBR = (int) (widthCell * getHorizontalSymbolFraction());
        heightMBR = (int) (heightCell * getVerticalSymbolFraction());
        size = widthCell - 2 * getX_Margin();  // max size allowed
        if (size < 0)
            size = 0;
        if (widthMBR > size)
            widthMBR = size;
        size = heightCell - 2 * getY_Margin();  // max size allowed
        if (size < 0)
            size = 0;
        if (heightMBR > size)
            heightMBR = size;
        if (getHorizontalSymbolAlignment() == SwingConstants.RIGHT) {
            paintValues.x_ul_ds = widthCell - getX_Margin() - insets.right - widthMBR;
        } else if (getHorizontalSymbolAlignment() == SwingConstants.CENTER) {
            paintValues.x_ul_ds = (widthCell - widthMBR) / 2;
        } else // if (getHorizontalSymbolAlignment() == SwingConstants.LEFT)
        {
            paintValues.x_ul_ds = getX_Margin();
        }
        if (getVerticalSymbolAlignment() == SwingConstants.BOTTOM) {
            paintValues.y_ul_ds = heightCell - getY_Margin() - heightMBR;
        } else if (getVerticalSymbolAlignment() == SwingConstants.CENTER) {
            paintValues.y_ul_ds = (heightCell - heightMBR) / 2;
        } else // if (getVerticalSymbolAlignment() == SwingConstants.TOP)
        {
            paintValues.y_ul_ds = getY_Margin();
        }
        _dateSymbolSupplier.cachePaintValues(g, widthMBR, heightMBR);
    }

}

