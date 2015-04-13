/**
 * $Id: Flamingos2_SciAreaFeature.java 46768 2012-07-16 18:58:53Z rnorris $
 */

package jsky.app.ot.gemini.flamingos2;

import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.flamingos2.F2ScienceAreaGeometry;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.FPUnit;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.inst.FeatureGeometry$;
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
     * Update the list of FOV figures to draw.
     */
    protected void _updateFigureList() {
        _figureList.clear();
        final Flamingos2 inst = getFlamingos2();
        if (inst != null) {
            final ObsContext ctx = _iw.getMinimalObsContext().getOrNull();
            final ImList<Shape> shape = inst.getVignettableScienceArea().geometryAsJava();
            final AffineTransform ptm = getPosAngleTransformModifier();
            shape.foreach(new ApplyOp<Shape>() {
                @Override
                public void apply(final Shape s) {
                    final Shape s2 = FeatureGeometry$.MODULE$.transformScienceAreaForContext(s, ctx);
                    final Shape s3 = FeatureGeometry$.MODULE$.transformScienceAreaForScreen(s2, _pixelsPerArcsec, ctx, _baseScreenPos);
                    final Shape s4 = ptm.createTransformedShape(s3);
                    _figureList.add(s4);
                }
            });
        }
    }

    private Flamingos2 getFlamingos2() {
        return _iw.getContext().instrument().orNull(Flamingos2.SP_TYPE);
    }

    /*
     * Overriden to take into account fov rotation from config file
     */
    @Override
    protected boolean _calc(final TpeImageInfo tii)  {
        final Flamingos2 inst = getFlamingos2();
        if (inst == null) return false;

        _sciArea.update(inst, tii);
        _baseScreenPos = tii.getBaseScreenPos();
        _sciAreaPD = _sciArea.getPolygonDAt(_baseScreenPos.x, _baseScreenPos.y);
        _pixelsPerArcsec = tii.getPixelsPerArcsec();
        final double posAngle = tii.getCorrectedPosAngleRadians();
        _posAngleTrans.setToIdentity();
        _posAngleTrans.rotate(-posAngle, _baseScreenPos.x, _baseScreenPos.y);


        final ObsContext ctx = _iw.getObsContext().getOrNull();
        if (ctx != null) {
            final AbstractDataObject aoComp = ctx.getAOComponent().getOrNull();
            if (aoComp != null) {
                _fovRotation = inst.getRotationConfig(aoComp.getNarrowType().equals(Gems.SP_TYPE.narrowType)).toRadians().getMagnitude();
            } else {
                _fovRotation = inst.getRotationConfig(false).toRadians().getMagnitude();
            }
        } else {
            _fovRotation = inst.getRotationConfig(false).toRadians().getMagnitude();
        }

        //rotate science area fov
        _posAngleTrans.rotate(-_fovRotation, _baseScreenPos.x, _baseScreenPos.y);

        // Init the _tickMarkPD
        if (_tickMarkPD == null) {
            final double[] xpoints = new double[4];
            final double[] ypoints = new double[4];
            _tickMarkPD = new PolygonD(xpoints, ypoints, 4);
        }

        final Point2D.Double tickOffset = _getTickMarkOffset();

        _tickMarkPD.xpoints[0] = tickOffset.x;
        _tickMarkPD.ypoints[0] = tickOffset.y - MARKER_SIZE * 2;

        _tickMarkPD.xpoints[1] = tickOffset.x - MARKER_SIZE;
        _tickMarkPD.ypoints[1] = tickOffset.y - 2;

        _tickMarkPD.xpoints[2] = tickOffset.x + MARKER_SIZE;
        _tickMarkPD.ypoints[2] = tickOffset.y - 2;

        _tickMarkPD.xpoints[3] = _tickMarkPD.xpoints[0];
        _tickMarkPD.ypoints[3] = _tickMarkPD.ypoints[0];

        // Rotate tick mark
        final double angle = tii.getCorrectedPosAngleRadians() + _fovRotation;

        ScreenMath.rotateRadians(_tickMarkPD, angle, _baseScreenPos.x, _baseScreenPos.y);
        _updateFigureList();
        return true;
    }

    /**
     * Draw the feature.
     */
    @Override
    public void draw(final Graphics g, final TpeImageInfo tii) {
        final Graphics2D g2d = (Graphics2D) g;

        if (!_calc(tii)) return;

        g2d.setColor(FOV_COLOR);

        // Draw the FOV
        for (final Shape shape : _figureList)
            g2d.draw(shape);

        drawDragItem(g2d);
    }
    /**
      * Drag to a new location.
      * Overridden to take into account fov rotation from config file.
      */
    @Override
     public void drag(final TpeMouseEvent tme) {
         if (_dragX == tme.xWidget && _dragY == tme.yWidget) {
             _iw.repaint();
             return;
         }

         if (_dragObject == null) return;
         _dragX = tme.xWidget;
         _dragY = tme.yWidget;

         final double radians = _dragObject.getAngle(_dragX, _dragY) * _tii.flipRA() + _tii.getTheta();
         final double degrees = Math.round(Angle.radiansToDegrees(radians-_fovRotation));
        _iw.setPosAngle(degrees);
        _iw.repaint();
     }

    /**
     * Draw the science area at the given x,y (screen coordinate) offset position.
     */
    @Override
    public void drawAtOffsetPos(final Graphics g, final TpeImageInfo tii, final double x, final double y) {
        if (!_calc(tii)) return;

        final Graphics2D g2d = (Graphics2D) g;
        if (getFlamingos2() != null) {
            final AffineTransform saveAT = g2d.getTransform();
            try {
                g2d.translate(x - _baseScreenPos.x, y - _baseScreenPos.y);
                for (final Shape shape : _figureList) {
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
            final FPUnit fpu = inst.getFpu();

            final double plateScale = inst.getLyotWheel().getPlateScale();
            if (fpu.isLongslit()) {
                final double slitNorth = F2ScienceAreaGeometry.LongSlitFOVNorthPos() * plateScale * _pixelsPerArcsec;
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
