package jsky.catalog;

import java.io.Serializable;

/**
 * Represents a range of values, given by minValue and maxValue,
 * where minValue <= x <= maxValue.
 */
public class ValueRange implements Serializable {

    /** The minimum value */
    Comparable minValue;

    /** The maximum value */
    Comparable maxValue;

    /** True if the range includes the min value */
    boolean minInclusive = true;

    /** True if the range includes the max value */
    boolean maxInclusive = true;

    /** Initialize from the given value (tests for equality) */
    public ValueRange(Comparable value) {
        this.minValue = this.maxValue = value;
    }

    /**
     * Initialize from the given min and max values and the flags.
     * @param minValue the minimum value.
     * @param minInclusive true if the range includes the min value.
     * @param maxValue the maximum value.
     * @param maxInclusive true if the range includes the max value.
     */
    public ValueRange(Comparable minValue, boolean minInclusive,
                      Comparable maxValue, boolean maxInclusive) {
        this.minValue = minValue;
        this.minInclusive = minInclusive;
        this.maxValue = maxValue;
        this.maxInclusive = maxInclusive;
    }

    /** Return the minimum value. */
    public Comparable getMinValue() {
        return minValue;
    }

    /** Return the maximum value. */
    public Comparable getMaxValue() {
        return maxValue;
    }

    /** Return True if the range includes the min value. */
    public boolean isMinInclusive() {
        return minInclusive;
    }

    /** Return True if the range includes the max value. */
    public boolean isMaxInclusive() {
        return maxInclusive;
    }

    public String toString() {
        if (minValue.equals(maxValue)) {
            return minValue.toString();
        }
        return minValue.toString() + "," + maxValue.toString();
    }
}



