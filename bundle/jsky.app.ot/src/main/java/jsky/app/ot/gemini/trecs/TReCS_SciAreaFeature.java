package jsky.app.ot.gemini.trecs;

import diva.util.java2d.Polygon2D;
import edu.gemini.pot.ModelConverters;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.util.Angle;
import jsky.app.ot.gemini.inst.SciAreaFeature;
import jsky.app.ot.gemini.inst.SciAreaFeatureBase;
import jsky.app.ot.gemini.tpe.TpePWFSFeature;
import jsky.app.ot.tpe.TpeImageInfo;
import jsky.app.ot.tpe.TpeImageWidget;
import jsky.app.ot.tpe.TpeMouseEvent;
import jsky.coords.WorldCoords;
import jsky.util.gui.DrawUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;



/**
 * Draws the Science Area, the detector or slit.
 */
public class TReCS_SciAreaFeature extends SciAreaFeatureBase {

    // Used to draw thin lines
    private static final Stroke DEFAULT_STROKE = new BasicStroke(1.0F);

    // Used to draw dashed lines
    private static final Stroke DASHED_LINE_STROKE
            = new BasicStroke(1.0F,
                              BasicStroke.CAP_BUTT,
                              BasicStroke.JOIN_BEVEL,
                              0.0F,
                              new float[]{3.0F, 4.0F},
                              0.0F);

    // The color to use to draw the chop beams
    private static final Color CHOP_BEAM_COLOR = FOV_COLOR.darker();

    // Used to translate the chop slit figures to the correct location
    private AffineTransform _chopTrans1 = new AffineTransform();
    private AffineTransform _chopTrans2 = new AffineTransform();

    // The shapes used to draw the two chop slits
    private Shape _chopShape1;
    private Shape _chopShape2;

    // The shapes used to draw the two chop slit handles for dragging
    private Shape _chopHandle1;
    private Shape _chopHandle2;

    // The current nod/chop offset from the base position in screen pixels
    private Point2D.Double _nodChopOffset = new Point2D.Double(0., 0.);

    // Used for rotating the chop beams
    private boolean _chopDragging = false;
    private int _chopDragX;
    private int _chopDragY;

    // The world coordinates of the base position
    private WorldCoords _basePos;


    /**
     * Construct the feature with its name and description.
     */
    public TReCS_SciAreaFeature() {
    }


    /**
     * Reinitialize the feature (after the base position moves for instance).
     * Part of the TpeImageFeature interface and called by the TpeImageWidget.
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);
    }

    private InstTReCS getTrecs() {
        return _iw.getContext().instrument().orNull(InstTReCS.SP_TYPE);
    }


    /**
     * Calculate the polygons describing the screen location of the science area.
     */
    protected boolean _calc(TpeImageInfo tii)  {
        if (!super._calc(tii)) return false;

        InstTReCS inst = getTrecs();
        if (inst == null) return false;

        if (SciAreaFeature.getDisplayChopBeams()) {
            double chopAngle = tii.flipRA() * inst.getChopAngleRadians() + tii.getTheta(); // rad

            // In TPE the separation of the chop beams from base should be ? (chop throw)
            double chopThrow = inst.getChopThrow() * _pixelsPerArcsec;      // pixels

            _nodChopOffset.x = 0.;
            _nodChopOffset.y = -chopThrow;
            Angle.rotatePoint(_nodChopOffset, chopAngle);
            _chopTrans1.setToTranslation(_nodChopOffset.x, _nodChopOffset.y);
            _chopTrans2.setToTranslation(-_nodChopOffset.x, -_nodChopOffset.y);
        }

        return true;
    }


    /**
     * Update the figures to draw.
     */
    private void _updateFigures() {
        if (SciAreaFeature.getDisplayChopBeams()) {
            _addChopBeams();
        }
    }


    /** Add the chop beams to the list of figures to display. */
    private void _addChopBeams() {
        Polygon2D.Double chopBeam = _sciAreaPD.getPolygon2D();
        _chopShape1 = _chopTrans1.createTransformedShape(chopBeam);
        _chopShape2 = _chopTrans2.createTransformedShape(chopBeam);

        // drag handle is a small rectangle in the center of the beam
        Rectangle bounds = chopBeam.getBounds();
        double x = bounds.x + bounds.width / 2.;
        double y = bounds.y + bounds.height / 2.;
        int r = 3;
        double[] coords = new double[]{
            x - r, y - r,
            x + r, y - r,
            x + r, y + r,
            x - r, y + r
        };
        Polygon2D.Double chopHandle = new Polygon2D.Double(coords);
        Shape s = _posAngleTrans.createTransformedShape(chopHandle);
        _chopHandle1 = _chopTrans1.createTransformedShape(s);
        _chopHandle2 = _chopTrans2.createTransformedShape(s);
    }


    /**
     * Draw the science area at the given x,y (screen coordinate) offset position.
     */
    @Override
    public void drawAtOffsetPos(Graphics g, TpeImageInfo tii, double x, double y) {
        if (_sciArea == null)
            return;

        Graphics2D g2d = (Graphics2D) g;
        Polygon2D.Double p = _sciArea.getPolygon2DAt(x, y);
        g2d.draw(p);

        if (SciAreaFeature.getDisplayChopBeams()) {
            g2d.setStroke(DASHED_LINE_STROKE);
            g2d.draw(_chopTrans1.createTransformedShape(p));
            g2d.draw(_chopTrans2.createTransformedShape(p));
            g2d.setStroke(DEFAULT_STROKE);
        }
    }


