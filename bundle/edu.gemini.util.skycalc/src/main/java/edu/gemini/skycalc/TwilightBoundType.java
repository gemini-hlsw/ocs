package edu.gemini.skycalc;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Definition for how the range from sunset to sunrise should be defined for
 * a night.  There are various standard options for definition where the night
 * begins and ends which are represented as static constants in side this
 * class.
 */
public final class TwilightBoundType implements Comparable<TwilightBoundType>, Serializable {

    private static final long serialVersionUID = 1;

    // Horizon geometric correction from p. 24 of the Skycalc manual: sqrt(2 * elevation / Re) (radians)
    static double Re = 6378140; // Radius of the Earth (meters)
    static double SiteElevation = 4213; // Mauna Kea elevation (meters)

    public static final TwilightBoundType OFFICIAL =
            new TwilightBoundType("Official", 50.0/60.0 + Math.sqrt(2.0 * SiteElevation / Re) * 180./Math.PI);

    public static final TwilightBoundType CIVIL =
            new TwilightBoundType("Civil", 6.0);

    public static final TwilightBoundType NAUTICAL =
            new TwilightBoundType("Nautical", 12.0);

    public static final TwilightBoundType ASTRONOMICAL =
            new TwilightBoundType("Astronomical", 18.0);

    public static final List<TwilightBoundType> ALL =
            Collections.unmodifiableList(Arrays.asList(OFFICIAL, CIVIL, NAUTICAL, ASTRONOMICAL));

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
        return _horizAngle == that._horizAngle;
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
    @Override
    public int compareTo(final TwilightBoundType that) {
        final double diff = _horizAngle - that._horizAngle;
        if (diff != 0) {
            return (diff < 0) ? -1 : 1;
        }
        return _name.compareTo(that._name);
    }
}
