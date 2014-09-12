//
// $Id: ChartAttrs.java 4841 2004-07-14 18:05:49Z shane $
//
package edu.gemini.dbTools.semesterStatus;

import edu.gemini.dbTools.odbState.ProgramState;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

final class ChartAttrs {

    /** Blank space at the left of the image. */
    private static final int LEFT_MARGIN   = 20;

    /** Blank space at the top of the image. */
    static final int TOP_MARGIN    = 10;

    /** Blank space at the right of the image. */
    private static final int RIGHT_MARGIN  = 10;

    /** Blank space at the bottom of the image. */
    static final int BOTTOM_MARGIN =  10;


    /** Gap between the title and the band arrows. */
    private static final int TITLE_GAP = 15;

    /** Height of the arrow indicating the science band. */
    private static final int BAND_ARROW_HEIGHT = 30;


    /** Width of each bar in the bar chart. */
    static final int BAR_WIDTH  = 7;

    /** Height of each bar in the bar chart. */
    private static final int BAR_HEIGHT = 400;

    /** Blank space between each bar in the bar chart. */
    static final int BAR_GAP    = 2;


    /** Gap between the X axis tick labels and the X axis label. */
    static final int X_AXIS_LABEL_GAP = 5;

    /** Gap between the Y axis label and the Y axis tick labels. */
    static final int Y_AXIS_LABEL_GAP = 5;

    /**
     * Length (or height) of each tick mark.  For the Y axis tick marks, this
     * is the length.  For the X axis, it is the height.
     */
    static final int TICK_SIZE = 4;

    /**
     * Vertical gap between the X axis tick label and the tick mark.
     * For example, the space between:
     *
     * <pre>  |
     *        5
     * </pre>
     */
    static final int X_AXIS_TICK_LABEL_GAP_HEIGHT = 5;

    /**
     * Horizontal gap between the Y axis tick label and the tick mark.
     * For example, the space between "100% -".
     */
    static final int Y_AXIS_TICK_LABEL_GAP_WIDTH  = 5;

    /**
     * Vertical gap between the X axis label "Program ID" and the start
     * of the legend itself.
     */
    private static final int LEGEND_GAP_HEIGHT = 20;

    /** Font to use to display the title of the chart. */
    static final Font TITLE_FONT      = new Font("Arial", Font.PLAIN, 20);

    /** Font to use to display the axis labels (both x and y). */
    static final Font AXIS_LABEL_FONT = new Font("Arial", Font.BOLD, 16);

    /** Font to use to display the tick labels (both x and y). */
    static final Font TICK_LABEL_FONT = new Font("Arial", Font.PLAIN, 12);

    private final double _xAxisTickLabelHeight;
    private final double _yAxisTickLabelWidth;

    private final Rectangle2D _chartBounds;
    private final Dimension2D _imageDim;

    ChartAttrs(final ProgramState[] progStateA, final RenderedImage legendImage) {
        Rectangle2D bounds;

        // Measure needed sizes required by various fonts.
        bounds = Util.getStringBounds("X", TITLE_FONT);
        final double _titleHeight = bounds.getHeight();

        bounds = Util.getStringBounds("X", AXIS_LABEL_FONT);
        final double _xAxisLabelHeight = bounds.getHeight();
        //noinspection UnnecessaryLocalVariable
        final double _yAxisLabelWidth = _xAxisLabelHeight;

        bounds = Util.getStringBounds("100%", TICK_LABEL_FONT);
        _xAxisTickLabelHeight = bounds.getHeight(); // not important what the text is: has the same height
        _yAxisTickLabelWidth  = bounds.getWidth();  // 100% is widest y tick label

        // Calculate the chart bounds.
        final double chartX = LEFT_MARGIN + _yAxisLabelWidth + Y_AXIS_LABEL_GAP +
                        _yAxisTickLabelWidth + Y_AXIS_TICK_LABEL_GAP_WIDTH +
                        TICK_SIZE;
        final double chartY = TOP_MARGIN + _titleHeight + TITLE_GAP +
                        BAND_ARROW_HEIGHT;

        final int progCount = progStateA.length;
        final double chartWidth = (progCount * (BAR_WIDTH + BAR_GAP)) - BAR_GAP;
        _chartBounds = new Rectangle2D.Double(chartX, chartY, chartWidth, BAR_HEIGHT);

        // Calculate the size of the image.
        double width  = _chartBounds.getX() + _chartBounds.getWidth() +
                         RIGHT_MARGIN;
        width = Math.max(width, (LEFT_MARGIN + legendImage.getWidth() + RIGHT_MARGIN));

        final double height = _chartBounds.getY() + _chartBounds.getHeight() +
                        TICK_SIZE + X_AXIS_TICK_LABEL_GAP_HEIGHT +
                        _xAxisTickLabelHeight + X_AXIS_LABEL_GAP +
                _xAxisLabelHeight +  LEGEND_GAP_HEIGHT +
                        legendImage.getHeight() + BOTTOM_MARGIN;

        _imageDim = new Dimension();
        _imageDim.setSize(width,height);
    }

    public double getXAxisTickLabelHeight() {
        return _xAxisTickLabelHeight;
    }

    public double getYAxisTickLableWidth() {
        return _yAxisTickLabelWidth;
    }

    public Rectangle2D getChartBounds() {
        return _chartBounds;
    }

    public Dimension2D getImageSize() {
        return _imageDim;
    }
}