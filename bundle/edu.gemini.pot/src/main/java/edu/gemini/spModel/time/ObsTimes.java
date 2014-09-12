/**
 * $Id: ObsTimes.java 6306 2005-06-05 22:41:34Z shane $
 */

package edu.gemini.spModel.time;

import java.io.Serializable;

/**
 * A record of the total execution time together with an {@link ObsTimeCharges}
 * instance that indicates how the total time should be split amongst the
 * various {@link ChargeClass}es.
 */
public final class ObsTimes implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * An ObsTimes instance in which all the times are <code>0</code>.  This
     * is suitable, for example, for an observation which has not been
     * executed. 
     */
    public static final ObsTimes ZERO_TIMES = new ObsTimes(0, ObsTimeCharges.ZERO_CHARGES);

    private long _totalTime;
    private ObsTimeCharges _charges;

    public ObsTimes(long totalTime, ObsTimeCharges charges) {
        _totalTime = totalTime;
        _charges   = charges;
    }

    public long getTotalTime() {
        return _totalTime;
    }

    public ObsTimeCharges getTimeCharges() {
        return _charges;
    }
}