    /**
     * Return the current Nod/Chop offset in screen pixels, if any,
     * or null if not applicable.
     * This is used to optionally offset the PWFS display.
     */
    public Point2D.Double getNodChopOffset() {
        Point2D.Double offset = new Point2D.Double(0., 0.);
        int mode = TpePWFSFeature.getNodMode();
        switch (mode) {
            case TpePWFSFeature.DEFAULT_NOD:
                break;
            case TpePWFSFeature.NOD_A_CHOP_B:  // top
                offset.x = _nodChopOffset.x;
                offset.y = _nodChopOffset.y;
                break;
            case TpePWFSFeature.NOD_B_CHOP_A:  // bottom
                offset.x = -_nodChopOffset.x;
                offset.y = -_nodChopOffset.y;
                break;
        }

        return offset;
    }

    /**
     * Draw the science area.
     * (Redefined from the parent class to use dashed lines for the chop beams)
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        Graphics2D g2d = (Graphics2D) g;

        if (!_calc(tii)) return;
        _updateFigures();

        InstTReCS inst = getTrecs();
        if (inst == null) return;

        g2d.setColor(FOV_COLOR);

        // Draw the drag item and science area
        g2d.fill(_tickMarkPD.getPolygon2D());
        g2d.draw(_sciAreaPD.getPolygon2D());

        if (_dragging) {
            // Draw a little above the mouse
            int baseX = _dragX;
            int baseY = _dragY - 10;

            // Draw a string displaying the rotation angle
            String s = "position angle = " + inst.getPosAngleDegreesStr() + " deg";
            g2d.setFont(POS_ANGLE_FONT);
            DrawUtil.drawString(g, s, FOV_COLOR, Color.black, baseX, baseY);
        } else if (_chopDragging) {
            // Draw a little above the mouse
            int baseX = _chopDragX;
            int baseY = _chopDragY - 10;

            // Draw a string displaying the chop angle and throw
            String s = "chop angle = " + ((int) inst.getChopAngle())
                    + " deg, throw = " + ((int) inst.getChopThrow()) + " arcsec";
            g2d.setFont(FONT);
            DrawUtil.drawString(g, s, FOV_COLOR, Color.black, baseX, baseY);
        }

        if (SciAreaFeature.getDisplayChopBeams()) {
            // draw the chop beams
            g2d.setColor(CHOP_BEAM_COLOR);
            g2d.setStroke(DASHED_LINE_STROKE);
            g2d.draw(_chopShape1);
            g2d.draw(_chopShape2);
            g2d.setStroke(DEFAULT_STROKE);
            g2d.fill(_chopHandle1);
            g2d.fill(_chopHandle2);
        }
    }


    /**
     * Handle the dragging of the chop beams to set the chop angle and throw.
     */
    @Override
    public Option<Object> dragStart(TpeMouseEvent tme, TpeImageInfo tii) {
        Option<Object> res = super.dragStart(tme, tii);
        if (!res.isEmpty()) return res;

        InstTReCS inst = getTrecs();
        if (inst == null) return None.instance();

        if (_chopHandle1 == null || _chopHandle2 == null) return None.instance();

        _chopDragging = false;
        Shape chopHandle = null;

        Point2D.Double pos = new Point2D.Double(tme.xWidget, tme.yWidget);
        if (_chopHandle1 != null && _chopHandle1.contains(pos)) {
            chopHandle = _chopHandle1;
        } else if (_chopHandle2 != null && _chopHandle2.contains(pos)) {
            chopHandle = _chopHandle2;
        }

        if (chopHandle != null) {
            _basePos = tii.getBasePos();
            _chopDragging = true;
            _chopDragX = tme.xWidget;
            _chopDragY = tme.yWidget;
            return new Some<>(inst);
        }

        return None.instance();
    }

    /**
     * Drag to a new location.
     */
    public void drag(TpeMouseEvent tme) {
        super.drag(tme);

        if (_chopDragX == tme.xWidget && _chopDragY == tme.yWidget) {
            _iw.repaint();
            return;
        }

        InstTReCS inst = getTrecs();
        if (inst == null) return;

        if (_chopDragging) {
            _chopDragX = tme.xWidget;
            _chopDragY = tme.yWidget;

            // update the angle
            inst.setChopAngleRadians(_tii.positionAngle(tme).toRadians());

            // get distance between base position and the mouse position in arcsec
            double dist = Coordinates.difference(ModelConverters.toCoordinates(_basePos), tme.pos).distance().toArcsecs();
            inst.setChopThrow(Math.round(dist));

            _iw.repaint();
        }
    }

    /**
     * Stop dragging.
     */
    public void dragStop(TpeMouseEvent tme) {
        super.dragStop(tme);

        if (_chopDragging) {
            drag(tme);
            _chopDragging = false;
            _iw.getContext().instrument().commit();
        }
    }


    /**
     * Return true if the mouse is over an active part of this image feature
     * (so that dragging can begin there).
     */
    public boolean isMouseOver(TpeMouseEvent tme) {
        if (super.isMouseOver(tme))
            return true;

        Point2D.Double pos = new Point2D.Double(tme.xWidget, tme.yWidget);
        return ((_chopHandle1 != null && _chopHandle1.contains(pos))
                || (_chopHandle2 != null && _chopHandle2.contains(pos)));
    }
}

