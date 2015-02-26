package edu.gemini.qpt.core.util;

/**
 * Anything that represents an interval with a start and end time.
 */
public interface IntervalType<T extends IntervalType<T>> {

    Interval getInterval();
    long getStart();
    long getEnd();
    long getLength();
    boolean contains(long value);
    boolean overlaps(IntervalType<?> interval, Interval.Overlap overlap);
    boolean abuts(IntervalType<?> interval);
    T plus(IntervalType<?> other);
    T minus(IntervalType<?> other);
    T create(long start, long end);

}
