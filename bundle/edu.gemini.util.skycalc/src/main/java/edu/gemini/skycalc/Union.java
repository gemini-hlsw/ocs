package edu.gemini.skycalc;

import edu.gemini.skycalc.Interval.Overlap;

import java.time.Instant;
import java.util.*;
import java.io.Serializable;

/**
 * Represents a collection of Intervals which are automatically merged and split such that
 * they never overlap or abut.
 * @see Interval#overlaps(Interval, Overlap)
 * @see Interval#abuts(Interval)
 * @author rnorris
 * @param <T> the Interval type
 */
public class Union<T extends Interval> implements Iterable<T>, Serializable {

	private final SortedSet<T> intervals = new TreeSet<T>();

	public Union() {
	}

	public Union(Union<T> other) {
		this();
		add(other);
	}

	public Union(Collection<? extends T> other) {
		this();
		add(other);
	}

	public Union(T... intervals) {
		add(intervals);
	}

	public Iterator<T> iterator() {
		return intervals.iterator();
	}

	public void add(Union<? extends T> other) {
		for (T t: other) add(t);
	}

	public void add(Collection<? extends T> other) {
		for (T t: other) add(t);
	}

	public void add(T... other) {
		for (T t: other) add(t);
	}

	public final void remove(Union<? extends T> other) {
		for (T t: other) remove(t);
	}

	public final void remove(Collection<? extends T> other) {
		for (T t: other) remove(t);
	}

	public final void remove(T... other) {
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
				ni = (T) ni.plus(oi);
			}
		}
		intervals.add(ni);
	}

	/**
	 * Removes the specified interval, which will be clipped out of the internal collection.
	 * @param del
	 */
	@SuppressWarnings("unchecked")
	public final void remove(final T del) {
		final List<T> toAdd = new ArrayList<T>();
		for (final Iterator<T> it = intervals.iterator(); it.hasNext(); ) {
			final T oi = it.next();
			if (del.overlaps(oi, Overlap.TOTAL)) {
				it.remove();
				continue;
			}

            if (del.overlaps(oi, Overlap.PARTIAL)) {
				it.remove();
				toAdd.add((T) oi.minus(del));
			} else if (oi.overlaps(del, Overlap.TOTAL)) {
				it.remove();
				toAdd.add((T) oi.create(oi.getStart(), del.getStart()));
				toAdd.add((T) oi.create(del.getEnd(), oi.getEnd()));
			}
		}
		intervals.addAll(toAdd);
	}



	@SuppressWarnings("unchecked")
	public void intersect(Union<? extends T> that) {

		// Get the intervals
		final SortedSet<? extends Interval> thisI = this.getIntervals();
		final SortedSet<? extends Interval> thatI = that.getIntervals();

		// Collect the interval endpoints.
		int i = 0;
		final long[] points = new long[(thisI.size() + thatI.size()) * 2];
		for (Interval iv: thisI) { points[i++] = iv.getStart(); points[i++] = iv.getEnd(); }
		for (Interval iv: thatI) { points[i++] = iv.getStart(); points[i++] = iv.getEnd(); }
		Arrays.sort(points);
//		System.out.println("Points == " + Arrays.toString(points));

		// Ok, we know there is an even number of points. Look at each
		// consecutive pair.
		for (i = 0; i < points.length - 1; i++) {
			long a = points[i], b = points[i+1];
//			System.out.println("Examining " + a + " - " + b);
			if (this.contains(a) && that.contains(a)) continue;

//			System.out.println("Removing " + a + " - " + b);

			remove((T) new Interval(a, b));
		}

	}

	public static void main(String[] args) {

		Union<Interval> u1 = new Union<Interval>();
		Union<Interval> u2 = new Union<Interval>();

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




	public boolean contains(long t) {
		for (Interval i: this)
			if (i.contains(t))
				return true;
		return false;
	}


	public boolean contains(Instant i) {
	    return contains(i.toEpochMilli());
	}


	/**
	 * Returns any internal Intervals which overlap with <code>interval</code> as specified.
	 * @see Interval#overlaps(Interval, Overlap)
	 * @see edu.gemini.skycalc.Interval.Overlap
	 */
	public List<T> getOverlaps(T interval, Overlap olap) {
		List<T> ret = new ArrayList<T>();
		for (T i : intervals) {
			if (i.overlaps(interval, olap))
				ret.add(i);
		}
		return ret;
	}

	/**
	 * Returns the current interval set (immutable). You should copy it if you want to
	 * iterate.
	 * @return this Union's internal Intervals in natural order
	 */
	public SortedSet<T> getIntervals() {
		return Collections.unmodifiableSortedSet(intervals);
	}

	/**
	 * Sums the lengths of all contained intervals.
	 */
	public long sum() {
		long sum = 0;
		for (T i: intervals) sum += i.getLength();
		return sum;
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
	@SuppressWarnings({"CloneDoesntCallSuperClone"})
    public Union<T> clone() {
		Union<T> u = new Union<T>();
		for (T i: intervals)
			u.add(i);
		return u;
	}

}
