//
// $Id: SemesterChart.java 4839 2004-07-13 21:38:58Z shane $
//
package edu.gemini.dbTools.semesterStatus;

import edu.gemini.dbTools.odbState.ProgramState;

import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.text.SimpleDateFormat;
import java.util.Date;

final class SemesterChart {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMdd");

    private static String _getDateString() {
        return DATE_FORMAT.format(new Date());
    }

    private static RenderedImage LEGEND;

    private static RenderedImage _getLegendImage() {
        if (LEGEND == null) {
            final Legend legend = new Legend();
            LEGEND = legend.create();
        }
        return LEGEND;
    }

    private final ProgramGroupId _group;
    private final ProgramState[] _progStateA;
    private final ChartAttrs _attrs;

    SemesterChart(final ProgramGroupId group, final ProgramState[] progStateA) {
        _group      = group;
        _progStateA = progStateA;
        _attrs      = new ChartAttrs(progStateA, _getLegendImage());
    }

    public RenderedImage create() {

        final Dimension2D imageDim = _attrs.getImageSize();
        final int w = (int) Math.round(imageDim.getWidth());
        final int h = (int) Math.round(imageDim.getHeight());
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        final Graphics2D gc = image.createGraphics();
        gc.setRenderingHints(Util.getRenderingHints());
        gc.setBackground(Color.white);
        gc.setPaint(Color.black);

        gc.clearRect(0, 0, w, h);

        _drawTitle(gc);

        _drawPercentageLines(gc);
        _drawBars(gc);

        _drawXTicks(gc);
        _drawXAxisLabel(gc);

        _drawYTicks(gc);
        _drawYAxisLabel(gc);

        _drawLegend(gc);

        return image;
    }

    private void _drawTitle(final Graphics2D gc) {
        // Save gc state
        final GraphicsState state = GraphicsState.save(gc);
        final Font font = ChartAttrs.TITLE_FONT;
        gc.setFont(font);

        final String label = _group.getSite() + " " + _group.getSemester() +
                       " on " + _getDateString();

        final Dimension2D dim = _attrs.getImageSize();

        final LineMetrics lm = Util.getLineMetrics(label, font);
        final Rectangle2D textR = Util.getStringBounds(label, font);

        final double textX = (dim.getWidth() - textR.getWidth())/2;
        final double textY = ChartAttrs.TOP_MARGIN + lm.getAscent();

        gc.drawString(label, (float) textX, (float) textY);

        // Restore gc state
        state.restore(gc);
    }

    private void _drawPercentageLines(final Graphics2D gc) {
        final Rectangle2D r = _attrs.getChartBounds();
        final double x = r.getX();
        double y = r.getY();
        final double w = r.getWidth();
        final double step = r.getHeight() * 0.1;

        final Line2D line = new Line2D.Double();
        for (int i=0; i<=10; ++i) {
            line.setLine(x, y, x+w, y);
            gc.draw(line);
            y += step;
        }
    }

    private void _drawBars(final Graphics2D gc) {
        double x = _attrs.getChartBounds().getX();
        for (final ProgramState pstate : _progStateA) {
            _drawBar(pstate, gc, x);
            x = x + ChartAttrs.BAR_WIDTH + ChartAttrs.BAR_GAP;
        }
    }

    private void _drawBar(final ProgramState pstate, final Graphics2D gc, final double x) {

        // Save gc state
        final GraphicsState state = GraphicsState.save(gc);

        final Rectangle2D r = _attrs.getChartBounds();
        double y = r.getY();
        final double h = r.getHeight();

        final ObsPercentage[] opA = ObsPercentage.getPercentages(pstate);

        final Rectangle2D rect = new Rectangle2D.Double();

        int runningTotal = 0;
        for (int i=opA.length-1; i>=0; --i) {
            final ObsPercentage op = opA[i];

            final double remainder = Math.max(0, h - runningTotal);
            double pixels = h * op.getPercentage();
            if (pixels > remainder) pixels = remainder;
            runningTotal += pixels;

            gc.setPaint(op.getPaint());
            rect.setFrame(x ,y, ChartAttrs.BAR_WIDTH, pixels);
            gc.fill(rect);

            gc.setPaint(Color.black);
            rect.setFrame(x, y, ChartAttrs.BAR_WIDTH, pixels);
            gc.draw(rect);
            y += pixels;
        }

        // Restore gc state
        state.restore(gc);
    }

    private void _drawXTicks(final Graphics2D gc) {
        // Save gc state
        final GraphicsState state = GraphicsState.save(gc);

        final Font font = ChartAttrs.TICK_LABEL_FONT;
        gc.setFont(font);

        final Rectangle2D r = _attrs.getChartBounds();
        double x = r.getX();
        final double h = r.getHeight();
        final double y = r.getY() + h;

        final double xinc = ChartAttrs.BAR_WIDTH + ChartAttrs.BAR_GAP;

        final Line2D tickLine = new Line2D.Double();

        for (int i=0; i<_progStateA.length; ++i) {
            tickLine.setLine(x, y, x, y+ChartAttrs.TICK_SIZE);
            gc.draw(tickLine);

            final int progIndex = i+1;
            if ((progIndex % 5) == 0) {
                final String label = String.valueOf(progIndex);

                final LineMetrics lm = Util.getLineMetrics(label, font);
                final double textY = y + ChartAttrs.TICK_SIZE +
                               ChartAttrs.X_AXIS_TICK_LABEL_GAP_HEIGHT +
                               lm.getAscent();

                final Rectangle2D textR = Util.getStringBounds(label, font);
                final double textX = x + xinc/2 - textR.getWidth()/2;

                gc.drawString(label, (float) textX, (float) textY);
            }

            x += xinc;
        }

        // Restore gc state
        state.restore(gc);
    }

