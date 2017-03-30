package edu.gemini.horizons.api;

import edu.gemini.spModel.core.Site;

import java.io.Serializable;
import java.util.Date;

/**
 * A description of a query to be executed in the Horizons service. A query is
 * executed in a {@link edu.gemini.horizons.api.IQueryExecutor}
 * @see edu.gemini.horizons.api.IQueryExecutor
 */
public final class HorizonsQuery implements Serializable {

    /**
     * Start date from where the ephemeris will be gotten
     */
    private Date _startDate;

    /**
     * End date from where the ephemeris will be gotten
     */
    private Date _endDate;

    /**
     * Name or id of the object
     */
    private String _objectId;

    /**
     * Step size, in _stepUnits increments. Default value is <code>60</code>.
     */
    private int _stepSize = 60;

    /**
     * Units for stepsize. Default value is <code>TIME_MINUTES</code>.
     */
    private StepUnits _stepUnits = StepUnits.TIME_MINUTES;

    /**
     * Defines the step unit, which may be time or space.
     */
    public enum StepUnits {

        TIME_MINUTES(1, Integer.MAX_VALUE, "m"),
        SPACE_ARCSECONDS(60, 3600, "VAR")
        ;

        /** Smallest allowed value for units of this type, inclusive. */
        public final int minValue;

        /** Latgest allowed value for units of this type, inclusive. */
        public final int maxValue;

        /** Used internally; clients should not reference this field. */
        // associated suffix for horizons queries
        public final String suffix;

        StepUnits(int minValue, int maxValue, String suffix) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.suffix = suffix;
        }

    }

    /**
     * Site configuration
     */
    private Site _site = Site.GN;


    /**
     * Default constructor
     */
    public HorizonsQuery(Site site) {
        _site = site;
    }

    /**
     * Set the site that must be used to
     * perform this query
     * @param site The site to perform the query
     */
    public void setSite(Site site) {
        if (site != null) {
            _site = site;
        }
    }
    /**
     * Specify the object to be queried. It could be an object name, an object Id or
     * even a part of an object name.
     * @param object Object id
     */
    public void setObjectId(String object) {
        _objectId = object;
    }

    /**
     * Defines the start date when we need ephemeris information for the particular object.
     * @param date Starting date for the ephemeris, if available
     */
    public void setStartDate(Date date)  {
        _startDate = date;
    }

    /**
     * Defines the end date when we need ephemeris information for the particular object.
     * @param date End date for the ephemeris, if available
     */

    public void setEndDate(Date date)  {
        _endDate = date;
    }

    /**
     * Define the step size and units. If unspecified, the default is 60 minutes (time).
     * Note that each StepUnits value imposes limits on allowed values for
     * <code>steps</code>; see the Javadoc for {@linkplain StepUnits} or use the
     * min/maxValue fields for runtime checks.<p>
     * If left unspecified, the default value of <code>60, TIME_MINUTES</code> will
     * be used.
     * @param size number of <code>units</code> to be used when getting ephemeris data
     * @param units units for the <code>size</code>
     */
    public synchronized void setSteps(int size, StepUnits units) {

        // Steps must be within correct range for the specified units, which may not be null.
        if (units == null)
            throw new IllegalArgumentException("Units may not be null.");
        if (size < units.minValue || size > units.maxValue)
            throw new IllegalArgumentException("Out of range: " + size + "; valid range for " + units + " is [" + units.minValue + " .. " + units.maxValue + "]");

        _stepUnits = units;
        _stepSize = size;
    }

    /**
     * Get the site configuration for this query.
     * The site defines the geographical position from where the
     * data is valid.
     *
     * @return the Site configuration for this query
     */
    public Site getSite() {
        return _site;
    }

    /**
     * Get the start date for the ephemeris data
     * @return start date for the ephemeris data
     */
    public Date getStartDate() {
        return _startDate;
    }

    /**
     * Get the end date for the ephemeris data
     * @return end date for the ephemeris data
     */
    public Date getEndDate() {
        return _endDate;
    }

    /**
     * Get the step in stepUnits used to get the ephemeris information if available
     * @return the step in stepUnits used to get the ephemeris information
     */
    public synchronized int getStepSize() {
        return _stepSize;
    }

    /**
     * Get the step units (which will be applied to step size).
     */
    public synchronized StepUnits getStepUnits() {
        return _stepUnits;
    }

    /**
     * Get the current object being queried.
     * @return  object being queried. The string could represent an object Id, an
     * object name or even a part of an object name
     */
    public String getObjectId() {
        return _objectId;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HorizonsQuery)) return false;

        HorizonsQuery that = (HorizonsQuery)obj;

        if (getObjectId() == null) {
            if (that.getObjectId() != null) return false;
        } else {
            if (!getObjectId().equals(that.getObjectId())) return false;
        }

        if (getStartDate() == null) {
            if (that.getStartDate() != null) return false;
        } else {
            if (!getStartDate().equals(that.getStartDate())) return false;
        }

        if (getEndDate() == null) {
            if (that.getEndDate() != null) return false;
        } else {
            if (!getEndDate().equals(that.getEndDate())) return false;
        }

        return getStepSize() == that.getStepSize() && getStepUnits() == that.getStepUnits();
    }

    public int hashCode() {
        int hash = getStepSize() << 3 + getStepUnits().ordinal();

        if (getObjectId() != null) {
            hash = 31*hash + getObjectId().hashCode();
        }

        if (getStartDate() != null) {
            hash = 31*hash + getStartDate().hashCode();
        }

        if (getEndDate() != null) {
            hash = 31*hash + getEndDate().hashCode();
        }

        return hash;
    }

}