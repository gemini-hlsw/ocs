//
// $Id: ObsTimeCorrection.java 6986 2006-05-01 17:05:49Z shane $
//

package edu.gemini.spModel.time;

import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.pio.*;

import java.io.Serializable;


/**
 * An (immutable) entry that indicates a single correction to be applied to the
 * automatically calculated observation time.  Each correction has an
 * reason comment and a timestamp.  This is a single entry in the
 * {@link ObsTimeCorrectionLog}.
 */
public final class ObsTimeCorrection implements Serializable, Comparable {

    private static final long serialVersionUID = 2;

    public static final ObsTimeCorrection[] EMPTY_ARRAY = new ObsTimeCorrection[0];

    public static final String PARAM_SET_NAME = "timeCorrection";
    public static final String TIMESTAMP_PARAM = "timestamp";
    public static final String TIME_VALUE_PARAM_SET    = "timeValue";
    public static final String TIME_VALUE_AMOUNT_PARAM = "amount";
    public static final String TIME_VALUE_UNITS_PARAM  = "units";
    public static final String CHARGE_CLASS_PARAM = "chargeClass";
    public static final String REASON_PARAM = "reason";


    // What time the adjustment was applied.
    private long _timestamp;

    // The adjustment to be made.
    private TimeValue _correction;

    // The charge class for the correction time
    private ChargeClass _chargeClass = ChargeClass.DEFAULT;

    // The reason why the adjustment was applied.
    private String _reason;

    /**
     * Creates a new ObsTimeCorrection from its contituent parts.
     *
     * @param correction the time correction to apply to the total time
     * observed for an observation
     *
     * @param timestamp the time at which this correction was created/applied
     *
     * @param chargeClass the charge class for the correction timef
     *
     * @param reason the reason that the user is making the correction
     */
    public ObsTimeCorrection(TimeValue correction, long timestamp, ChargeClass chargeClass,
                             String reason) {
        if (correction == null) throw new NullPointerException("missing correction");

        _timestamp  = timestamp;
        _correction = correction;
        _chargeClass = chargeClass;
        _reason     = reason;
    }

    public ObsTimeCorrection(ParamSet pset) throws PioParseException {
        _timestamp =  Pio.getLongValue(pset, TIMESTAMP_PARAM, -1);
        if (_timestamp == -1) {
            throw new PioParseException("missing timestamp");
        }

        ParamSet timeValuePset = pset.getParamSet(TIME_VALUE_PARAM_SET);
        if (timeValuePset == null) {
            throw new PioParseException("missing timeValue");
        }

        Param amountParam = timeValuePset.getParam(TIME_VALUE_AMOUNT_PARAM);
        if (amountParam == null) {
            throw new PioParseException("missing timeValue/amount");
        }
        long ms = Pio.getLongValue(timeValuePset, TIME_VALUE_AMOUNT_PARAM, 0);
        String unitsStr = Pio.getValue(timeValuePset, TIME_VALUE_UNITS_PARAM);
        if (unitsStr == null) {
            throw new PioParseException("missing timeValue/units");
        }
        TimeValue.Units units;
        try {
            units = TimeValue.Units.valueOf(unitsStr);
        } catch (Exception ex) {
            throw new PioParseException("unexpected timeValue/units: "+ unitsStr);
        }

        _correction = TimeValue.millisecondsToTimeValue(ms, units);

        String s = Pio.getValue(pset, CHARGE_CLASS_PARAM);
        if (s != null) {
            _chargeClass = ChargeClass.parseType(s);
        }

        _reason = Pio.getValue(pset, REASON_PARAM);
    }

    /**
     * Gets the time correction to make to the total time observed.
     */
    public TimeValue getCorrection() {
        return _correction;
    }

    /**
     * Gets the time at which the time correction was applied.
     */
    public long getTimestamp() {
        return _timestamp;
    }

    /**
     * Return the charge class for the time
     */
    public ChargeClass getChargeClass() {
        return _chargeClass;
    }

    /**
     * Gets the reason why the user decided to adjust the total time that was
     * automatically calculated (if any).  May be <code>null</code>.
     */
    public String getReason() {
        return _reason;
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet pset = factory.createParamSet(PARAM_SET_NAME);

        Pio.addLongParam(factory, pset, TIMESTAMP_PARAM, _timestamp);

        ParamSet timeValuePset = factory.createParamSet(TIME_VALUE_PARAM_SET);
        pset.addParamSet(timeValuePset);
        Pio.addLongParam(factory, timeValuePset, TIME_VALUE_AMOUNT_PARAM, _correction.getMilliseconds());
        Pio.addParam(factory, timeValuePset, TIME_VALUE_UNITS_PARAM, _correction.getTimeUnits().name());

        Pio.addParam(factory, pset, CHARGE_CLASS_PARAM, _chargeClass.name());

        if (_reason != null) {
            Pio.addParam(factory, pset, REASON_PARAM, _reason);
        }

        return pset;
    }

    public boolean equals(Object o) {
        if (!(o instanceof ObsTimeCorrection)) return false;

        ObsTimeCorrection that = (ObsTimeCorrection) o;

        if (_timestamp != that._timestamp) return false;
        if (!_correction.equals(that._correction)) return false;

        if (_chargeClass != that._chargeClass) return false;

        if (_reason == null) {
            if (that._reason != null) return false;
        } else {
            if (!_reason.equals(that._reason)) return false;
        }

        return true;
    }

    public int hashCode() {
        long bits = Double.doubleToLongBits(_timestamp);
        int res = (int) (bits ^ (bits >>> 32));

        res = 37*res + _chargeClass.hashCode();
        res = 37*res + _correction.hashCode();
        if (_reason != null) {
            res = 37*res + _reason.hashCode();
        }
        return res;
    }

    public int compareTo(Object o) {
        ObsTimeCorrection that = (ObsTimeCorrection) o;

        if (_timestamp != that._timestamp) {
            return _timestamp < that._timestamp ? -1 : 1;
        }

        int res = _correction.compareTo(that._correction);
        if (res != 0) return res;

        res = _chargeClass.compareTo(that._chargeClass);
        if (res != 0) return res;

        if (_reason == null) {
            if (that._reason != null) {
                res = -1;
            }
        } else if (that._reason == null) {
            res = 1;
        } else {
            res = _reason.compareTo(that._reason);
        }

        return res;
    }
}
