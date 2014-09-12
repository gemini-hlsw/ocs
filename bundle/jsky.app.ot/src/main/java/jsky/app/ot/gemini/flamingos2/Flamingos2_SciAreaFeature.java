/**
 * $Id: Flamingos2_SciAreaFeature.java 46768 2012-07-16 18:58:53Z rnorris $
 */

package jsky.app.ot.gemini.flamingos2;

import diva.util.java2d.Polygon2D;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.FPUnit;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.util.Angle;
import jsky.app.ot.gemini.inst.SciAreaFeatureBase;
import jsky.app.ot.tpe.TpeImageInfo;
import jsky.app.ot.tpe.TpeMouseEvent;
import jsky.app.ot.util.PolygonD;
import jsky.app.ot.util.ScreenMath;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;


/**
 * Draws the Science Area, the detector or slit (see OT-540).
 */
public class Flamingos2_SciAreaFeature  extends SciAreaFeatureBase {

    // OT-540:
    // Imaging field of view: 230.12mm diameter circle, centered on base position.
    //
    // MOS field of view: pseudo-rectangular, with arcs on top and bottom. width of
    // rectangle is 75.16mm. Top and bottom have radius 230.12mm from base position.
    //
    // Longslit FOV: narrow slit with length 164.1 mm. At a PA of 0 degrees, with the
    // instrument mounted on the sideport, the slit center is south of the center
    // of the imaging/MOS FOV. It runs from 112.0 mm South of the imaging field
    // center 52.1 mm North of the field center.


    // from config file
    private double _fovRotation;//in radians

    /**
     * Construct the feature
     */
    public Flamingos2_SciAreaFeature() {
    }


    /**
     * Add the imaging FOV to the list of figures to display.
     * @param plateScale plate scale in arcsec/mm
     */
    private void _addImagingFOV(double plateScale) {
        double size = Flamingos2.IMAGING_FOV_SIZE * plateScale * _pixelsPerArcsec;
        double radius = size/2.;
        Ellipse2D.Double fig = new Ellipse2D.Double(_baseScreenPos.x - radius,
                                                    _baseScreenPos.y - radius,
                                                    size, size);
        _figureList.add(fig);
    }

    /**
     * Add the MOS field of view to the list of figures to display.
     * @param plateScale plate scale in arcsec/mm
     */
    private void _addMOS_FOV(double plateScale) {

        // Make the rectangle and circle and then take the intersection
        double width = Flamingos2.MOS_FOV_WIDTH * plateScale * _pixelsPerArcsec;
        double height = Flamingos2.IMAGING_FOV_SIZE * plateScale * _pixelsPerArcsec;

        double radius = height /2.;
        Ellipse2D.Double circle = new Ellipse2D.Double(_baseScreenPos.x - radius,
                                                       _baseScreenPos.y - radius,
                                                       height, height);

        double x = _baseScreenPos.x - (width / 2);
        double y = _baseScreenPos.y - radius;
        Polygon2D.Double rect = new Polygon2D.Double(x, y);
        rect.lineTo(x + width, y);
        rect.lineTo(x + width, y + height);
        rect.lineTo(x, y + height);

        // rotate by position angle
        rect.transform(_posAngleTrans);

        // take the intersection
        Area area = new Area(circle);
        area.intersect(new Area(rect));

        _figureList.add(area);
    }

    /**
     * Add the long slit field of view to the list of figures to display.
     * @param plateScale plate scale in arcsec/mm
     */
    private void _addLongSlitFOV(double plateScale) {
        double slitWidth = _sciArea.getWidth();
        double slitHeight = Flamingos2.LONG_SLIT_FOV_HEIGHT * plateScale * _pixelsPerArcsec;

        double slitSouth = Flamingos2.LONG_SLIT_FOV_SOUTH_POS * plateScale * _pixelsPerArcsec;

        double x = _baseScreenPos.x - (slitWidth / 2);
        double y = _baseScreenPos.y + slitSouth;

        Polygon2D.Double slit = new Polygon2D.Double(x, y);
        slit.lineTo(x + slitWidth, y);
        slit.lineTo(x + slitWidth, y - slitHeight);
        slit.lineTo(x, y - slitHeight);

        // rotate by position angle
        slit.transform(_posAngleTrans);

        _figureList.add(slit);
    }

    /**
     * Update the list of FOV figures to draw.
     */
    protected void _updateFigureList() {
        _figureList.clear();

        // OT-540:
        // There should be different FOV drawings for imaging, MOS, longslit,
        // MCAO imaging, MCAO MOS, and the OIWFS (for both non-AO and MCAO feeds).
        // (OIWFS is handled elsewhere)
        Flamingos2 inst = getFlamingos2();
        if (inst != null) {
            double plateScale = inst.getLyotWheel().getPlateScale();
            Flamingos2.FPUnit fpu = inst.getFpu();
            switch (fpu) {
                case FPU_NONE:
                    _addImagingFOV(plateScale);
                    break;
                case CUSTOM_MASK:
                    _addMOS_FOV(plateScale);
                    break;
                default:
                    if (fpu.isLongslit()) _addLongSlitFOV(plateScale);
                    break;
            }
        }
    }

    private Flamingos2 getFlamingos2() {
        return _iw.getContext().instrument().orNull(Flamingos2.SP_TYPE);
    }

