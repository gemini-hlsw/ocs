//
// $Id: Legend.java 4826 2004-07-09 18:47:39Z shane $
//
package edu.gemini.dbTools.semesterStatus;

import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

final class Legend {

    /** Font to use to display the legend. */
    private static final Font LEGEND_FONT     = new Font("Arial", Font.PLAIN, 14);

    private static final int LEFT_RIGHT_MARGIN = 20;
    private static final int TOP_BOTTOM_MARGIN =  5;

    private static final int ENTRY_X_GAP = 25;
    private static final int ENTRY_Y_GAP =  2;

    private static final int COLOR_SWATCH_SIDE = 9;
    private static final int COLOR_SWATCH_GAP  = 5;

    static final class LegendEntry {
        static LegendEntry[] getAll() {
            final int len = ObsStatusData.STATUS_ARRAY.length;

            final LegendEntry[] res = new LegendEntry[len];
            for (int i=0; i<len; ++i) {
                res[i] = new LegendEntry(ObsStatusData.STATUS_ARRAY[i]);
            }
            return res;
        }

        final ObsStatusData obsStatus;
        final Rectangle2D   stringBounds;
        final LineMetrics   lineMetrics;
        final String        label;

        final Rectangle2D   entryBounds = new Rectangle2D.Double();

        LegendEntry(final ObsStatusData osd) {
            obsStatus = osd;
            label     = "% obs " + osd.getLegend();

            stringBounds = Util.getStringBounds(label, LEGEND_FONT);
            lineMetrics  = Util.getLineMetrics(label, LEGEND_FONT);
        }
    }


    //private Dimension2D _imageDim;
    private final LegendEntry[][] _entryTable;

    Legend() {
        _entryTable = _createEntryTable();
    }

    public RenderedImage create() {

        // Figure out the image dimensions. The legend entries are already
        // positioned.  Just need to add the right margin to the end of the
        // last column and the bottom margin to the end of the last row.
        final int rows = _entryTable.length;
        final int cols = _entryTable[0].length;

        Rectangle2D bounds = _entryTable[0][cols-1].entryBounds;
        final double wd = bounds.getX() + bounds.getWidth() + LEFT_RIGHT_MARGIN;

        bounds = _entryTable[rows-1][0].entryBounds;
        final double hd = bounds.getY() + bounds.getHeight() + TOP_BOTTOM_MARGIN;

        final int w = (int) Math.round(wd);
        final int h = (int) Math.round(hd);
//        _imageDim = new Dimension(w, h);

        final BufferedImage image = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);

        final Graphics2D gc = image.createGraphics();
        gc.setRenderingHints(Util.getRenderingHints());
        gc.setBackground(Color.white);
        gc.setPaint(Color.black);
        gc.clearRect(0, 0, w, h);
        gc.drawRect(0, 0, w-1, h-1);

        for (final LegendEntry[] a_entryTable : _entryTable) {
            for (int col = 0; col < cols; ++col) {
                final LegendEntry entry = a_entryTable[col];
                if (entry == null) break;
                _drawLegendEntry(gc, entry);
            }
        }

        return image;
    }

    private static LegendEntry[][] _createEntryTable() {
        final LegendEntry[] entryA = LegendEntry.getAll();

        final int len  = entryA.length;
        final int rows = 2;
        final int cols = entryA.length/2 + entryA.length%2;

        int index = 0;
        final LegendEntry[][] entryTable = new LegendEntry[rows][cols];
        for (int row=0; row<rows; ++row) {
            for (int col=0; col<cols; ++col) {
                if (index >= len) break;
                final LegendEntry entry = entryA[index++];
                entryTable[row][col] = entry;
            }
        }

        _positionEntries(entryTable);
        return entryTable;
    }

    private static void _positionEntries(final LegendEntry[][] entryTable) {

        final int rows = entryTable.length;
        final int cols = entryTable[0].length;

        // Get the max height for each row and the max width for each column.
        final double[] maxRowHeight = new double[rows];
        final double[] maxColWidth  = new double[cols];
        for (int row=0; row<rows; ++row) {
            for (int col=0; col<cols; ++col) {
                final LegendEntry entry = entryTable[row][col];
                if (entry == null) break;

                final Rectangle2D r = entry.stringBounds;

                if (r.getHeight() > maxRowHeight[row]) {
                    maxRowHeight[row] = r.getHeight();
                }
                if (r.getWidth() > maxColWidth[col]) {
                    maxColWidth[col] = r.getWidth();
                }
            }
        }

        // Set the bounds of each entry.
        double y = TOP_BOTTOM_MARGIN;
        for (int row=0; row<rows; ++row) {
            if (row != 0) y += ENTRY_Y_GAP;
            final double labelH = maxRowHeight[row];

            double x = LEFT_RIGHT_MARGIN;
            for (int col=0; col<cols; ++col) {
                final LegendEntry entry = entryTable[row][col];
                if (entry == null) break;

                if (col != 0) x += ENTRY_X_GAP;
                final double labelW = maxColWidth[col];

                final double w = labelW + COLOR_SWATCH_SIDE + COLOR_SWATCH_GAP;
                entry.entryBounds.setFrame(x,y,w,labelH);

                x += w;
            }

            y += labelH;
        }
    }

    private static void _drawLegendEntry(final Graphics2D gc, final LegendEntry entry) {
        // Save gc state
        final GraphicsState state = GraphicsState.save(gc);
        final Font font = LEGEND_FONT;
        gc.setFont(font);

        final double ascent = entry.lineMetrics.getAscent();
        final Rectangle2D r = entry.entryBounds;

        // Draw the color swatch.
        double x = r.getX();
        double y = r.getY() + ascent - COLOR_SWATCH_SIDE;
        gc.setPaint(entry.obsStatus.getPaint());
        final Rectangle2D swatch = new Rectangle2D.Double();
        swatch.setFrame(x, y, COLOR_SWATCH_SIDE, COLOR_SWATCH_SIDE);
        gc.fill(swatch);
        gc.setPaint(Color.black);
        gc.draw(swatch);

        // Draw the label.
        x += COLOR_SWATCH_SIDE + COLOR_SWATCH_GAP;
        y  = r.getY();
        gc.drawString(entry.label, (float) x, (float) (y + ascent));

        // Restore gc state
        state.restore(gc);
    }
}
