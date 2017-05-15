package edu.gemini.spModel.timeacct;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

/**
 * A collection of time allocations for the various
 * {@link TimeAcctCategory categories}.
 */
public final class TimeAcctAllocation implements Serializable {

    public static final TimeAcctAllocation EMPTY = new TimeAcctAllocation();

    private final Map<TimeAcctCategory, TimeAcctAward> allocMap;

    private final TimeAcctAward sum;

    private TimeAcctAllocation() {
        allocMap = Collections.emptyMap();
        sum      = TimeAcctAward.ZERO;
    }

    /**
     * Computes a TimeAcctAllocation from a map containing hours allocated for
     * each non-zero time accounting category.
     *
     * @param allocationMap hours for all relevant categories; hours must be
     * greater than or equal to 0
     */
    public TimeAcctAllocation(final Map<TimeAcctCategory, TimeAcctAward> allocationMap) {
        final Map<TimeAcctCategory, TimeAcctAward> tmpAllocMap = new TreeMap<>(allocationMap);

        TimeAcctAward tmp = TimeAcctAward.ZERO;

        for (final Map.Entry<TimeAcctCategory, TimeAcctAward> me : tmpAllocMap.entrySet()) {
            final TimeAcctAward a = me.getValue();
            if (a == null) {
                throw new IllegalArgumentException(
                        String.format("null time accounting award for category %s", me.getKey().name())
                );
            }
            tmp = tmp.plus(a);
        }

        sum      = tmp;
        allocMap = Collections.unmodifiableMap(tmpAllocMap);
    }

    /**
     * Gets the number of hours associated with the given category.
     */
    public TimeAcctAward getAward(final TimeAcctCategory category) {
        final TimeAcctAward res = allocMap.get(category);
        return (res == null) ? TimeAcctAward.ZERO : res;
    }

    private double ratio(Function<TimeAcctAward, Duration> toDuration, TimeAcctAward award) {
        if (toDuration.apply(sum).isZero()) return 0;
        return ((double) toDuration.apply(award).toMillis()) / ((double) toDuration.apply(sum).toMillis());
    }

    /**
     * Gets the percentage of the total time allocation that is associated with
     * the given category.
     */
    public double getPercentage(final TimeAcctCategory category, Function<TimeAcctAward, Duration> toDuration) {
        return ratio(toDuration, getAward(category)) * 100;
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
     *
     * @param toDuration function that converts an award to a Duration; with
     *                   this the caller can get total time ratios or just
     *                   program or partner time ratios
     */
    public SortedMap<TimeAcctCategory, Double> getRatios(final Function<TimeAcctAward, Duration> toDuration) {
        final SortedMap<TimeAcctCategory, Double> res = new TreeMap<>();
        if (sum.isZero()) return res;

        for (final Map.Entry<TimeAcctCategory, TimeAcctAward> me : allocMap.entrySet()) {
            res.put(me.getKey(), ratio(toDuration, me.getValue()));
        }

        return res;
    }

    /**
     * Gets the total time allocated to all the categories combined.
     */
    public TimeAcctAward getSum() {
        return sum;
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder();

        for (final Map.Entry<TimeAcctCategory, TimeAcctAward> me : allocMap.entrySet()) {
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
        final TimeAcctAllocation that = (TimeAcctAllocation) o;
        return Objects.equals(allocMap, that.allocMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allocMap);
    }
}
