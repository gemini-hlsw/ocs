package jsky.app.ot.tpe;

import java.awt.Polygon;


import edu.gemini.spModel.obscomp.SPInstObsComp;
import jsky.app.ot.util.PolygonD;
import jsky.app.ot.util.ScreenMath;
import diva.util.java2d.Polygon2D;

/**
 * Describes the science area and facilitates drawing, rotating it.
 */
public class TpeSciArea {

    // Dimensions of the science area in screen pixels
    private double _width;
    private double _height;

    private double _posAngleRadians;
    private double _flipRA;    // direction of rotation
    private double _skyCorrection;

    // scratch work variable
    private PolygonD _pd;


    /**
     * Constructor
     */
    public TpeSciArea() {
        _pd = new PolygonD();
        _pd.xpoints = new double[5];
        _pd.ypoints = new double[5];
        _pd.npoints = 5;
    }


    /** Return the width of the science area in screen pixels */
    public double getWidth() {
        return _width;
    }

    /** Return the height of the science area in screen pixels */
    public double getHeight() {
        return _height;
    }

    public double getPosAngleRadians() {
        return _posAngleRadians;
    }

    public double getSkyCorrection() {
        return _skyCorrection;
    }

    /**
     * Update the SciArea fields, returning true iff changes were made.
     *
     * @param spInst the instrument data object corresponding to the current observation
     * @param tii info about base pos and angle
     */
    public boolean update(SPInstObsComp spInst, TpeImageInfo tii)
             {

        double[] ds = spInst.getScienceArea();
        double pixelsPerArcsec = tii.getPixelsPerArcsec();
        double w = ds[0] * pixelsPerArcsec;
        double h = ds[1] * pixelsPerArcsec;


        double posAngle = spInst.getPosAngleRadians();
        double sky = tii.getTheta();
        double flipRA = tii.flipRA();

        // Update the instance variables if necessary.
        if ((w != _width) ||
                (h != _height) ||
                (posAngle != _posAngleRadians) ||
                (sky != _skyCorrection) ||
                (flipRA != _flipRA)) {
            _width = w;
            _height = h;
            _posAngleRadians = posAngle;
            _skyCorrection = sky;
            _flipRA = flipRA;
            return true;
        }

        return false;
    }

    /**
     * Get an AWT Polygon object representing the science area at the
     * given x, y location, taking into account rotation.
     */
    public Polygon getPolygonAt(double x, double y) {
        double hw = _width / 2.0;
        double hh = _height / 2.0;

        PolygonD pd = _pd;
        double[] xpoints = pd.xpoints;
        double[] ypoints = pd.ypoints;

        xpoints[0] = x - hw;
        xpoints[1] = x + hw;
        ypoints[0] = y - hh;
        ypoints[1] = y - hh;

        xpoints[2] = x + hw;
        xpoints[3] = x - hw;
        ypoints[2] = y + hh;
        ypoints[3] = y + hh;

        xpoints[4] = xpoints[0];
        ypoints[4] = ypoints[0];

        ScreenMath.rotateRadians(pd, _flipRA * _posAngleRadians + _skyCorrection, x, y);
        return pd.getAWTPolygon();
    }

    /**
     * Get a PolygonD object representing the science area at the
     * given x, y location, taking into account rotation.
     */
    public PolygonD getPolygonDAt(double x, double y) {
        double hw = _width / 2.0;
        double hh = _height / 2.0;

        PolygonD pd = _pd;
        double[] xpoints = pd.xpoints;
        double[] ypoints = pd.ypoints;

        xpoints[0] = x - hw;
        xpoints[1] = x + hw;
        ypoints[0] = y - hh;
        ypoints[1] = y - hh;

        xpoints[2] = x + hw;
        xpoints[3] = x - hw;
        ypoints[2] = y + hh;
        ypoints[3] = y + hh;

        xpoints[4] = xpoints[0];
        ypoints[4] = ypoints[0];

        ScreenMath.rotateRadians(pd, _flipRA * _posAngleRadians + _skyCorrection, x, y);
        return new PolygonD(pd);
    }

    /**
     * Get a Polygon2D object representing the science area at the
     * given x, y location, taking into account rotation.
     */
    public Polygon2D.Double getPolygon2DAt(double x, double y) {
        PolygonD pd = getPolygonDAt(x, y);
        return pd.getPolygon2D();
    }
}
