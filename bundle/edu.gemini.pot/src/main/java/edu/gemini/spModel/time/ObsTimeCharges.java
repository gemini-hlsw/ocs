//
// $Id: ObsTimeCharges.java 7011 2006-05-04 16:12:21Z shane $
//

package edu.gemini.spModel.time;

import java.io.Serializable;

/**
 * A collection of time values (in milliseconds) and their associated
 * {@link ChargeClass}.  This is an immutable value object.
 */
public final class ObsTimeCharges implements Comparable, Serializable {
    private static final long serialVersionUID = 1l;

    private long[] _times = new long[ChargeClass.values().length];

    /**
     * A constant reference to an ObsTimeCharges instance with zero
     * time charges.
     */
    public static final ObsTimeCharges ZERO_CHARGES = new ObsTimeCharges();

    /**
     * Constructs with all charges equal to <code>0</code>.
     */
    public ObsTimeCharges() {
    }

    /**
     * Constructs with the given set of {@link ObsTimeCharge} values.  If any
     * {@link ChargeClass} is represented more than once in the
     * <code>charges</code> array, only the last value is used.  Any
     * {@link ChargeClass}es not represented in <code>charges</code> are set to
     * zero milliseconds.
     */
    public ObsTimeCharges(ObsTimeCharge[] charges) {
        if (charges == null) return;

        for (ObsTimeCharge c : charges) {
            _times[c.getChargeClass().ordinal()] = c.getTime();
        }
    }

    /**
     * Gets the amount of time (as a long in milliseconds) associated with the
     * given {@link ChargeClass}.
     */
    public long getTime(ChargeClass chargeClass) {
        return _times[chargeClass.ordinal()];
    }

    /**
     * Gets the {@link ObsTimeCharge} associated with the given
     * <code>chargeClass</code>.
     */
    public ObsTimeCharge getTimeCharge(ChargeClass chargeClass) {
        return new ObsTimeCharge(getTime(chargeClass), chargeClass);
    }

    /**
     * Creates a new instance of ObsTimeCharges that is the same as this
     * instance, except that the time for <code>chargeClass</code> is
     * increased (or decreased) by the number of milliseconds
     * specified in the  <code>time</code> parameter.
     *
     * @param time amount of time in milliseconds that should be added to the
     * specified <code>chargeClass</code>
     * @param chargeClass observation time charge class that should be
     * increased (or decreased)
     *
     * @return a new ObsTimeCharges instance whose time for
     * <code>chargeClass</code> has been increased or decreased by
     * <code>time</code> milliseconds
     */
    public ObsTimeCharges addTime(long time, ChargeClass chargeClass) {
        ObsTimeCharges res = new ObsTimeCharges();

        System.arraycopy(_times, 0, res._times, 0, _times.length);
        res._times[chargeClass.ordinal()] += time;
        return res;
    }

    /**
     * Creates a new instance of ObsTimeCharges where the time amount
     * associated with the {@link ChargeClass} of <code>charge</code> has been
     * adjusted by the time amount of <code>charge</code>.
     *
     * @param charge time charge to apply to this instance
     *
     * @return a new ObsTimeCharges instance whose time for
     * <code>charge.getChargeClass()</code> has been increased or decreased by
     * the amount of time represented by <code>charge</code>
     */
    public ObsTimeCharges addTimeCharge(ObsTimeCharge charge) {
        return addTime(charge.getTime(), charge.getChargeClass());
    }

    /**
     * Creates a new ObsTimeCharges instance in which all the time amounts have
     * been adjusted by the corresponding values in <code>charges</code>.
     * For example, the amount for the {@link ChargeClass} PROGRAM is
     * increased or decreased accoring to <code>charges</code> time amount
     * for the PROGRAM charge class.
     *
     * @return a new ObsTimeCharges instance whose times are the result of
     * adding corresponding time amounts in this instance and in
     * <code>charges</code>
     */
    public ObsTimeCharges addTimeCharges(ObsTimeCharges charges) {
        ObsTimeCharges res = new ObsTimeCharges();
        for (int i=0; i<_times.length; ++i) {
            res._times[i] = _times[i] + charges._times[i];
        }
        return res;
    }

    public int compareTo(Object o) {
        ObsTimeCharges that = (ObsTimeCharges) o;
        for (int i=0; i<_times.length; ++i) {
            long thisTime = _times[i];
            long thatTime = that._times[i];
            if (thisTime != thatTime) {
                return thisTime < thatTime ? -1 : 0;
            }
        }
        return 0;
    }

    public boolean equals(Object o) {
        if (!(o instanceof ObsTimeCharges)) return false;
        ObsTimeCharges that = (ObsTimeCharges) o;

        for (int i=0; i<_times.length; ++i) {
            if (_times[i] != that._times[i]) return false;
        }
        return true;
    }

    public int hashCode() {
        int res = 0;

        for (int i=0; i<_times.length; ++i) {
            long val = _times[i];
            res = 37*res + (int) (val ^ (val>>>32));
        }

        return res;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("ObsTimesCharges [");
        for (ChargeClass cc : ChargeClass.values()) {
            buf.append(cc.name()).append(" = ").append(_times[cc.ordinal()]).append(", ");
        }
        buf.replace(buf.length()-2, buf.length()-1, "]");

        return buf.toString();
    }
}
