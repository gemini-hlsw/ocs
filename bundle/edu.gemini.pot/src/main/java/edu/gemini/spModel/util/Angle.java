// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: Angle.java 4726 2004-05-14 16:50:12Z brighton $
//
package edu.gemini.spModel.util;

import java.awt.geom.Point2D;


/**
 * A utility class for angles.
 */
public final class Angle {
    private static double _ERROR = 0.000001;

    /**
     * Convert from degrees to radians.
     */
    public static double degreesToRadians(double degrees) {
        return degrees * Math.PI / 180.0;
    }

    /**
     * Convert from radians to degrees.
     */
    public static double radiansToDegrees(double radians) {
        return radians * 180.0 / Math.PI;
    }

    /**
     * Given an angle in degrees, determine whether the angle is very close to
     * zero.
     */
    public static boolean almostZeroDegrees(double angle) {
        if ((angle >= -_ERROR) && (angle <= _ERROR)) {
            return true;
        }
        if ((angle >= (360 - _ERROR)) && (angle <= (360 + _ERROR))) {
            return true;
        }
        return false;
    }

    /**
     * Given an angle in radians, determine whether the angle is very close
     * to zero.
     */
    public static boolean almostZeroRadians(double angle) {
        if ((angle >= -_ERROR) && (angle <= _ERROR)) {
            return true;
        }

        double twoPI = Math.PI * 2.0;
        if ((angle >= (twoPI - _ERROR)) && (angle <= (twoPI + _ERROR))) {
            return true;
        }

        return false;
    }


    //
    // Is the number almost zero?  This is a helper used to get the sin and
    // cos of angles correct when they should be zero...
    // (e.g., Math.cos(Math.PI) is 6.12323e-17).
    //
    private static boolean _almostZero(double n) {
        return (n >= -_ERROR) && (n <= _ERROR);
    }

    /**
     * Get the sin of the angle (in radians)
     */
    public static double sinRadians(double angle) {
        double d = Math.sin(angle);
        if (_almostZero(d)) {
            return 0;
        }
        return d;
    }

    /**
     * Get the cos of the angle (in radians)
     */
    public static double cosRadians(double angle) {
        double d = Math.cos(angle);
        if (_almostZero(d)) {
            return 0;
        }
        return d;
    }

    /**
     * Get the atan of the angle (in radians).
     */
    public static double atanRadians(double angle) {
        double d = Math.atan(angle);
        if (_almostZero(d)) {
            return 0;
        }
        return d;
    }

    ///**
    // * Given an angle in degrees, return an equivalent angle between 0 and 360.
    // * If the angle is 360, 0 is returned.
    // */
    //public static double
    //normalizeDegrees(double degrees)
    //{
    //   System.out.println("--> " + degrees);
    //   double abs = Math.abs( degrees );
    //   if (abs >= 360.0) {
    //      abs -= ((int) abs/360.0) * 360.0;
    //   }
    //
    //   if (almostZero(abs)) {
    //      degrees = 0.0;
    //   } else if (degrees > 0) {
    //      degrees = abs;
    //   } else {
    //      degrees = 360 - abs;
    //   }
    //
    //   System.out.println("<-- " + degrees);
    //
    //   return degrees;
    //}

    /**
     * Given an arbitrary angle (in degrees), return an equivalent angle in
     * degrees between 0 (inclusive) and 360 (exclusive).
     */
    public static double normalizeDegrees(double in) {
        double out = in;
        if (in < 0) {
            int t = ((int) (-in / 360.0)) + 1;
            out = in + ((double) (360 * t));
        } else if (in >= 360.0) {
            int t = (int) (in / 360.0);
            out = in - ((double) (360 * t));
        }
        if (almostZeroDegrees(out)) {
            return 0.0;
        }
        return out;
    }


    /**
     * Given an angle in radians, return an equivalent angle between 0 and 2PI.
     * If the angle is 2PI, 0 is returned.
     */
    public static double normalizeRadians(double radians) {
        double twoPI = Math.PI * 2.0;
        double abs = Math.abs(radians);

        if (abs >= twoPI) {
            abs -= ((int) (abs / twoPI)) * twoPI;
        }

        if (almostZeroRadians(abs)) {
            radians = 0.0;
        } else if (radians > 0) {
            radians = abs;
        } else {
            radians = twoPI - abs;
        }

        return radians;
    }

    /**
     * Convert a string to a double, returning 0.0 if the string is not
     * formatted properly.
     */
    public static double stringToAngle(String angle) {
        double val = 0;
        try {
            val = Double.valueOf(angle).doubleValue();
        } catch (NumberFormatException ex) {
        }
        return val;
    }

    /*
     * Rotate the point p by the negative of the given angle (-angle) in radians about (0, 0).
     * Equivalent to executing an AffineTransform.getRotateInstance(-angle).
     */
    public static void rotatePoint(Point2D.Double p, double angle) {
        if (angle != 0.0) {
            double cosa = Math.cos(angle);
            double sina = Math.sin(angle);
            double tmp = p.x;
            p.x = p.x * cosa + p.y * sina;
            p.y = -tmp * sina + p.y * cosa;
        }
    }
}

