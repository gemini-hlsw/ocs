// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ScreenMath.java 4725 2004-05-14 16:32:54Z brighton $
//
package jsky.app.ot.util;

import edu.gemini.spModel.util.Angle;

import java.awt.geom.Point2D;

/**
 * Routines for dealing with screen coordinates.
 */
public class ScreenMath {

    /**
     * Rotate a point through a given angle about a given point.
     */
    public static Point2D.Double rotateRadians(double x, double y, double angle, double xBase, double yBase) {
        if (angle == 0.0) {
            return new Point2D.Double(x, y);
        }

        // Translate x,y to be relative to the origin, where y is increasing
        // toward the top instead of toward the bottom.
        double x0 = x - xBase;
        double y0 = yBase - y;

        // Rotate the point and translate it back.
        double sin = Angle.sinRadians(angle);
        double cos = Angle.cosRadians(angle);

        x = xBase + (x0 * cos - y0 * sin);
        y = yBase - (x0 * sin + y0 * cos);

        return new Point2D.Double(x, y);
    }

    /**
     * Rotate a polygon through a given angle about a given point.
     */
    public static void rotateRadians(PolygonD p, double angle, double xBase, double yBase) {
        if (angle == 0.0) {
            return;
        }

        // Remember the sin and cos of the angle.
        double sin = Angle.sinRadians(angle);
        double cos = Angle.cosRadians(angle);

        // Rotate each point.
        for (int i = 0; i < p.npoints; ++i) {
            // Translate the point to be relative to the origin, and make y
            // increase toward the top instead of toward the bottom.
            double x0 = p.xpoints[i] - xBase;
            double y0 = yBase - p.ypoints[i];

            // Rotate the point and translate it back.
            p.xpoints[i] = xBase + (x0 * cos - y0 * sin);
            p.ypoints[i] = yBase - (x0 * sin + y0 * cos);
        }
    }

}

