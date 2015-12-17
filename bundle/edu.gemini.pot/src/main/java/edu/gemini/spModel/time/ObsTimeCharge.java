package edu.gemini.spModel.time;

import java.io.Serializable;

/**
 * An association of a long time value and a {@link ChargeClass}.  This is an
 * immutable value object.
 */
public final class ObsTimeCharge implements Comparable<ObsTimeCharge>, Serializable {
    private static final long serialVersionUID = 1L;

    private long _time;
    private ChargeClass _class;

    /**
     * Constructs with the time value and its {@link ChargeClass}.
     *
     * @param time time amount in milliseconds
     * @param chargeClass class to associate with this time amount
     */
    public ObsTimeCharge(long time, ChargeClass chargeClass) {
        if (chargeClass == null) throw new NullPointerException();
        _time  = time;
        _class = chargeClass;
    }

    /**
     * Gets the time value as a long in milliseconds.
     */
    public long getTime() {
        return _time;
    }

    /**
     * Adds the given amount of time (milliseconds) to the ObsTimeCharge,
     * returning a new value with the new total amount of time.
     */
    public ObsTimeCharge addTime(long time) {
        return new ObsTimeCharge(_time + time, _class);
    }

    /**
     * Gets the charge class associated with the time.
     */
    public ChargeClass getChargeClass() {
        return _class;
    }

    @Override
    public int compareTo(ObsTimeCharge that) {
        int res = _class.compareTo(that._class);
        if (res != 0) return res;

        if (_time != that._time) {
            return (_time < that._time) ? -1 : 0;
        }
        return 0;
    }

    public boolean equals(Object other) {
        if (!(other instanceof ObsTimeCharge)) return false;

        ObsTimeCharge that = (ObsTimeCharge) other;
        if (_time != that._time) return false;
        if (_class != that._class) return false;

        return true;
    }

    public int hashCode() {
        int res = (int) (_time ^ (_time >>> 32));
        res = 37*res + _class.hashCode();
        return res;
    }
}
