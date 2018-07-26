package edu.gemini.qpt.core.util;

import edu.gemini.spModel.core.Site;
import edu.gemini.skycalc.TwilightBoundedNight;
import edu.gemini.skycalc.TwilightBoundType;

/**
 * Twilight night bound calculations used for block limits. Note, we display
 * nautical twilight bounds in the visualizer and property sheet, but the
 * twilight calculations are based on civil twilight.
 */
public final class Twilight {
    private Twilight() {}

    /** Definition of twilight in use for the QPT. */
    public static final TwilightBoundType TYPE = TwilightBoundType.CIVIL;

    /**
     * Computes the twilight bounds for the night starting on the date of the
     * given time.  For example, at midnight October 8 that would be the night
     * October 8/9, whereas for 11:59:59 October 7 it would be the night of
     * October 7/8.
     */
    public static TwilightBoundedNight startingOnDate(long time, Site site) {
        return new TwilightBoundedNight(TYPE, time, site);
    }

    /**
     * Computes the twilight bounds corresponding to the given time.  If the
     * time falls within twilight bounds, the same night is returned.  On the
     * other hand if the time falls during daytime, it returns the coming night.
     *
     * For example, at midnight October 8, this will return the night of
     * October 7/8 (the same for 11:59:59 October 7).
     */
    public static TwilightBoundedNight forTime(long time, Site site) {
        return TwilightBoundedNight.forTime(TYPE, time, site);
    }

}
