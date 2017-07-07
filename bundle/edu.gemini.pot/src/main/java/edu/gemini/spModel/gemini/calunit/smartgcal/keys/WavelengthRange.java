// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id:$
//
package edu.gemini.spModel.gemini.calunit.smartgcal.keys;

import java.io.Serializable;
import java.util.*;

/**
 * Immutable representation of wavelength ranges with inclusive minimal and exclusive maximal value [min,max).
 * These ranges are used in central wavelength maps. Since there is only a small number of actually different ranges,
 * already existing ranges are reused in order to keep memory consumption low (similar to how java strings are
 * reused, a range [500-650) can be defined in the calibration tables several hundred times but there will be only
 * one object representing that range). This brings the number of range objects down from several hundred thousands
 * to a few hundred (450'000 to 200).
 */
public final class WavelengthRange implements Serializable {

    public static final String RANGE_ANY_STRING = "any";

    // keep a set of disjoint range objects for recycling
    private static Map<WavelengthRange, WavelengthRange> rangeCache = new HashMap<WavelengthRange, WavelengthRange>();

    private static synchronized WavelengthRange matching(WavelengthRange wr) {
        if (!rangeCache.containsKey(wr)) rangeCache.put(wr, wr);
        return rangeCache.get(wr);
    }

    // the members
    private final int hashCode;
    private final double min;
    private final double max;

    /**
     * Parses a string and creates a wavelength range object for it.
     * Allowed is any string with two numbers seperated by a dash, e.g. 100 - 200.
     * The range will be defined as including the lower boundary and excluding the upper boundary.
     * @param rangeString
     * @return
     */
    public static WavelengthRange parse(String rangeString) {

        // --- step one: parse the string and on success create a new range

        WavelengthRange newRange;
        // range "any" -> [0.0,MAX_VALUE)
        if (RANGE_ANY_STRING.equalsIgnoreCase(rangeString)) {
            newRange = new WavelengthRange(
                    0.0d,
                    Double.MAX_VALUE
            );
        // range "x - y" -> [x,y)
        } else {
            if (rangeString == null || rangeString.isEmpty()) {
                throw new IllegalArgumentException("invalid range (empty string)");
            }
            StringTokenizer tokenizer = new StringTokenizer(rangeString, "-");
            if (tokenizer.countTokens() != 2) {
                throw new IllegalArgumentException("invalid range (can not parse) " + rangeString);
            }
            newRange = new WavelengthRange(
                    Double.parseDouble(tokenizer.nextToken()),
                    Double.parseDouble(tokenizer.nextToken())
            );
        }

        return matching(newRange);
    }

    /**
     * Constructs a  new wavelength range objects.
     * @param min
     * @param max
     */
    public WavelengthRange(double min, double max) {
        if (min >= max) {
            throw new IllegalArgumentException("invalid range (min>=max) " + toString());
        }
        this.min = min;
        this.max = max;
        this.hashCode = calculateHashCode();
    }

    /**
     * Gets the lower boundary.
     */
    public double getMin() { return this.min; }

    /**
     * Gets the upper boundary.
     */
    public double getMax() { return this.max; }

    public boolean overlaps(final WavelengthRange that) {
        return min < that.max && max >= that.min;
    }

    public boolean contains(double val) {
        return val >= min && val < max;
    }


    @Override
    public String toString() { return "[" + min + "," + max + ")"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WavelengthRange that = (WavelengthRange) o;

        if (Double.compare(that.max, max) != 0) return false;
        return Double.compare(that.min, min) == 0;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int calculateHashCode() {
        int result;
        long temp;
        temp = min != +0.0d ? Double.doubleToLongBits(min) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = max != +0.0d ? Double.doubleToLongBits(max) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
