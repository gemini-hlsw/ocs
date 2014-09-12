//
// $Id: TwilightBoundType.java 6519 2005-07-24 00:39:18Z shane $
//

package edu.gemini.skycalc;

import java.io.Serializable;

/**
 * Definition for how the range from sunset to sunrise should be defined for
 * a night.  There are various standard options for definition where the night
 * begins and ends which are represented as static constants in side this
 * class.
 */
public final class TwilightBoundType implements Comparable, Serializable {

    private static final long serialVersionUID = 1;

    public static final TwilightBoundType OFFICIAL =
            new TwilightBoundType("Official", 50.0/60.0);

    public static final TwilightBoundType CIVIL =
            new TwilightBoundType("Civil", 6.0);

    public static final TwilightBoundType NAUTICAL =
            new TwilightBoundType("Nautical", 12.0);

    public static final TwilightBoundType ASTRONOMICAL =
            new TwilightBoundType("Astronomical", 18.0);

    private String _name;
    private double _horizAngle; // angle below the horizon

    public TwilightBoundType(String name, double horizonAngle) {
        if (name == null) throw new NullPointerException();

        _name   = name;
        _horizAngle = horizonAngle;
    }

    public String getName() {
        return _name;
    }

    /**
     * Gets the angle below the horizon associated with this type of twilight.
     */
    public double getHorizonAngle() {
        return _horizAngle;
    }

    public boolean equals(Object o) {
        if (!(o instanceof TwilightBoundType)) return false;

        TwilightBoundType that = (TwilightBoundType) o;

        if (!_name.equals(that._name)) return false;
        if (_horizAngle != that._horizAngle) return false;

        return true;
    }

    public int hashCode() {
        int res = _name.hashCode();

        long v = Double.doubleToLongBits(_horizAngle);
        res = 37*res + (int)(v^(v>>>32));

        return res;
    }

    /**
     * Sorts first by angle below the horizon, then by name.
     */
    public int compareTo(Object o) {
        TwilightBoundType that = (TwilightBoundType) o;
        double diff = _horizAngle - that._horizAngle;
        if (diff != 0) {
            return (diff < 0) ? -1 : 1;
        }
        return _name.compareTo(that._name);
    }
}
