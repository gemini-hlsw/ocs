package jsky.app.ot.gemini.niri;

import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.niri.Niri;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import jsky.app.ot.gemini.inst.OIWFS_FeatureBase;
import jsky.app.ot.tpe.TpeImageInfo;
import jsky.app.ot.util.PolygonD;
import jsky.app.ot.util.ScreenMath;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Hashtable;


/**
 * Draws the OIWFS overlay for NIRI.
 */
public class NIRI_OIWFS_Feature extends OIWFS_FeatureBase {

    // Composite used for drawing items that block the view
    private static final Composite BLOCKED = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9F);

    // The selected camera
    private Niri.Camera _camera;

    /**
     * Construct the feature with its name and description.
     */
    public NIRI_OIWFS_Feature() {
        super("NIRI OIWFS FOV", "Show the field of view of the OIWFS (if any).");
    }


    /**
     * Update the list of figures to draw.
     *
     * @param guidePosX the X screen coordinate position for the OIWFS guide star
     * @param guidePosY the Y screen coordinate position for the OIWFS guide star
     * @param offsetPosX the X screen coordinate for the selected offset
     * @param offsetPosY the X screen coordinate for the selected offset
     * @param translateX translate resulting figure by this amount of pixels in X
     * @param translateY translate resulting figure by this amount of pixels in Y
     * @param basePosX the X screen coordinate for the base position (IGNORED)
     * @param basePosY the Y screen coordinate for the base position (IGNORED)
     * @param oiwfsDefined set to true if an OIWFS position is defined (otherwise
     *                     the xg and yg parameters are ignored)
     */
    protected void _updateFigureList(double guidePosX, double guidePosY, double offsetPosX, double offsetPosY,
                                     double translateX, double translateY, double basePosX, double basePosY, boolean oiwfsDefined) {
        _figureList.clear();

        InstNIRI inst = _iw.getContext().instrument().orNull(InstNIRI.SP_TYPE);
        if (inst == null) return;

        _camera = inst.getCamera();

        int r, d;
        if (getWithVignetting()) {
            r = (int) (_pixelsPerArcsec * 84.0);
            d = (int) (_pixelsPerArcsec * 168.0);
        } else {
            r = (int) (_pixelsPerArcsec * 105.0);
            d = (int) (_pixelsPerArcsec * 210.0);
        }

        Point2D.Double p = new Point2D.Double(offsetPosX + translateX, offsetPosY + translateY);
        Ellipse2D.Double oval = new Ellipse2D.Double(p.x - r, p.y - r, d, d);
        Composite composite = null;
        _figureList.add(new Figure(oval, OIWFS_COLOR, composite, OIWFS_STROKE));

        // Obscured area
        Polygon obscuredPoly = NIRI_OIWFS_Obscured.getObscured(_camera, _withVignetting, _pixelsPerArcsec,
                                                               offsetPosX + translateX, offsetPosY + translateY,
                offsetPosX, offsetPosY, _posAngle);
        if (getFillObscuredArea())
            composite = BLOCKED;
        _figureList.add(new Figure(obscuredPoly, OIWFS_OBSCURED_COLOR, BLOCKED, OIWFS_STROKE));
    }


    /** Return true if the display needs to be updated because values changed. */
    protected boolean _needsUpdate(SPInstObsComp inst, TpeImageInfo tii) {
        if (super._needsUpdate(inst, tii))
            return true;

        return (_camera != ((InstNIRI) inst).getCamera());
    }
}


class NIRI_OIWFS_Obscured {
    private static Hashtable<Niri.Camera, PolygonD> _ignoreVig =
            new Hashtable<Niri.Camera, PolygonD>(4);
    private static Hashtable<Niri.Camera, PolygonD> _withVig   =
            new Hashtable<Niri.Camera, PolygonD>(4);

//    private static final SPTypeBase CAMERA_F6 = NIRIParams.Camera.f6;
//    private static final SPTypeBase CAMERA_F14 = NIRIParams.Camera.f14;
//    private static final SPTypeBase CAMERA_F32 = NIRIParams.Camera.f32;
//    private static final SPTypeBase CAMERA_F32_PV = NIRIParams.Camera.f32_pv;

