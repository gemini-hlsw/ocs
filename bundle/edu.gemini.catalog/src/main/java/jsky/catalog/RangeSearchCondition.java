// Copyright 2003
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: RangeSearchCondition.java 47100 2012-07-31 21:10:41Z swalker $

package jsky.catalog;

import jsky.util.StringUtil;


/**
 * Represents a search condition for a range of values.
 */
public class RangeSearchCondition extends AbstractSearchCondition {

    // The minimum value
    private Comparable _minVal;

    // The maximum value
    private Comparable _maxVal;

    // True if the condition includes the min value
    private boolean _minInclusive = true;

    // True if the condition includes the max value
    private boolean _maxInclusive = true;


    /**
     * Create a new RangeSearchCondition where minVal <= x <= maxVal
     * for the given column or parameter description.
     */
    public RangeSearchCondition(FieldDesc fieldDesc, Comparable minVal, Comparable maxVal) {
        super(fieldDesc);
        _minVal = minVal;
        _maxVal = maxVal;
    }

    /**
     * Create a new RangeSearchCondition for the given column or parameter description,
     * where the "inclusive" parameters specify whether
     * the min and/or max values are included in the range.
     */
    public RangeSearchCondition(FieldDesc fieldDesc, Comparable minVal, boolean minInclusive,
                                Comparable maxVal, boolean maxInclusive) {
        super(fieldDesc);
        _minVal = minVal;
        _minInclusive = minInclusive;
        _maxVal = maxVal;
        _maxInclusive = maxInclusive;
    }

    /**
     * Create a new numerical RangeSearchCondition where (minVal <= x <= maxVal)
     * for the given column or parameter description.
     */
    public RangeSearchCondition(FieldDesc fieldDesc, double minVal, double maxVal) {
        this(fieldDesc, new Double(minVal), new Double(maxVal));
    }

    /**
     * Create a new String RangeSearchCondition where (minVal <= x <= maxVal)
     * for the given column or parameter description.
     */
    public RangeSearchCondition(FieldDesc fieldDesc, String minVal, String maxVal) {
        this(fieldDesc, (Comparable) minVal.trim(), (Comparable) maxVal.trim());
    }

    /** Return the minimum value (actually a Double or String) */
    public Comparable getMinVal() {
        return _minVal;
    }

    /** Return the maximum value (actually a Double or String) */
    public Comparable getMaxVal() {
        return _maxVal;
    }

    /** Return True if the condition includes the min value. */
    public boolean isMinInclusive() {
        return _minInclusive;
    }

    /** Return True if the condition includes the max value. */
    public boolean isMaxInclusive() {
        return _maxInclusive;
    }

    /**
     * Return true if the condition is true for the given value.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    public boolean isTrueFor(Comparable val) {
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

        if (_minInclusive && !_maxInclusive) {
            return ((_minVal == null || _minVal.compareTo(val) <= 0)
                    && (_maxVal == null || _maxVal.compareTo(val) > 0));
        }

        if (!_minInclusive && _maxInclusive) {
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
    public boolean isTrueFor(double val) {
        return isTrueFor(new Double(val));
    }


    /** Return true if this object represents a range. */
    public boolean isRange() {
        return (_minVal != _maxVal);
    }


    /** Return the value as a String in the format "minVal,maxVal" or just "minVal", if minVal=maxVal. */
    public String getValueAsString(String sep) {
        if (_minVal == _maxVal) {
            if (_minVal == null) {
                return "";
            }
            return _minVal.toString();
        }
        String minVal = "";
        if (_minVal != null) {
            minVal = _minVal.toString();
        }
        String maxVal = "";
        if (_maxVal != null) {
            maxVal = _maxVal.toString();
        }
        return minVal + sep + maxVal;
    }

    /**
     * Test cases
     */
    public static void main(String[] args) {
        RangeSearchCondition s = new RangeSearchCondition(new FieldDescAdapter("X"), 0, 1);

        if (!s.isTrueFor(0.5))
            throw new RuntimeException("test failed for 0.5: " + s);
        if (s.isTrueFor(1.5))
            throw new RuntimeException("test failed for 1.5: " + s);

        s = new RangeSearchCondition(new FieldDescAdapter("S"), "aaa", "kkk");
        if (!s.isTrueFor("bbb"))
            throw new RuntimeException("test failed for \"bbb\": " + s);
        if (s.isTrueFor("mmm"))
            throw new RuntimeException("test failed for \"mmm\": " + s);

        System.out.println("All tests passed");
    }
}





