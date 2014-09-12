package edu.gemini.shared.gui.calendar;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;

public class DefaultCalendarHeaderRenderer extends AbstractCalendarRenderer implements CalendarCellRenderer {

    /** Default background color for this component */
    public static final Color DEFAULT_HEADER_BACKGROUND = new Color(0xaaaaaa);

    protected static final double DEFAULT_HEADER_X_FRACTION = .7;

    protected static final int DOW_FONT_STYLE = Font.BOLD;

    protected static final int MIN_DOW_FONT_SIZE = 8;

    // This class stores values that are cached once before a paint cycle.
    protected class PaintValues {

        // These values are cached prior to paint.
        Font font;

        FontMetrics fontMetrics;

        int x_offset;  // location to paint text

        int y_offset;  // location to paint text

        int x_ul;  // MBR of text

        int y_ul;  // MBR of text

        boolean singleLetter;  // indicates 1 or 3 letter abbrev of week day

        Dimension MBR;

        Dimension cellDimension;
    }

    protected PaintValues paintValues = new PaintValues();

    private static final String FONT_TEST_STRING = "Wed";

    private static final int MIN_FONT_SIZE = 6;

    public DefaultCalendarHeaderRenderer() {
        setOpaque(true);
        ((JLabel) this).setBackground(DEFAULT_HEADER_BACKGROUND);
        setVerticalAlignment(JLabel.TOP);
        setHorizontalAlignment(JLabel.CENTER);
        Font font = getFont();
        //font.setStyle(DOW_FONT_STYLE);
        setText("Wed");  // just to give it a size
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
        Component render = super.getCalendarCellRendererComponent(parent, value, even, isSelected, hasFocus);
        setText(value.toString());
        return render;
    }

    public Color getBackdrop() {
        return Color.lightGray;
    }

    /**
     * This method is called by the calendar once before a paint cycle
     * as a hint to cache values that apply to the whole calendar.
     * The calendar has already cached column width information for the
     * paint cycle, but don't count on anything else.
     * @param g - the Graphics context
     * @param calendar - the calendar that wants to be painted.  The render
     * can query the calendar for any values it needs to paint cells.
     */
    public void cachePaintValues(Graphics g, CalendarMonth parent) {
        int heightCell = parent.cellHeight();
        int widthCell = parent.cellWidth();
        paintValues.cellDimension = new Dimension(heightCell, widthCell);

        // Calculate the size of the box that text will go in.
        // Max size is cell size.

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
        size = TextUtil.fontSizeForDimension(g, "Wed", paintValues.MBR, font.getName(), font.getStyle());
        if (size < MIN_DOW_FONT_SIZE) {
            paintValues.singleLetter = true;
            size = TextUtil.fontSizeForDimension(g, "W", paintValues.MBR, font.getName(), font.getStyle());
            // Now that we know how big the font is, we can shrink the MBR
            // to fit the widest numbers.
            paintValues.MBR.width = paintValues.fontMetrics.stringWidth("W");
        } else {
            paintValues.singleLetter = false;
            // Now that we know how big the font is, we can shrink the MBR
            // to fit the widest numbers.
            paintValues.MBR.width = paintValues.fontMetrics.stringWidth("Wed");
        }
        paintValues.font = new Font(font.getName(), font.getStyle(), size);
        g.setFont(paintValues.font);
        paintValues.fontMetrics = g.getFontMetrics();

        // These are all letters with no descent.
        paintValues.MBR.height = paintValues.fontMetrics.getAscent();
        if (getHorizontalAlignment() == SwingConstants.RIGHT) {
            paintValues.x_ul = widthCell - getX_Margin() - paintValues.MBR.width;
        } else if (getHorizontalAlignment() == SwingConstants.CENTER) {
            paintValues.x_ul = (widthCell - paintValues.MBR.width) / 2;
        } else // if (getHorizontalAlignment() == SwingConstants.LEFT)
        {
            paintValues.x_ul = getX_Margin();
        }

        // Remember these are all letters with no descent.
        if (getVerticalAlignment() == SwingConstants.BOTTOM) {
            paintValues.y_ul = heightCell - getY_Margin() - paintValues.MBR.height;
        } else if (getVerticalAlignment() == SwingConstants.CENTER) {
            paintValues.y_ul = (heightCell - paintValues.MBR.height) / 2;
        } else // if (getVerticalAlignment() == SwingConstants.TOP)
        {
            paintValues.y_ul = getY_Margin();
        }
        paintValues.y_offset = paintValues.y_ul + paintValues.fontMetrics.getAscent();
    }

}
