package edu.gemini.skycalc;

import java.io.Serializable;
import java.time.Instant;

/**
 * Pair of longs representing the interval <code>(start .. end]</code>.
 * @author rnorris
 */
public class Interval implements Comparable<Interval>, Serializable /*, PioSerializable */ {

	private final long start, end;

	/**
	 * Enum representing the different ways in which intervals may overlap.<p>
	 * BUG: there is no way to represent the inverse of PARTIAL ... hasn't come up yet
	 * but I should support it.
	 * @see Interval#overlaps(Interval, Overlap)
	 */
	public enum Overlap {

		/**
		 * A overlaps B partially if and only if part of A lies within B and part of A
		 * lines outside of B:<pre>
		 * A = 1 2 3 4 5
		 * B =       4 5 6 7
		 * </pre>
		 */
		PARTIAL,


		/**
		 * A overlaps B totally if every element of B is also in A:<pre>
		 * A = 1 2 3 4 5
		 * B =     3 4
		 * </pre>
		 */
		TOTAL,

		/**
		 * Semantics of <code>PARTIAL || TOTAL</code>.
		 */
		EITHER,

		/**
		 * Semantics of <code>!EITHER</code>
		 */
		NONE,

	}

	/**
	 * Creates a new Interval that begins before <code>start</code> and ends before
	 * <code>end</code>. So the interval specified by <code>start = 10</code> and
	 * <code>end = 14</code> contains exactly the integers <code>10, 11, 13</code>.
	 * @param start the first number in the interval
	 * @param end the first number following first that is <i>not</i> in the interval.
	 */
	public Interval(long start, long end) {
		if (start > end) throw new IllegalArgumentException(start + " > " + end);
		this.start = start;
		this.end = end;
	}

	public Interval(Instant start, Instant end) {
		this(start.toEpochMilli(), end.toEpochMilli());
	}

	/**
	 * Returns <code>true</code> if the specified value falls within the interval. Note that
	 * the <code>end</code> value does <i>not</i> fall within the interval.
	 * @param value
	 * @return true if <code>value</code> falls within the interval.
	 */
	public final boolean contains(long value) {
		return value >= start && value < end;
	}

	/**
	 * Returns true if this interval overlaps the passed <code>interval</code> in the
	 * specified manner. Note that this operation is not necessarily commutative;
	 * <code>(10 .. 20]</code> overlaps <code>(13 .. 15]</code> completely, but the inverse
	 * is not true.
	 * @see Overlap
	 * @param interval another interval
	 * @param overlap the type of Overlap we're looking for
	 * @return true if this Interval overlaps <code>interval</code> in the specified manner
	 */
	public final boolean overlaps(final Interval interval,final  Overlap overlap) {

		boolean first = start == interval.start || contains(interval.start);
		boolean last = end == interval.end || contains(interval.start + interval.getLength());

		switch (overlap) {
		case TOTAL : return first && last;
		case EITHER : return first || last;
		case PARTIAL : return first ^ last;
		case NONE: return !(first || last);
		default: throw new Error();
		}
	}

	/**
	 * Returns true if the passed interval does not overlap, but is immediately adjacent,
	 * such that the union would be continuous. Null values are ok, and return false.
	 * @param interval
	 * @return true if the this Interval is adjacent to <code>interval</code>
	 */
	public boolean abuts(Interval interval) {
		return interval != null && (interval.end == start || interval.start == end);
	}

	/**
	 * Returns the outer union of this Interval and <code>interval</code>; that is, the
	 * new union will start with the smaller <code>start</code> value and end with the
	 * larger <code>end</code> value.
	 * @param other
	 * @return the outer union of this Interval and <code>interval</code>
	 */
	public Interval plus(Interval other) {
		return create(Math.min(start, other.start), Math.max(end, other.end));
	}

	/**
	 * Returns a copy of this Interval, clipped by the passed partially-overlapping
	 * <code>other</code>. This operation is undefined if the Intervals do not have a
	 * partial overlap.
	 * @param other the Interval to subtract from this one
	 * @throws IllegalArgumentException if the intervals do not overlap partially
	 * @see Overlap#PARTIAL
	 * @return this Interval, clipped by <code>other</code>.
	 */
	public Interval minus(Interval other) {
		if (!other.overlaps(this, Overlap.PARTIAL)) throw new IllegalArgumentException("Intervals do not overlap partially.");
		if (other.start > start) return create(start, other.start);
		return create(other.end, end);
	}

	/**
	 * Specifies a natural ordering for Intervals in which the interval that starts first
	 * is ordered first, and if two intervals start at the same point the shorter of the
	 * two will come first. Equal intervals compared to zero.
	 * @param o another Interval
	 */
	public int compareTo(Interval o) {
		int ret = Long.signum(start - o.start);
		return ret != 0 ? ret : Long.signum(end - o.end);
	}

	/**
	 * Returns true if and only if the passed object is an Interval with the same bounds
	 * as this one.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Interval) {
			Interval o = (Interval) obj;
			return start == o.start && end == o.end;
		}
		return false;
	}

	public boolean equals(long start, long end) {
		return this.start == start && this.end == end;
	}

	@Override
	public int hashCode() {
		return (int) (start ^ end);
	}

	// HACK: this shouldn't do dates.
	@Override
	public String toString() {
		return "(" + start + ".." + end + "]";
	}

	/**
	 * Returns the end point for this Interval.
	 */
	public long getEnd() {
		return end;
	}

	public Instant getEndInstant() {
		return Instant.ofEpochMilli(end);
	}

	/**
	 * Returns the start point for this Interval.
	 */
	public long getStart() {
		return start;
	}

	public Instant getStartInstant() {
		return Instant.ofEpochMilli(start);
	}

	/**
	 * Returns the length of this Interval, as <code>end - start</code>.
	 */
	public long getLength() {
		return end - start;
	}

	/**
	 * Subclasses <i>must</i> override this method to return an object of the same type with
	 * the given bounds in order to support the methods that return new instances. There is
	 * probably a way to enforce this using generics but I can't figure it out.
	 * @return a new Interval of the same type, with the given bounds
	 */
	protected Interval create(long start, long end) {
		return new Interval(start, end);
	}
}
