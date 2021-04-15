package edu.gemini.spdb.reports.impl;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.ISort;
import edu.gemini.spdb.reports.ISort.NullPolicy;
import edu.gemini.spdb.reports.ISort.Order;

/**
 * Composite comparator that uses a collection of ISorts to compare raw
 * rows. This is how rows are Grouped and Sorted in one pass.
 * @author rnorris
 */
public class RowComparator implements Comparator<Map<IColumn, ?>>, Serializable {

	private static final long serialVersionUID = 1L;

	public LinkedHashMap<IColumn, Comparator> comparators = new LinkedHashMap<>();

	@SuppressWarnings("unchecked")
	public int compare(Map<IColumn, ?> a, Map<IColumn, ?> b) {
		for (Entry<IColumn, Comparator> entry: comparators.entrySet()) {
			IColumn column = entry.getKey();
			Comparator comparator = entry.getValue();
			int ret = comparator.compare(a.get(column), b.get(column));
			if (ret != 0)
				return ret;
		}
		return 0;
	}

	public void addComparator(ISort sort) {
		addComparator(sort.getColumn(), sort.getOrder(), sort.getNullPolicy());
	}

	@SuppressWarnings("unchecked")
	public void addComparator(IColumn col, Order order, NullPolicy policy) {
		Comparator comp = new CellComparator(order, policy);
		comparators.put(col, comp);
	}

	public void removeComparator(IColumn col) {
		comparators.remove(col);
	}

}
