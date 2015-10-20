package edu.gemini.mask;

import edu.gemini.catalog.ui.tpe.CatalogDisplay;
import jsky.image.gui.ImageGraphicsHandler;
import jsky.image.gui.BasicImageDisplay;
import jsky.coords.CoordinateConverter;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.Shape;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.util.List;
import java.util.ArrayList;

import diva.util.java2d.Polygon2D;

import javax.swing.JComponent;

/**
 * Displays the mask related items as an image overlay.
 */
class MaskDisplay implements ImageGraphicsHandler {

    // graphics settings
    private static final Stroke DEFAULT_STROKE = new BasicStroke(1.0F);

    private static final Color GAP_COLOR = Color.blue;
    private static final Color SLIT_COLOR = Color.YELLOW;
    private static final Color BAND_COLOR = Color.RED;

    private static final Composite GAP_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50F);
    private static final Composite BAND_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50F);

    // Reference to main image display widget
    private CatalogDisplay _imageDisplay;

    // Usd to convert coordinates for display
    private CoordinateConverter _cc;

    // The mask table data
    private ObjectTable _table;

    // figures to display
    private List<MaskFigure> _gaps = new ArrayList<>();
    private List<MaskFigure> _slits = new ArrayList<>();
    private List<MaskFigure> _bands = new ArrayList<>();

    private boolean _showGaps;
    private boolean _showSlits;
    private boolean _showBands;

    // enable / disable the display
    private boolean _enabled = true;

    // Local class used to store information about a PWFS figure for later drawing.
    private static class MaskFigure {
        public Shape shape;
        public Color color;
        public Composite composite;

        public MaskFigure(Shape shape, Color color, Composite composite) {
            this.shape = shape;
            this.color = color;
            this.composite = composite;
        }
    }

    MaskDisplay(CatalogDisplay imageDisplay) {
        _imageDisplay = imageDisplay;
        _imageDisplay.addImageGraphicsHandler(this);
        _imageDisplay.addChangeListener(e -> updateFigures());
        _cc = _imageDisplay.getCoordinateConverter();
    }

    void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    void repaintImage() {
        ((JComponent)_imageDisplay).repaint();
    }

    void updateFigures() {
        updateBands();
        updateSlits();
        updateGaps(); // XXX doesn't always need to be recalculatedk
    }

    // Update the list of gap figures
    void updateGaps() {
        int binning = _table.getMaskParams().getBandDef().getBinning();
        double gapAx = 2067. / binning;
        double gapBx = 4153. / binning;
        double gapW = 42. / binning;
        double bottomy = 1.;

        double halfGapW = gapW / 2;
        double gapAx0 = gapAx - halfGapW;
        double gapAx1 = gapAx0 + gapW;
        double topy = _imageDisplay.getImageHeight();
        double gapBx0 = gapBx - halfGapW;
        double gapBx1 = gapBx0 + gapW;

        Polygon2D.Double gap1 = new Polygon2D.Double(gapAx0, bottomy);
        gap1.lineTo(gapAx0, topy);
        gap1.lineTo(gapAx1, topy);
        gap1.lineTo(gapAx1, bottomy);
        gap1.closePath();

        Polygon2D.Double gap2 = new Polygon2D.Double(gapBx0, bottomy);
        gap2.lineTo(gapBx0, topy);
        gap2.lineTo(gapBx1, topy);
        gap2.lineTo(gapBx1, bottomy);
        gap2.closePath();

        _gaps.clear();
        _gaps.add(new MaskFigure(gap1, GAP_COLOR, GAP_COMPOSITE));
        _gaps.add(new MaskFigure(gap2, GAP_COLOR, GAP_COMPOSITE));
    }

    void setShowGaps(boolean show) {
        _showGaps = show;
    }

    // Update the list of slit figures
    void updateSlits() {
        _slits.clear();

        // Add the boundary.
        _slits.add(_getBoundary());

        MaskParams maskParams = _table.getMaskParams();
        double specLen = maskParams.getSpecLen()/2.0;
        double pixelScale = maskParams.getPixelScale();

        int nobj = _table.getRowCount();
        for (int row = 0; row < nobj; row++) {

            // All columns (except x_ccd&y_ccd, specpos_*) are in arcsec
            // and so need to be converted to pixels by dividing by pixeelScale
            double x = _table.getXCcd(row) + _table.getSpecPosX(row);
            double y = _table.getYCcd(row) + _table.getSpecPosY(row);
            double xccd = _table.getXCcd(row) + _table.getSlitPosX(row) / pixelScale;
            double yccd = _table.getYCcd(row) + _table.getSlitPosY(row) / pixelScale;
            double dimx = _table.getSlitSizeX(row) / 2.0 / pixelScale;
            double dimy = _table.getSlitSizeY(row) / 2.0 / pixelScale;

            String priority = _table.getPriority(row);
            Color fg = (priority.equals("0") ? Color.cyan : Color.white);
            Color fg2 = SLIT_COLOR;

            double x1 = x + dimx;
            double y1 = y + dimy;
            double x2 = x + dimx;
            double y2 = y - dimy;
            double x3 = x - dimx;
            double y3 = y - dimy;
            double x4 = x - dimx;
            double y4 = y + dimy;
            double xx1 = x + specLen;
            double xx2 = x + specLen;
            double xx3 = x - specLen;
            double xx4 = x - specLen;

            // add spectrum rect
            double[] ar = new double[]{
                // spectrum far right line.
                xx1, y1, xx2, y2,

                // bottom line of spectrum
                xx3, y3,

                // spectrum near left line.
                xx4, y4,

                // spectrum top line
                xx1, y1,
            };
            Polygon2D.Double pg = new Polygon2D.Double(ar);
            _slits.add(new MaskFigure(pg, fg, null));


            // Draw slit right hand line.
            _slits.add(new MaskFigure(new Line2D.Double(x1, y1, x2, y2), fg, null));

            // Draw slit lef hand line.
            _slits.add(new MaskFigure(new Line2D.Double(x3, y3, x4, y4), fg, null));

            // Draw line from center of spectrum to the center of the slit.
            _slits.add(new MaskFigure( new Line2D.Double(x, y, xccd, yccd), fg2, null));

            // Draw line for the slit.
            y1 = yccd - dimy;
            y2 = yccd + dimy;
            _slits.add(new MaskFigure(new Line2D.Double(xccd, y1, xccd, y2), fg2, null));
        }
    }


    // Return a figure for the boundary of the FoV
    private MaskFigure _getBoundary() {
        double pixelScale = _table.getMaskParams().getPixelScale();

        GmCoords xVal = GmCoords.getX(pixelScale);
        GmCoords yVal = GmCoords.getY(pixelScale);

        double xstart = xVal.getStart();
        double xend = xVal.getEnd();
        double ystart = yVal.getStart();
        double yend = yVal.getEnd();

        // This chops :
        //      A( xstart,yend-ycorner2) to B(xstart+xcorner2,yend)
        //      C(xend-xcorner1,yend) to D(xend, yend-ycorner2)
        //      E(xstart,ystart+ycorner1 ) to F(xstart+xcorner2, ystart)
        //      G(xend-xcorner1, ystart) to H(xend, ystart+ycorner1)
        //
        //           XCor2         XCor1
        //            |              |
        //               B        C
        //  YCor2-     /           \
        //           A               D
        //
        //           E               H
        //  YCor1-     \           /
        //               F        G
        //
        //
        double ay = yend - yVal.getCorner2();
        double bx = xstart + xVal.getCorner2();
        double cx = xend - xVal.getCorner1();
        double hy = ystart + yVal.getCorner1();

        double[] ar = new double[]{
            // A( xstart,yend-ycorner2) to B(xstart+xcorner2,yend)
            xstart,
            ay,
            bx,
            yend,

            // B(xstart+xcorner2,yend) to C(xend-xcorner1,yend)
            bx,
            yend,
            cx,
            yend,

            // C(xend-xcorner1,yend) to D(xend, yend-ycorner2)
            cx,
            yend,
            xend,
            ay,

            // D(xend, yend-ycorner2) to H(xend, ystart+ycorner1)
            xend,
            ay,
            xend,
            hy,

            // H(xend, ystart+ycorner1) to G((xend-xcorner1, ystart)
            xend,
            hy,
            cx,
            ystart,

            // G((xend-xcorner1, ystart) to  F(xstart+xcorner2, ystart)
            cx,
            ystart,
            bx,
            ystart,

            // F(xstart+xcorner2, ystart) to  E(xstart,ystart+ycorner1 )
            bx,
            ystart,
            xstart,
            hy,

            //  E(xstart,ystart+ycorner1 ) to  A( xstart,yend-ycorner2)
            xstart,
            hy,
            xstart,
            ay,
        };

        Polygon2D.Double pg = new Polygon2D.Double(ar);
        return new MaskFigure(pg, Color.red, null);
  }

    void setShowSlits(boolean show) {
        _showSlits = show;
    }

    // Show the nod&shuffle bands and prohibited areas
    void updateBands() {
        _bands.clear();

        MaskParams maskParams = _table.getMaskParams();
        BandDef bandDef = maskParams.getBandDef();

        if (bandDef.getShuffleMode() == BandDef.BAND_SHUFFLE) {
            int imageWidth = _imageDisplay.getImageWidth();
            int imageHeight = _imageDisplay.getImageHeight();

            double prevY = 1;
            BandDef.Band[] bands = bandDef.getBands();
            for (BandDef.Band band : bands) {
                double y = band.getYPos();
                double bandHeight = band.getHeight();

                // outside (forbidden) area
                if (prevY < y) {
                    Polygon2D.Double pgOut = new Polygon2D.Double(new double[]{
                            1, prevY,
                            imageWidth, prevY,
                            imageWidth, y,
                            1, y
                    });
                    pgOut.closePath();
                    _bands.add(new MaskFigure(pgOut, BAND_COLOR, BAND_COMPOSITE));
                }

                // inside area
                Polygon2D.Double pgIn = new Polygon2D.Double(new double[]{
                        1, y,
                        imageWidth, y,
                        imageWidth, y + bandHeight,
                        1, y + bandHeight
                });
                _bands.add(new MaskFigure(pgIn, BAND_COLOR, null));
                prevY = y + bandHeight;
            }

            // Mark final outside (forbidden) area
            if (prevY < imageHeight) {
                Polygon2D.Double pgOut = new Polygon2D.Double(new double[]{
                    1, prevY,
                    imageWidth, prevY,
                    imageWidth, imageHeight,
                    1, imageHeight
                });
                pgOut.closePath();
                _bands.add(new MaskFigure(pgOut, BAND_COLOR, BAND_COMPOSITE));
            }
        }
    }

    void setShowBands(boolean show) {
        _showBands = show;
    }

    public void drawImageGraphics(BasicImageDisplay imageDisplay, Graphics2D g) {
        if (_enabled) {
            if (_showGaps) {
                _drawMaskFigures(_gaps, g);
            }
            if (_showSlits) {
                _drawMaskFigures(_slits, g);
            }
            if (_showBands) {
                _drawMaskFigures(_bands, g);
            }
        }
    }

    private void _drawMaskFigures(List<MaskFigure> items, Graphics2D g) {
        g.setStroke(DEFAULT_STROKE);
        for (MaskFigure fig: items) {
            g.setColor(fig.color);
            Shape shape = _imageToScreenCoords(fig.shape);
            g.draw(shape);
            if (fig.composite != null) {
                g.setComposite(fig.composite);
                g.fill(shape);
                g.setPaintMode();
            }
        }
    }

    private Shape _imageToScreenCoords(Shape shape) {
        if (shape instanceof Polygon2D.Double) {
            return _imageToScreenCoords((Polygon2D.Double)shape);
        } else if (shape instanceof Line2D.Double) {
            return _imageToScreenCoords((Line2D.Double)shape);
        }
        throw new RuntimeException("Unexpected mask figure shape: "
                + shape.getClass().getName());
    }

    private Line2D.Double _imageToScreenCoords(Line2D.Double line) {
        Point2D.Double p1 = (Point2D.Double)line.getP1();
        Point2D.Double p2 = (Point2D.Double)line.getP2();
        _cc.imageToScreenCoords(p1, false);
        _cc.imageToScreenCoords(p2, false);
        return new Line2D.Double(p1, p2);
    }

    private Polygon2D.Double _imageToScreenCoords(Polygon2D.Double pg) {
        int n = pg.getVertexCount();
        double[] ar = new double[n*2];
        Point2D.Double p = new Point2D.Double();
        for(int i = 0; i < n; i++) {
            p.x = pg.getX(i);
            p.y = pg.getY(i);
            _cc.imageToScreenCoords(p, false);
            ar[i*2] = p.x;
            ar[i*2+1] = p.y;
        }
        return new Polygon2D.Double(ar);
    }

    /**
     * double the current object table
     */
    public void setTable(ObjectTable table) {
        _table = table;
        updateFigures();
    }
}
