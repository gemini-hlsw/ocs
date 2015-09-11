// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: AngleMath.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.system;

import edu.gemini.spModel.target.system.CoordinateParam.Units;

/**
 * A utility class for mathmatical operations using angles.
 * <p>
 * The static methods within this class provide support for
 * common operations in the target
 */
public final class AngleMath {
    private static double _ERROR = 0.000001;

    /**
     * Convert from degrees to radians.
     */
    public static double degreesToRadians(double degrees) {
        return Math.toRadians(degrees);
    }

    /**
     * Convert from radians to degrees.
     */
    public static double radiansToDegrees(double radians) {
        return Math.toDegrees(radians);
    }

    /**
     * Convert hours of time to degrees.
     */
    public static double hoursToDegrees(double hours) {
        return hours * 15.0;
    }

    /**
     * Convert degrees to hours of time.
     */
    public static double degreesToHours(double degrees) {
        return degrees / 15.0;
    }

    /**
     * Convert hours of time to radians.
     */
    public static double hoursToRadians(double hours) {
        return degreesToRadians(hoursToDegrees(hours));
    }


    /**
     * Convert radians to hours of time.
     */
    public static double radiansToHours(double radians) {
        return degreesToHours(radiansToDegrees(radians));
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

    /**
     * Given an arbitrary angle (in degrees), return an equivalent angle in
     * degrees between 0 (inclusive) and 360 (exclusive).
     * From SEA Coordinates.java - Jeremy Jones
     */
    public static double normalizeRa(double in) {
        double out = in % 360.0;         // Must be >= 0.0 and < 360.0 Degrees
        if (out < 0.0) out += 360.0;     // must be positive too

        if (almostZeroDegrees(out)) {
            return 0.0;
        }
        return out;
    }

    /**
     * Given an arbitrary angle (in degrees), return an equivalent angle in
     * degrees between 90 (inclusive) and -90 (inclusive).
     * From SEA Coordinates.java - Jeremy Jones
     */
    public static double normalizeDec(double in) {
        double out = in % 360.0;            // Want a manageable angle

        if (out < 0.0) out += 360.0;        // Make negative positive

        if (out > 270.0) {
            out -= 360.0;
        } else if (out > 90.0) {
            out = 180.0 - out;
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

    /**
     * Convert a coordinate in degrees to some other units specified by
     * a <code>CoordinateParam.Unit</code> instance.
     */
    public static double convertFromDegrees(double in, Units outUnits) {
        if (outUnits == Units.DEGREES) return in;

        if (outUnits == Units.ARCSECS) {
            return in * 3600.0;
        } else if (outUnits == Units.RADIANS) {
            return AngleMath.degreesToRadians(in);
        } else if (outUnits == Units.HMS) {
            return AngleMath.degreesToHours(in);
        }
        System.out.println("[Convert->unknown units: " + outUnits + "]");
        return Double.NaN;
    }

    /**
     * Convert a coordinate from some other units to degrees.
     * The units are specified with an object of type
     * a <code>CoordinateParam.Unit</code> instance.
     */
    public static double convertToDegrees(double in, Units inUnits) {
        if (inUnits == Units.DEGREES) return in;

        if (inUnits == Units.ARCSECS) {
            return in / 3600.0;
        } else if (inUnits == Units.RADIANS) {
            return AngleMath.radiansToDegrees(in);
        } else if (inUnits == Units.HMS) {
            return AngleMath.hoursToDegrees(in);
        }
        System.out.println("[Convert->unknown units: " + inUnits + "]");
        return Double.NaN;
    }

    /**
     * Convert a coordinate (as a double) from one type of units to another.
     * The units are specified by <code>CoordinateParam.Unit</code> instances.
     * This is based upon Jeremy Jones' convert in SEA.
     */
    public static double convert(double in, Units inUnits, Units outUnits) {
        if (inUnits == outUnits) return in;

        if (inUnits == Units.DEGREES) {
            return AngleMath.convertFromDegrees(in, outUnits);
        }
        return in;
    }
}

