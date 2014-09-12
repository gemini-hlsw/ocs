package edu.gemini.spdb.reports;

import java.io.Serializable;

/**
 * Defines a table sort element, as a simple tuple of IColumn, Order,
 * and NullPolicy. The collation order is based on the IColumn's 
 * comparator.
 * @author rnorris
 */
public interface ISort extends Serializable {

	/**
	 * Enum for sort order (up or down).
	 */
	enum Order { ASC, DESC }
	
	/**
	 * Enum for null policy. Empty values can bubble up or down.
	 */
	enum NullPolicy { NULL_FIRST, NULL_LAST }

	/**
	 * Reference to the column upon which to sort.
	 * @return the column
	 */
	IColumn getColumn();
	
	/**
	 * Sort order for the column, based on the column's comparator.
	 * @return an Order
	 */
	Order getOrder();
	
	/** 
	 * Sets the sort order for the column.
	 * @param order an Order
	 */
	void setOrder(Order order);
	
	/**
	 * The null policy. Up or down.
	 * @return a NullPolicy 
	 */
	NullPolicy getNullPolicy();
	
	/**
	 * Sets the null policy. Up or Down.
	 * @param nullPolicy a NullPolicy
	 */
	void setNullPolicy(NullPolicy nullPolicy);
	
}