    private void _drawXAxisLabel(final Graphics2D gc) {
        // Save gc state
        final GraphicsState state = GraphicsState.save(gc);
        final Font font = ChartAttrs.AXIS_LABEL_FONT;
        gc.setFont(font);

        final String label = "Program ID";

        // Get the width, height, and ascent of the Y axis label.
        final LineMetrics lm = Util.getLineMetrics(label, font);
        final Rectangle2D textR = Util.getStringBounds(label, font);
        final double textWidth  = textR.getWidth();
//        double textHeight = textR.getHeight();
        final double ascent = lm.getAscent();

        final Rectangle2D r = _attrs.getChartBounds();
//        Dimension2D d = _attrs.getImageSize();
        final double x = r.getX() + (r.getWidth() - textWidth)/2;
        final double y = r.getY() + r.getHeight() + ChartAttrs.TICK_SIZE +
                   ChartAttrs.X_AXIS_TICK_LABEL_GAP_HEIGHT +
                   _attrs.getXAxisTickLabelHeight() +
                   ChartAttrs.X_AXIS_LABEL_GAP;

        gc.drawString(label, (float)x, (float)(y+ascent));

        // Restore gc state
        state.restore(gc);
    }

    private void _drawYTicks(final Graphics2D gc) {
        // Save gc state
        final GraphicsState state = GraphicsState.save(gc);
        final Font font = ChartAttrs.TICK_LABEL_FONT;
        gc.setFont(font);

        final Rectangle2D r = _attrs.getChartBounds();
        final double x = r.getX() - ChartAttrs.TICK_SIZE;
        double y = r.getY();
        final double h = r.getHeight();

        final double yinc   = h * 0.1;
        final double bottom = y + h;

        final Line2D tickLine = new Line2D.Double();

        int percent = 100;
        for (int i=0; i<=10; ++i) {
            if (i==10) {
                y = bottom;
            }
            // Draw the tick mark itself.
            tickLine.setLine(x, y, x+ChartAttrs.TICK_SIZE, y);
            gc.draw(tickLine);

            // Draw the label.
            final String label = String.valueOf(percent) + "%";

            final LineMetrics lm = Util.getLineMetrics(label, font);
            final Rectangle2D textR = Util.getStringBounds(label, font);
            double textY = y - textR.getHeight()/2;
            textY += lm.getAscent();

            final double textX = x - ChartAttrs.Y_AXIS_TICK_LABEL_GAP_WIDTH - textR.getWidth();

            gc.drawString(label, (float) textX, (float) textY);

            y += yinc;
            percent -= 10;
        }

        // Restore gc state
        state.restore(gc);
    }

    private void _drawYAxisLabel(final Graphics2D gc) {
        // Save gc state
        final GraphicsState state = GraphicsState.save(gc);
        final Font font = ChartAttrs.AXIS_LABEL_FONT;
        gc.setFont(font);

        final String label = "Percentage of observations";

        // Get the width, height, and ascent of the Y axis label.
        final LineMetrics lm = Util.getLineMetrics(label, font);
        final Rectangle2D textR = Util.getStringBounds(label, font);
        final double textWidth  = textR.getHeight(); // vertical text
        final double textHeight = textR.getWidth();  // vertical text
        final double ascent = lm.getAscent();

        final Rectangle2D r = _attrs.getChartBounds();
        final double x = r.getX() - ChartAttrs.TICK_SIZE - ChartAttrs.Y_AXIS_TICK_LABEL_GAP_WIDTH -
                   _attrs.getYAxisTickLableWidth() - ChartAttrs.Y_AXIS_LABEL_GAP -
                   textWidth;
        final double h = r.getHeight();
        final double y = r.getY() + (h + textHeight)/2;

        gc.setTransform(AffineTransform.getRotateInstance(Math.toRadians(270),x+ascent, y));
        gc.drawString(label, (float)(x+ascent), (float)y);

        // Restore gc state
        state.restore(gc);
    }

    private void _drawLegend(final Graphics2D gc) {
        final RenderedImage legend = _getLegendImage();

        final Dimension2D imageSize = _attrs.getImageSize();
        final double x = (imageSize.getWidth() - legend.getWidth())/2;
        final double y = imageSize.getHeight() - ChartAttrs.BOTTOM_MARGIN -
                   legend.getHeight();

        gc.drawRenderedImage(legend, AffineTransform.getTranslateInstance(x, y));
    }
}
