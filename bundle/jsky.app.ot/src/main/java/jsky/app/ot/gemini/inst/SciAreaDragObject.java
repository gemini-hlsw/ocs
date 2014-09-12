// Copyright 1997-2001 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SciAreaDragObject.java 21057 2009-07-13 22:13:12Z swalker $
//
package jsky.app.ot.gemini.inst;


import edu.gemini.spModel.util.Angle;

/**
 * An implementation class used to simplify the job of rotating science
 * areas.
 */
public final class SciAreaDragObject {
    int _xb, _yb;
    double _angle = 0;

    /**
     * Construct with the base position at (xb,yb).
     */
    public SciAreaDragObject(int xb, int yb, int x, int y) {
        _xb = xb;
        _yb = yb;
        _angle = _getAngle(x, y);
    }

    public double nextAngleDiff(int x, int y) {
        double angle = _getAngle(x, y);
        double val = angle - _angle;
        _angle = angle;

        return val;
    }

    //
    // SW: This was the original "getAngle" method, which was only used
    // internally in this class.  It is called by "nextAngleDiff" above,
    // which is used for dragging the science area and computing the position
    // angle accordingly.
    //
    // When implementing GSAOI, I noticed weird drag behavior and looked into
    // it.  It appears to me that the cause is accummulating error in
    // constantly computing "angle differences" and adding them to the existing
    // position angle.  It seemed much better to me to just directly compute
    // the position angle and assign it each time rather than constantly
    // adding up small differences.
    //
    // TODO: ? Fix the SciAreaFeatureBase to just directly call the updated
    // getAngle method instead of adding up tiny diffs.  This is done in an
    // overridden "drag" method in GsaoiDetectorArrayFeature.
    //
    private double _getAngle(int x, int y) {
        double angle;

        // All the points are in screen coordinates, which means y increases down
        // This makes x and y relative to the origin in a right side up frame.
        int xp = x - _xb;
        int yp = _yb - y;

        int xa = Math.abs(xp);
        int ya = Math.abs(yp);

        if (xa == 0) {
            if (yp >= 0) {
                return Math.PI * 0.5;
            } else {
                return Math.PI * 1.5;
            }
        }

        angle = Angle.atanRadians(((double) ya) / ((double) xa));

        if ((xp > 0) && (yp >= 0)) {
            return angle;
        }

        if ((xp < 0) && (yp >= 0)) {
            return Math.PI - angle;
        }

        if ((xp < 0) && (yp < 0)) {
            return Math.PI + angle;
        }

        return Math.PI * 2.0 - angle;
    }

    public double getAngle(int x, int y) {
        double angle;

        // All the points are in screen coordinates, which means y increases down
        // This makes x and y relative to the origin in a right side up frame.
        int xp = x - _xb;
        int yp = _yb - y;

        int xa = Math.abs(xp);
        int ya = Math.abs(yp);

        if (xa == 0) {
            if (yp >= 0) {
                return 0.0;
            } else {
                return Math.PI;
            }
        }

        angle = Angle.atanRadians(((double) xa) / ((double) ya));

        if ((xp > 0) && (yp >= 0)) {
            return Math.PI * 2.0 - angle;
        }

        if ((xp < 0) && (yp >= 0)) {
            return angle;
        }

        if ((xp < 0) && (yp < 0)) {
            return Math.PI - angle;
        }

        return Math.PI + angle;
    }
}

