package edu.gemini.qpt.core.util;

import edu.gemini.spModel.core.Angle;
import edu.gemini.spModel.core.Angle$;

/**
 * Airmass limits used throughout the QPT.
 */
public enum AirmassLimit {
    WARNING(2.0, 30.000),
    ERROR  (2.3, 25.625),
    ;

    /** Airmass limit above which the warning or error is triggered. */
    public final double airmass;

    /** Corresponding elevation angle. */
    public final Angle elevation;

    private AirmassLimit(double airmass, double elevationDeg) {
        this.airmass   = airmass;
        this.elevation = Angle$.MODULE$.fromDegrees(elevationDeg);
    }
}