    /*
     * Overriden to take into account fov rotation from config file
     */
    @Override
    protected boolean _calc(TpeImageInfo tii)  {
        Flamingos2 inst = getFlamingos2();
        if (inst == null) return false;

        _sciArea.update(inst, tii);
        _baseScreenPos = tii.getBaseScreenPos();
        _sciAreaPD = _sciArea.getPolygonDAt(_baseScreenPos.x, _baseScreenPos.y);
        _pixelsPerArcsec = tii.getPixelsPerArcsec();
        double posAngle = tii.getCorrectedPosAngleRadians();
        _posAngleTrans.setToIdentity();
        _posAngleTrans.rotate(-posAngle, _baseScreenPos.x, _baseScreenPos.y);


        ObsContext ctx = _iw.getObsContext().getOrNull();
        if (ctx != null) {
            AbstractDataObject aoComp = ctx.getAOComponent().getOrNull();
            if (aoComp != null) {
                _fovRotation = inst.getRotationConfig(aoComp.getNarrowType().equals(Gems.SP_TYPE.narrowType)).toRadians().getMagnitude();
            } else {
                _fovRotation = inst.getRotationConfig(false).toRadians().getMagnitude();
            }
        } else {
// RCN: actually this is ok; it happens in template obs
//            System.out.println("No ObsContext!!!");
            _fovRotation = inst.getRotationConfig(false).toRadians().getMagnitude();
        }

        //rotate science area fov
        _posAngleTrans.rotate(-_fovRotation, _baseScreenPos.x, _baseScreenPos.y);

        // Init the _tickMarkPD
        if (_tickMarkPD == null) {
            double[] xpoints = new double[4];
            double[] ypoints = new double[4];
            _tickMarkPD = new PolygonD(xpoints, ypoints, 4);
        }

        Point2D.Double tickOffset = _getTickMarkOffset();

        _tickMarkPD.xpoints[0] = tickOffset.x;
        _tickMarkPD.ypoints[0] = tickOffset.y - MARKER_SIZE * 2;

        _tickMarkPD.xpoints[1] = tickOffset.x - MARKER_SIZE;
        _tickMarkPD.ypoints[1] = tickOffset.y - 2;

        _tickMarkPD.xpoints[2] = tickOffset.x + MARKER_SIZE;
        _tickMarkPD.ypoints[2] = tickOffset.y - 2;

        _tickMarkPD.xpoints[3] = _tickMarkPD.xpoints[0];
        _tickMarkPD.ypoints[3] = _tickMarkPD.ypoints[0];

        //_iw.skyRotate(_tickMarkPD);
        //rotate tick mark
        double angle = tii.getCorrectedPosAngleRadians() + _fovRotation;

        ScreenMath.rotateRadians(_tickMarkPD, angle, _baseScreenPos.x, _baseScreenPos.y);
        return true;
    }

    /**
     * Draw the feature.
     */
    @Override
    public void draw(Graphics g, TpeImageInfo tii) {
        Graphics2D g2d = (Graphics2D) g;

        if (!_calc(tii)) return;
        _updateFigureList();

        g2d.setColor(FOV_COLOR);

        // draw the FOV
        for (Shape shape : _figureList) g2d.draw(shape);
        drawDragItem(g2d);
    }
    /**
      * Drag to a new location.
      * Overridden to take into account fov rotation from config file.
      */
    @Override
     public void drag(TpeMouseEvent tme) {
         if (_dragX == tme.xWidget && _dragY == tme.yWidget) {
             _iw.repaint();
             return;
         }

         if (_dragObject == null) return;
         _dragX = tme.xWidget;
         _dragY = tme.yWidget;

         double radians = _dragObject.getAngle(_dragX, _dragY) * _tii.flipRA() + _tii.getTheta();
         double degrees = Math.round(Angle.radiansToDegrees(radians-_fovRotation));
        _iw.setPosAngle(degrees);
        _iw.repaint();
     }

    /**
     * Draw the science area at the given x,y (screen coordinate) offset position.
     */
    @Override
    public void drawAtOffsetPos(Graphics g, TpeImageInfo tii, double x, double y) {
        Graphics2D g2d = (Graphics2D) g;
        if (getFlamingos2() != null) {
            AffineTransform saveAT = g2d.getTransform();
            try {
                g2d.translate(x - _baseScreenPos.x, y - _baseScreenPos.y);
                for (Shape shape : _figureList) {
                    g2d.draw(shape);
                }
            } finally {
                g2d.setTransform(saveAT);
            }
        }
    }

    /**
     * Return the offset from the base position in arcsec for drawing the
     * "tick mark" (used as a handle to rotate the science area). The offset
     * depends on whether long slit is used or not since they are not
     * vertically centered at the base position.
     */
    protected Point2D.Double _getTickMarkOffset() {
        final Flamingos2 inst = getFlamingos2();
        final Point2D.Double offset;
        if (inst != null) {
            FPUnit fpu = inst.getFpu();

            double plateScale = inst.getLyotWheel().getPlateScale();
            if (fpu.isLongslit()) {
                double slitNorth = Flamingos2.LONG_SLIT_FOV_NORTH_POS * plateScale * _pixelsPerArcsec;
                offset = new Point2D.Double(_baseScreenPos.x,
                                            _baseScreenPos.y - slitNorth);

            } else {
                offset = new Point2D.Double(_baseScreenPos.x,
                                            _baseScreenPos.y - _sciArea.getHeight() / 2.0);
            }
        } else {
            offset = new Point2D.Double(_baseScreenPos.x, _baseScreenPos.y);
        }
        return offset;
    }

}
