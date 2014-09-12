//
// $Id: PlannedTimeSummary.java 38186 2011-10-24 13:21:33Z swalker $
//

package edu.gemini.spModel.obs.plannedtime;

import java.io.Serializable;

/**
 * Value object holder for planned times.  Planned time is the anticipated
 * execution time of an observation.  There are two values for planned time
 * though, because the pi is only interested in the planned time that will be
 * charged to his program.  Planned "pi" time is the planned time charged to
 * a program whereas planned "exec" time is just the guess for the amount of
 * exec time that will be required.
 *
 * <p>This class contains a simple summary of total planned times for
 * an observation.  It is ultimately cached in the obs cach and therefore
 * kept simple and small.  More detail is available via {@link PlannedTime}</p>
 */
public final class PlannedTimeSummary implements Serializable {
    private static final long serialVersionUID = 1l;

    /**
     * A constant PlannedTime with 0 pi and exec times.
     */
    public static final PlannedTimeSummary ZERO_PLANNED_TIME = new PlannedTimeSummary(0, 0);

    private long _piTime;
    private long _execTime;

    /**
     * Constructs with the planned time to be charged to the PI and the total
     * anticipated elapsed time.
     *
     * @param piTime planned pi time (in milliseconds)
     * @param execTime planned exec time (in milliseconds)
     */
    public PlannedTimeSummary(long piTime, long execTime) {
        _piTime   = piTime;
        _execTime = execTime;
    }

    /**
     * Support for the original format for planned time -- as a double in
     * seconds.
     *
     * @param piTime planned pi time (in seconds)
     * @param execTime planned exec time (in seconds)
     */
    public PlannedTimeSummary(double piTime, double execTime) {
        _piTime = Math.round(piTime * 1000.0);
        _execTime = Math.round(execTime * 1000.0);
    }

    /**
     * Gets the planned time that will be charged to the PI (in milliseconds).
     */
    public long getPiTime() {
        return _piTime;
    }

    /**
     * Gets the anticipated elapsed time (in milliseconds).
     */
    public long getExecTime() {
        return _execTime;
    }

    /**
     * Sums this PlannedTime object with the given one, returning a new
     * PlannedTime object with the total of the two.  This PlannedTime and the
     * given one are not modified.
     */
    public PlannedTimeSummary sum(PlannedTimeSummary that) {
        return new PlannedTimeSummary(_piTime + that._piTime, _execTime + that._execTime);
    }

    public boolean equals(Object o) {
        if (!(o instanceof PlannedTimeSummary)) return false;

        PlannedTimeSummary that = (PlannedTimeSummary) o;

        if (_piTime != that._piTime) return false;

        //noinspection RedundantIfStatement
        if (_execTime != that._execTime) return false;

        return true;
    }

    public int hashCode() {
        int res = (int)(_piTime^(_piTime>>>32));
        res = 37*res + (int)(_execTime^(_execTime>>>32));
        return res;
    }
}
