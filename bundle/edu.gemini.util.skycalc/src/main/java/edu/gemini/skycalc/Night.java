//
// $Id: Night.java 6519 2005-07-24 00:39:18Z shane $
//

package edu.gemini.skycalc;

import edu.gemini.spModel.core.Site;

import java.io.Serializable;

/**
 * The start and end of a particular night at a particular location.
 */
public interface Night extends Serializable {

    /**
     * Gets the location at which the times described by this night are valid.
     *
     * @return description of the site associated with this night information
     */
    Site getSite();

    /**
     * Gets the date at the start of the night (inclusive).  For example,
     *
     * <pre>
     * Night n = ...
     * if (n.includes(n.getStartTime())) { // always true
     * </pre>
     */
    long getStartTime();

    /**
     * Gets the date at the end of the night (exclusive).  For example,
     *
     * <pre>
     * Night n = ...
     * if (n.includes(n.getEndTime())) { // always false
     * </pre>
     */
    long getEndTime();

    /**
     * Gets the duration, in milliseconds of the night.
     */
    long getTotalTime();

    /**
     * Returns <code>true</code> if the given time falls within the bounds of
     * the night.  In other words, if <pre>start <= time < end</pre>.
     */
    boolean includes(long time);

    /**
     * Creates an interval for the start and end of the night.
     */
    default Interval toInterval() {
        return new Interval(getStartTime(), getEndTime());
    }
}
