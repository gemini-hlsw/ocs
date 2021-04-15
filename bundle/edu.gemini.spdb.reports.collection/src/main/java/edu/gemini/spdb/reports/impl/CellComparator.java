package edu.gemini.spdb.reports.impl;

import java.io.Serializable;
import java.util.Comparator;

import edu.gemini.spdb.reports.ISort.NullPolicy;
import edu.gemini.spdb.reports.ISort.Order;

/**
 * Comparator that wraps another comparator, adding Order and NullPolicy
 * semantics. Basically this lets you take any comparator and flip its
 * order, or make nulls bubble one way or the other. Also makes comparators
 * null-safe as a side-effect.
 * @author rnorris
 * @param <T> any type
 */
public class CellComparator<T> implements Comparator<T>, Serializable {

	private static final long serialVersionUID = 1L;

	private final NullPolicy nullPolicy;
	private final int factor;

	public CellComparator(Order sortOrder, NullPolicy nullPolicy) {
		this.nullPolicy = nullPolicy;
		this.factor = sortOrder == Order.ASC ? 1 : -1;
	}

	@SuppressWarnings("unchecked")
	public int compare(T a, T b) {

		if (a == null && b == null)
			return 0;

		if (a == null)
			return nullPolicy == NullPolicy.NULL_FIRST ? factor : -factor;

		if (b == null)
			return nullPolicy == NullPolicy.NULL_FIRST ? -factor : factor;

		return factor * ((Comparable<T>) a).compareTo(b);

	}



}
