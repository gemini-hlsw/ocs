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
public enum TwilightBoundType implements Serializable {

    OFFICIAL("Official", 50.0/60.0),

    CIVIL("Civil", 6.0),

    NAUTICAL("Nautical", 12.0),

    ASTRONOMICAL("Astronomical", 18.0)
    ;

    private final String _name;
    private final double _horizAngle; // angle below the horizon

    TwilightBoundType(String name, double horizonAngle) {
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

}
