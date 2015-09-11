// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: PolygonD.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.util;

import java.awt.Polygon;

import diva.util.java2d.Polygon2D;

/**
 * Like a java.awt.Polygon, but instead of integer coordinates, doubles are
 * used.
 */
public class PolygonD implements Cloneable {

    public int npoints = 0;
    public double xpoints[] = new double[4];
    public double ypoints[] = new double[4];

    private Polygon _awtPolygon;

    /**
     * Creates an empty polygon.
     */
    public PolygonD() {
    }

    /**
     * Creates a new PolygonD copying from the given PolygonD.
     */
    public PolygonD(PolygonD srcPD) {
        this.npoints = srcPD.npoints;
        this.xpoints = new double[this.npoints];
        this.ypoints = new double[this.npoints];
        System.arraycopy(srcPD.xpoints, 0, this.xpoints, 0, this.npoints);
        System.arraycopy(srcPD.ypoints, 0, this.ypoints, 0, this.npoints);
    }

    /**
     * Initializes a PolygonD from the specified parameters.
     * @param xpoints the array of x coordinates
     * @param ypoints the array of y coordinates
     * @param npoints the total number of points in the Polygon
     */
    public PolygonD(double[] xpoints, double[] ypoints, int npoints) {
        this.npoints = npoints;
        this.xpoints = new double[npoints];
        this.ypoints = new double[npoints];
        System.arraycopy(xpoints, 0, this.xpoints, 0, npoints);
        System.arraycopy(ypoints, 0, this.ypoints, 0, npoints);
    }

    public Polygon getAWTPolygon() {
        if ((_awtPolygon == null) || (_awtPolygon.npoints != npoints)) {
            _awtPolygon = new Polygon(new int[npoints], new int[npoints], npoints);
        }
        for (int i = 0; i < npoints; ++i) {
            _awtPolygon.xpoints[i] = (int) (xpoints[i] + 0.5);
            _awtPolygon.ypoints[i] = (int) (ypoints[i] + 0.5);
        }
        return _awtPolygon;
    }

    public Polygon2D.Double getPolygon2D() {
        double[] points = new double[npoints * 2];
        for (int i = 0; i < npoints; i++) {
            points[i * 2] = xpoints[i];
            points[i * 2 + 1] = ypoints[i];
        }

        return new Polygon2D.Double(points);
    }

    public String toString() {
        return getClass().getName() + "[x=" + xpoints + ", y=" + ypoints + "]";
    }

    public Object clone() {
        PolygonD pd;
        try {
            pd = (PolygonD) super.clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
        pd.xpoints = xpoints.clone();
        pd.ypoints = ypoints.clone();
        return pd;
    }
}

