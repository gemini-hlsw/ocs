package edu.gemini.spdb.reports.util;

import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.ISort;

/**
 * Trivial javabean implementation of ISort. Just a container.
 * @author rnorris
 */
public class SimpleSort implements ISort {

	private static final long serialVersionUID = 1L;

	private final IColumn column;
	private Order order;
	private NullPolicy nullPolicy;

	public SimpleSort(IColumn column) {
		this(column, Order.ASC);
	}
	
	public SimpleSort(IColumn column, Order order) {
		this(column, order, NullPolicy.NULL_LAST);
	}
	
	public SimpleSort(IColumn column, Order order, NullPolicy nullPolicy) {
		this.column = column;
		this.order = order;
		this.nullPolicy = nullPolicy;
	}

	public NullPolicy getNullPolicy() {
		return nullPolicy;
	}

	public void setNullPolicy(NullPolicy nullPolicy) {
		this.nullPolicy = nullPolicy;
	}
	
	public Order getOrder() {
		return order;
	}
	
	public void setOrder(Order order) {
		this.order = order;
	}
	
	public IColumn getColumn() {
		return column;
	}
	
}
