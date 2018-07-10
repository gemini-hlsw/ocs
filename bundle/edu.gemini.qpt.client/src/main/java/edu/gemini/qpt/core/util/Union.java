package edu.gemini.qpt.core.util;

import edu.gemini.qpt.core.util.Interval.Overlap;

import java.util.*;

/**
 * Represents a collection of Intervals which are automatically merged and split such that
 * they never overlap or abut.
 * @see Interval#overlaps(Interval, Overlap)
 * @see Interval#abuts(Interval)
 * @author rnorris
 * @param <T> the Interval type 
 */
public class Union<T extends IntervalType<T>> implements Iterable<T> {

    private final SortedSet<T> intervals = new TreeSet<>();
    
    public Union() {
    }

    public Union(final Union<T> other) {
        this();
        add(other);
    }
    
    public Union(final Collection<? extends T> other) {
        this();
        add(other);
    }
    
    public Union(final T... intervals) {
        add(intervals);
    }

    public Iterator<T> iterator() {
        return intervals.iterator();
    }

    public void add(final Union<? extends T> other) {
        for (T t: other) add(t);
    }
    
    public void add(final Collection<? extends T> other) {
        for (T t: other) add(t);
    }
    
    public void add(final T... other) {
        for (T t: other) add(t);
    }
    
    public final void remove(final Union<? extends T> other) {
        for (T t: other) remove(t);
    }
    
    public final void remove(final Collection<? extends T> other) {
        for (T t: other) remove(t);
    }
    
    public final void remove(final T... other) {
        for (T t: other) remove(t);
    }
    

    
    /**
     * Adds the specified interval, which will be merged into the internal collection.
     * @param ni
     */
    @SuppressWarnings("unchecked")
    public void add(T ni) {
        for (Iterator<T> it = intervals.iterator(); it.hasNext(); ) {            
            T oi = it.next();
            if (oi.overlaps(ni, Overlap.EITHER) || oi.abuts(ni)) {
                it.remove();
                ni = ni.plus(oi);
            }
        }
        intervals.add(ni);
    }
    
    /**
     * Removes the specified interval, which will be clipped out of the internal collection.
     * @param del
     */
    @SuppressWarnings("unchecked")
    public final void remove(final IntervalType<?> del) {
        final List<T> toAdd = new ArrayList<>();
        for (final Iterator<T> it = intervals.iterator(); it.hasNext(); ) {            
            final T oi = it.next();
            if (del.overlaps(oi, Overlap.TOTAL)) {
                it.remove();
                continue;
            } else if (del.overlaps(oi, Overlap.PARTIAL)) {
                it.remove();
                toAdd.add(oi.minus(del));
            } else if (oi.overlaps(del, Overlap.TOTAL)) {
                it.remove();
                toAdd.add(oi.create(oi.getStart(), del.getStart()));
                toAdd.add(oi.create(del.getEnd(), oi.getEnd()));
            }
        }
        intervals.addAll(toAdd);
    }
    
    
    
    @SuppressWarnings("unchecked")
    public void intersect(final Union<? extends IntervalType<?>> that) {

        // Get the intervals
        final SortedSet<? extends IntervalType<T>> thisI = this.getIntervals();
        final SortedSet<? extends IntervalType<?>> thatI = that.getIntervals();
        
        // Collect the interval endpoints.
        int i = 0;
        final long[] points = new long[(thisI.size() + thatI.size()) * 2];
        for (IntervalType<T> iv: thisI) { points[i++] = iv.getStart(); points[i++] = iv.getEnd(); }
        for (IntervalType<?> iv: thatI) { points[i++] = iv.getStart(); points[i++] = iv.getEnd(); }
        Arrays.sort(points);

        // Ok, we know there is an even number of points. Look at each 
        // consecutive pair.
        for (i = 0; i < points.length - 1; i++) {
            long a = points[i], b = points[i+1];
            if (this.contains(a) && that.contains(a)) continue;

            remove(new Interval(a, b));
        }
        
    }
    
    public static void main(final String[] args) {
        
        final Union<Interval> u1 = new Union<>();
        final Union<Interval> u2 = new Union<>();
        
        u1.add(new Interval(1, 5));
        u1.add(new Interval(6, 10));

        u2.add(new Interval(-7, -5));
        u2.add(new Interval(0, 2));
        u2.add(new Interval(4, 7));
        u2.add(new Interval(9, 12));
        
        Union<Interval> u3 = u1.clone();
        u3.intersect(u2);
        
        System.out.println(u1 + " intersect " + u2 + " == " + u3);
        
    }
    

    public boolean contains(final long t) {
        for (IntervalType<?> i: this)
            if (i.contains(t))
                return true;
        return false;
    }

    /**
     * Returns the current interval set (immutable). You should copy it if you want to
     * iterate.
     * @return this Union's internal Intervals in natural order
     */
    public SortedSet<T> getIntervals() {
        return Collections.unmodifiableSortedSet(intervals);
    }
    
    @Override
    public String toString() {
        return intervals.toString();
    }
    
    public boolean isEmpty() {
        return intervals.isEmpty();
    }
    
    /**
     * Returns a copy of this Union.
     */
    public Union<T> clone() {
        final Union<T> u = new Union<>();
        for (T i: intervals)
            u.add(i);
        return u;
    }

}