    //
    // Calculate the obscured region based upon a center square with a
    // cross (X) on top of it that covers the area of the given OIWFS
    // range (circle).
    //
    private static PolygonD _calculateObscured(
            double centerSquareWidth, double crossWidth,
            double circleDiameter) {

        double[] xpoints = new double[17];
        double[] ypoints = new double[17];

        double halfCrossWidth = crossWidth / 2.0;
        double halfCircleDiameter = circleDiameter / 2.0;

        double tmp = Math.sqrt(2.0 * (centerSquareWidth * centerSquareWidth)) / 2.0;
        tmp -= halfCrossWidth;

        // Assign the coordinate pairs working clockwise

        // Quadrant I points
        xpoints[0] = halfCrossWidth;
        ypoints[0] = halfCircleDiameter;

        xpoints[1] = halfCrossWidth;
        ypoints[1] = tmp;

        xpoints[2] = tmp;
        ypoints[2] = halfCrossWidth;

        xpoints[3] = halfCircleDiameter;
        ypoints[3] = halfCrossWidth;


        // Quadrant IV points
        xpoints[4] = xpoints[3];
        xpoints[5] = xpoints[2];
        ypoints[4] = -ypoints[3];
        ypoints[5] = -ypoints[2];

        xpoints[6] = xpoints[1];
        xpoints[7] = xpoints[0];
        ypoints[6] = -ypoints[1];
        ypoints[7] = -ypoints[0];


        // Quadrant III points
        xpoints[8] = -xpoints[0];
        xpoints[9] = -xpoints[1];
        ypoints[8] = -ypoints[0];
        ypoints[9] = -ypoints[1];

        xpoints[10] = -xpoints[2];
        xpoints[11] = -xpoints[3];
        ypoints[10] = -ypoints[2];
        ypoints[11] = -ypoints[3];


        // Quadrant II points
        xpoints[12] = -xpoints[3];
        xpoints[13] = -xpoints[2];
        ypoints[12] = ypoints[3];
        ypoints[13] = ypoints[2];

        xpoints[14] = -xpoints[1];
        xpoints[15] = -xpoints[0];
        ypoints[14] = ypoints[1];
        ypoints[15] = ypoints[0];


        // close the loop
        xpoints[16] = xpoints[0];
        ypoints[16] = ypoints[0];


        // Rotate by 45 degrees.
        double sin = new Angle(Math.sin(Math.PI / 4.0), Angle.Unit.RADIANS).getMagnitude();
        double cos = new Angle(Math.cos(Math.PI / 4.0), Angle.Unit.RADIANS).getMagnitude();
        for (int i = 0; i < xpoints.length; ++i) {
            double x0 = xpoints[i];
            double y0 = ypoints[i];

            xpoints[i] = x0 * cos - y0 * sin;
            ypoints[i] = x0 * sin + y0 * cos;
        }

        return new PolygonD(xpoints, ypoints, 17);
    }


    //
    // Calculate the polygon describing the f/6 camera with vignetting.
    // This is provided seperately from the general _calculateVignetting
    // routine because it contains no cross region (i.e., it is just a
    // simple square) and because the square extends beyond the bounds of
    // the OIWFS range.
    //
    private static PolygonD _calculateF6WithVignetting(double centerSquareWidth) {
        double halfSquareWidth = centerSquareWidth / 2.0;

        double[] xpoints = new double[5];
        double[] ypoints = new double[5];

        xpoints[0] = halfSquareWidth;
        xpoints[1] = -halfSquareWidth;
        ypoints[0] = halfSquareWidth;
        ypoints[1] = halfSquareWidth;

        xpoints[2] = -halfSquareWidth;
        xpoints[3] = halfSquareWidth;
        ypoints[2] = -halfSquareWidth;
        ypoints[3] = -halfSquareWidth;

        xpoints[4] = xpoints[0];
        ypoints[4] = ypoints[0];

        return new PolygonD(xpoints, ypoints, 5);
    }

    //
    // Get the PolygonD describing the obscured region for the given
    // camera and vignetting option.  Fills in the arrays as needed:
    //
    private static PolygonD _getObscured(Niri.Camera camera, boolean withVignetting) {
        PolygonD pd;

        if (withVignetting) {
            pd = _withVig.get(camera);
            if (pd == null) {
                if (camera == Niri.Camera.F6) {
                    pd = _calculateF6WithVignetting(148.0);
                    _withVig.put(Niri.Camera.F6, pd);
                } else {
                    pd = _calculateObscured(61.0, 15.0, 168.0);
                    _withVig.put(Niri.Camera.F14, pd);
                    _withVig.put(Niri.Camera.F32, pd);
                    _withVig.put(Niri.Camera.F32_PV, pd);
                }
            }
        } else {
            pd = _ignoreVig.get(camera);
            if (pd == null) {
                if (camera == Niri.Camera.F6) {
                    pd = _calculateObscured(123.0, 2.0, 210.0);
                    _ignoreVig.put(Niri.Camera.F6, pd);
                } else {
                    pd = _calculateObscured(51.0, 2.0, 210.0);
                    _ignoreVig.put(Niri.Camera.F14, pd);
                    _ignoreVig.put(Niri.Camera.F32, pd);
                    _ignoreVig.put(Niri.Camera.F32_PV, pd);
                }
            }
        }
        return pd;
    }

    /**
     * Get a Polygon describing the obscured region of the OIWFS range.
     */
    public static Polygon getObscured(Niri.Camera camera, boolean withVignetting,
                                      double pixelsPerArcsec,
                                      double offsetPosX, double offsetPosY,
                                      double basePosX, double basePosY,
                                      double angle) {

        PolygonD obscPD = _getObscured(camera, withVignetting);
        if (obscPD == null) throw new NullPointerException("obscured is null");

        // Calculate the screen location of the obscured region.
        //noinspection ConstantConditions
        PolygonD pd = (PolygonD) obscPD.clone();
        for (int i = 0; i < pd.npoints; ++i) {
            pd.xpoints[i] = (pd.xpoints[i] * pixelsPerArcsec) + offsetPosX;
            pd.ypoints[i] = (-pd.ypoints[i] * pixelsPerArcsec) + offsetPosY;
        }
        ScreenMath.rotateRadians(pd, angle, basePosX, basePosY);

        return pd.getAWTPolygon();
    }
}

