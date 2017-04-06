package jsky.app.ot.gemini.acqcam;

import jsky.app.ot.tpe.TpeImageFeature;
import jsky.app.ot.tpe.TpeImageFeatureCategory;
import jsky.app.ot.tpe.TpeImageInfo;
import jsky.app.ot.tpe.TpeImageWidget;
import jsky.app.ot.util.PolygonD;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Draws the field of view of the acquisition camera.
 */
public class TpeAcqCameraFeature extends TpeImageFeature {
    public static final double FOV_WIDTH = 120.0;
    public static final double FOV_HEIGHT = 120.0;

    private PolygonD _fovAreaPD;
    private boolean _valid = false;

    /**
     * Construct the feature with its name and description.
     */
    public TpeAcqCameraFeature() {
        super("Acq Cam", "Show the field of view of the Acquistion Camera.");
    }

    /**
     * Reinit.
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);
        _valid = false;
    }

    /**
     * The position angle has changed.
     */
    public void posAngleUpdate(TpeImageInfo tii) {
        _valid = false;
    }

    /**
     * Calculate the polygon describing the screen location of the science area.
     */
    private void _calc(TpeImageInfo tii) {
        if (_fovAreaPD == null) {
            _fovAreaPD = new PolygonD();
            _fovAreaPD.xpoints = new double[5];
            _fovAreaPD.ypoints = new double[5];
            _fovAreaPD.npoints = 5;
        }

        double[] xpoints = _fovAreaPD.xpoints;
        double[] ypoints = _fovAreaPD.ypoints;

        Point2D.Double baseScreenPos = tii.getBaseScreenPos();
        double x = baseScreenPos.x;
        double y = baseScreenPos.y;

        double pixelsPerArcsec = tii.getPixelsPerArcsec();
        double w = (pixelsPerArcsec * FOV_WIDTH) / 2.0;
        double h = (pixelsPerArcsec * FOV_HEIGHT) / 2.0;

        xpoints[0] = x - w;
        xpoints[1] = x + w;
        ypoints[0] = y - h;
        ypoints[1] = y - h;

        xpoints[2] = x + w;
        xpoints[3] = x - w;
        ypoints[2] = y + h;
        ypoints[3] = y + h;

        xpoints[4] = xpoints[0];
        ypoints[4] = ypoints[0];

        _iw.skyRotate(_fovAreaPD);
        _valid = true;
    }

    /**
     * Draw the feature.
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        if (!_valid) {
            _calc(tii);
        }

        g.setColor(Color.lightGray);
        g.drawPolygon(_fovAreaPD.getAWTPolygon());
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.fieldOfView;
    }
}

