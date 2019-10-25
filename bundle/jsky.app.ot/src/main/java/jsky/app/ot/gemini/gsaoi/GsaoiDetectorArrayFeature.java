package jsky.app.ot.gemini.gsaoi;

import edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray;
import static edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray.Quadrant;

import edu.gemini.spModel.telescope.IssPort;
import jsky.app.ot.gemini.inst.SciAreaFeatureBase;
import jsky.app.ot.tpe.TpeImageInfo;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Draws the Science Area, the detector or slit.
 */
public final class GsaoiDetectorArrayFeature extends SciAreaFeatureBase {
    private AffineTransform trans;

    @Override
    protected Point2D.Double _getTickMarkOffset() {
        AffineTransform trans = new AffineTransform();
        trans.translate(_baseScreenPos.x, _baseScreenPos.y);
        trans.scale(_tii.getPixelsPerArcsec(), _tii.getPixelsPerArcsec());

        Point2D.Double p;
        p = new Point2D.Double(0.0,
                - (GsaoiDetectorArray.DETECTOR_GAP_ARCSEC + GsaoiDetectorArray.DETECTOR_SIZE_ARCSEC)
                        + GsaoiDetectorArray.ODGW_HOTSPOT_OFFSET_Q);
        return (Point2D.Double) trans.transform(p, p);
    }

    @Override
    protected boolean _calc(TpeImageInfo tii)  {
        if (!super._calc(tii)) return false;

        trans = new AffineTransform();
        trans.concatenate(_posAngleTrans);
        trans.translate(_baseScreenPos.x, _baseScreenPos.y);
        trans.scale(tii.getPixelsPerArcsec(), tii.getPixelsPerArcsec());
        return true;
    }



    protected Shape getShape() {
        Area a = new Area(GsaoiDetectorArray.instance.shape());

        // Flip the shape if needed
        if (_flipRA == -1) {
            a = a.createTransformedArea(AffineTransform.getScaleInstance(_flipRA, 1.0));
        }

        return a.createTransformedArea(trans);
    }

    private void drawLabels(Graphics2D g2d) {
        Color origColor = g2d.getColor();
        Font  origFont  = g2d.getFont();

        Font font = FONT.deriveFont(Font.PLAIN);

        g2d.setColor(Color.red);
        g2d.setFont(font);

        for (Quadrant q : Quadrant.values()) drawLabel(g2d, q);

        g2d.setColor(origColor);
        g2d.setFont(origFont);
    }

    private IssPort getPort() {
        return _iw.getContext().instrument().issPortOrDefault();
    }

    private void drawLabel(Graphics2D g2d, Quadrant q) {
        final String idStr = q.id(getPort()).toString();
        final TextLayout  layout    = new TextLayout(idStr, g2d.getFont(), g2d.getFontRenderContext());
        final Rectangle2D strBounds = layout.getBounds();

        final int padding = 8;
        final double offset = ((strBounds.getHeight() + padding)/_tii.getPixelsPerArcsec())/2;

        final Rectangle2D bnds = q.shape().getBounds2D();
        final double x = (bnds.getX() < 0) ? bnds.getX() + offset : bnds.getMaxX() - offset;
        final double y = (bnds.getY() < 0) ? bnds.getY() + offset : bnds.getMaxY() - offset;
        final Point2D origPoint = new Point2D.Double(x*_flipRA, y); // flip RA if needed
        final Point2D destPoint = new Point2D.Double();
        trans.transform(origPoint, destPoint);

        final double textX = destPoint.getX() - strBounds.getWidth()/2.0;
        final double textY = destPoint.getY() + strBounds.getHeight()/2.0 + 0.5;
        layout.draw(g2d, (float) textX, (float) textY);
    }



    // Grim, but the super class is so poor that we basically have to handle
    // drawing from scratch.  Override and don't bother with the super class.
    @Override
    public void draw(Graphics g, TpeImageInfo tii) {
        Graphics2D g2d = (Graphics2D) g;

        if (!_calc(tii)) return;

        g2d.setColor(getFovColor());
        g2d.draw(getShape());
        drawLabels(g2d);
        drawDragItem(g2d);
    }

    /**
     * Draw the science area at the given x,y (screen coordinate) offset position.
     */
    @Override
    public void drawAtOffsetPos(Graphics g, TpeImageInfo tii, double x, double y) {
        Graphics2D g2d = (Graphics2D) g;
        if (!_calc(tii)) return;
        AffineTransform saveAT = g2d.getTransform();
        try {
            g2d.translate(x - _baseScreenPos.x, y - _baseScreenPos.y);
            g2d.draw(getShape());
        } finally {
            g2d.setTransform(saveAT);
        }
    }

}
