//
// $
//

package edu.gemini.spModel.timeacct;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.*;

/**
 * A collection of time allocations for the various
 * {@link TimeAcctCategory categories}.
 */
public final class TimeAcctAllocation implements Serializable {

    public static final TimeAcctAllocation EMPTY = new TimeAcctAllocation();

    private final Map<TimeAcctCategory, Double> allocMap;

    private transient double totalTime;

    private TimeAcctAllocation() {
        allocMap = Collections.emptyMap();
    }

    /**
     * Computes a TimeAcctAllocation from a map containing hours allocated for
     * each non-zero time accounting category.
     *
     * @param allocationMap hours for all relevant categories; hours must be
     * greater than or equal to 0
     */
    public TimeAcctAllocation(Map<TimeAcctCategory, Double> allocationMap) {
        final Map<TimeAcctCategory, Double> tmpAllocMap = new TreeMap<>(allocationMap);

        for (Map.Entry<TimeAcctCategory, Double> me : tmpAllocMap.entrySet()) {
            Double d = me.getValue();
            if (d == null) {
                String s;
                s = String.format("null time accounting allocation for category %s",
                        me.getKey().name());
                throw new IllegalArgumentException(s);
            }
            if (d < 0.0) {
                String s;
                s = String.format("negative time accounting allocation (%f) for category %s",
                        d, me.getKey().name());
                throw new IllegalArgumentException(s);
            }
            totalTime += d;
        }
        allocMap = Collections.unmodifiableMap(tmpAllocMap);
    }

    /**
     * Computes a TimeAcctAllocation from a total time and a ratio for each
     * category.  The total time is divided among the categories according to
     * the portion indicated in the ratioMap.
     *
     * @param totalHours total amount of time distributed across all categories
     * @param ratioMap fraction of time to allocate to each of the time
     * accounting categories; the sum of the values of the ratio map should
     * equal 1 (ignorning rounding errors in floating point math)
     *
     * @throws IllegalArgumentException if sum of fractions in
     * <code>ratioMap</code> does not equal 1
     */
    public TimeAcctAllocation(double totalHours, Map<TimeAcctCategory, Double> ratioMap) {
        final Map<TimeAcctCategory, Double> tmpAllocMap = new TreeMap<>();

        if (ratioMap.size() == 0) {
            // not clear how to award time, so the total time and allocation
            // map will be empty
            allocMap = Collections.emptyMap();
            return;
        }

        // Award a proportional amount of time to each category.
        for (Map.Entry<TimeAcctCategory, Double> me : ratioMap.entrySet()) {
            double hours = totalHours * me.getValue();
            tmpAllocMap.put(me.getKey(), hours);
            totalTime += hours;
        }

        // Make sure that the totalTime we computed matches the totalHours
        // provided this method.  In other words, verify that the ratioMap
        // was what we expected it to be, fractions adding up to 1.0.
        double zero = Math.abs(totalHours - totalTime);
        if (zero > 0.00001) {
            throw new IllegalArgumentException("ratioMap not valid");
        }

        allocMap = Collections.unmodifiableMap(tmpAllocMap);
    }


    /**
     * Gets the number of hours associated with the given category.
     */
    public double getHours(TimeAcctCategory category) {
        final Double res = allocMap.get(category);
        return (res == null) ? 0 : res;
    }

    /**
     * Gets the percentage of the total time allocation that is associated with
     * the given category.
     */
    public double getPercentage(TimeAcctCategory category) {
        if (totalTime == 0) return 0;
        double hours = getHours(category);
        return hours/totalTime * 100;
    }

    /**
     * Gets a collection of all the {@link TimeAcctCategory}s that have a
     * non-zero allocation.
     */
    public Set<TimeAcctCategory> getCategories() {
        return allocMap.keySet();
    }

    /**
     * Gets a map of {@link TimeAcctCategory} to the fraction of time that
     * should be associated with it.  For example, if half of the time spent
     * on a program should be attributed to the US, then the US category will
     * be 0.5.
     */
    public SortedMap<TimeAcctCategory, Double> getRatios() {
        SortedMap<TimeAcctCategory, Double> res = new TreeMap<>();
        if (totalTime == 0) return res;

        for (Map.Entry<TimeAcctCategory, Double> me : allocMap.entrySet()) {
            double time  = me.getValue();
            if (time == 0) continue;

            double ratio = time / totalTime;
            res.put(me.getKey(), ratio);
        }

        return res;
    }

    /**
     * Gets the total time allocated to all the categories combined.
     */
    public double getTotalTime() {
        return totalTime;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        totalTime = 0;
        for (Double d : allocMap.values()) {
            totalTime += d;
        }
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        for (Map.Entry<TimeAcctCategory, Double> me : allocMap.entrySet()) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(me.getKey()).append("=").append(me.getValue());
        }

        return buf.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimeAcctAllocation that = (TimeAcctAllocation) o;

        if (Double.compare(that.totalTime, totalTime) != 0) return false;
        if (!allocMap.equals(that.allocMap)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = allocMap.hashCode();
        temp = Double.doubleToLongBits(totalTime);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
