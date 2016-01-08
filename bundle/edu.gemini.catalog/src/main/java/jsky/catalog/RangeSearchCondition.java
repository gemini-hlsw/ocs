package jsky.catalog;

import jsky.util.StringUtil;


/**
 * Represents a search condition for a range of values.
 */
public class RangeSearchCondition extends AbstractSearchCondition {

    // The minimum value
    private final Comparable _minVal;

    // The maximum value
    private final Comparable _maxVal;

    // True if the condition includes the min value
    private final boolean _minInclusive;

    // True if the condition includes the max value
    private final boolean _maxInclusive;


    /**
     * Create a new RangeSearchCondition where minVal <= x <= maxVal
     * for the given column or parameter description.
     */
    public RangeSearchCondition(final FieldDesc fieldDesc, final Comparable minVal, final Comparable maxVal) {
        super(fieldDesc);
        _minVal = minVal;
        _minInclusive = true;
        _maxVal = maxVal;
        _maxInclusive = true;
    }

    /**
     * Create a new RangeSearchCondition for the given column or parameter description,
     * where the "inclusive" parameters specify whether
     * the min and/or max values are included in the range.
     */
    public RangeSearchCondition(final FieldDesc fieldDesc,
                                final Comparable minVal, final boolean minInclusive,
                                final Comparable maxVal, final boolean maxInclusive) {
        super(fieldDesc);
        _minVal = minVal;
        _minInclusive = minInclusive;
        _maxVal = maxVal;
        _maxInclusive = maxInclusive;
    }

    /** Return the minimum value (actually a Double or String) */
    public Comparable getMinVal() {
        return _minVal;
    }

    /** Return the maximum value (actually a Double or String) */
    public Comparable getMaxVal() {
        return _maxVal;
    }

    /**
     * Return true if the condition is true for the given value.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    @SuppressWarnings("unchecked")
    public boolean isTrueFor(final Comparable val) {
        if (val == null) return false;

        if (_minVal != null && !_minVal.getClass().equals(val.getClass())
            || (_maxVal != null && !_maxVal.getClass().equals(val.getClass()))) {
            return false;
        }

        if (_minVal == _maxVal) {
            if (_minVal instanceof String && val instanceof String) {
                return StringUtil.match((String) _minVal, (String) val);
            }
            return (_minVal == null || _minVal.compareTo(val) == 0);
        }

        if (_minInclusive && _maxInclusive) {
            return ((_minVal == null || _minVal.compareTo(val) <= 0)
                    && (_maxVal == null || _maxVal.compareTo(val) >= 0));
        }

        if (_minInclusive) {
            return ((_minVal == null || _minVal.compareTo(val) <= 0)
                    && (_maxVal == null || _maxVal.compareTo(val) > 0));
        }

        if (_maxInclusive) {
            return ((_minVal == null || _minVal.compareTo(val) < 0)
                    && (_maxVal == null || _maxVal.compareTo(val) >= 0));
        }

        return ((_minVal == null || _minVal.compareTo(val) < 0)
                && (_maxVal == null || _maxVal.compareTo(val) > 0));
    }


    /**
     * Return true if the condition is true for the given numeric value.
     * If the condition was specified as a String, the return value is false.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    public boolean isTrueFor(final double val) {
        return isTrueFor(new Double(val));
    }


    /** Return true if this object represents a range. */
    public boolean isRange() {
        return (_minVal != _maxVal);
    }


    /** Return the value as a String in the format "minVal,maxVal" or just "minVal", if minVal=maxVal. */
    public String getValueAsString(final String sep) {
        if (_minVal == _maxVal) {
            return _minVal == null ? "" : _minVal.toString();
        }
        final String minVal = _minVal == null ? "" : _minVal.toString();
        final String maxVal = _maxVal == null ? "" : _maxVal.toString();
        return minVal + sep + maxVal;
    }
}





