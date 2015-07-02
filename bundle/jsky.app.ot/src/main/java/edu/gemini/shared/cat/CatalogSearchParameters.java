package edu.gemini.shared.cat;

import edu.gemini.catalog.api.MagnitudeLimits;
import edu.gemini.catalog.api.RadiusLimits;
import edu.gemini.skycalc.Angle;
import edu.gemini.shared.skyobject.Magnitude;

/**
 * Parameters used to define a catalog search's options.
 */
public final class CatalogSearchParameters implements Cloneable {

    private static final double DEFAULT_MIN_RADIUS = 0.0;

    private static final double DEFAULT_MAX_RADIUS = 10.0;

    private static final double DEFAULT_BRIGHT_LIMIT = -2.0;

    private static final double DEFAULT_FAINT_LIMIT = 20.0;

    private static final int DEFAULT_RESULTS_LIMIT = 1000;

    private static final Magnitude.Band DEFAULT_BAND = Magnitude.Band.R;

    private RadiusLimits _radiusLimit = new RadiusLimits(new Angle(DEFAULT_MAX_RADIUS, Angle.Unit.ARCMINS), new Angle(DEFAULT_MIN_RADIUS, Angle.Unit.ARCMINS));

    private MagnitudeLimits _magLimit =
            new MagnitudeLimits(
                    DEFAULT_BAND,
                    new MagnitudeLimits.FaintnessLimit(DEFAULT_FAINT_LIMIT),
                    new MagnitudeLimits.SaturationLimit(DEFAULT_BRIGHT_LIMIT));

    private int _numStars = DEFAULT_RESULTS_LIMIT;

    /**
     * Provides clone support.
     */
    public Object clone() {
        CatalogSearchParameters result;
        try {
            result = (CatalogSearchParameters) super.clone();
        } catch (CloneNotSupportedException ex) {
            // Ain't gonna happen.
            System.err.println("BUG: clone() called on " + getClass().getName() + " but clone() is not supported.");
            System.exit(-1);
            return null; // fool the compiler
        }

        // All fields are primitives, so nothing more to do.
        return result;
    }

    /**
     * Gets the limit on the number of stars returned.
     */
    public int getResultsLimit() {
        return _numStars;
    }

    /**
     * Set search radius limits. This is essentially a flat donut.
     *
     * @param radiusLimit search radiu limit
     */
    public void setRadiusLimits(RadiusLimits radiusLimit) {
        _radiusLimit=radiusLimit;
    }

    /**
     * Return the radius limit
     *
     * @return
     */
    public RadiusLimits getRadiusLimits() {
        return _radiusLimit;
    }

    /**
     * Sets the magnitude limkits for the search. Parameter contains min and max magnitude and a Band.
     *
     * @param magLimit
     */
    public void setMagnitudeLimits(MagnitudeLimits magLimit) {
        _magLimit=magLimit;
    }

    /**
     * Return teh magnitude limits for the search, including the band on which it applies.
     *
     * @return
     */
    public MagnitudeLimits getMagnitudeLimits() {
        return _magLimit;
    }

    @Override
    public String toString() {
        return "CatalogSearchParameters: rm=" + _radiusLimit.getMinLimit() + "/" + _radiusLimit.getMaxLimit()
                + ", mag=" + _magLimit.getSaturationLimit() + ".." + _magLimit.getFaintnessLimit()
                + ", num=" + getResultsLimit();
    }
}
