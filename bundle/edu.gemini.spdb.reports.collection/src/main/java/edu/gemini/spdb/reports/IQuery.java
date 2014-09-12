package edu.gemini.spdb.reports;

import java.io.Serializable;
import java.util.List;

/**
 * Interface for a query definition on a specific table. A query consists 
 * of the following:
 * <dl>
 * <dt>Table</dt><dd>The table on which the query will operate. This is
 * normally read-only and is specified in the IQuery's factory method.</dd>
 * <dt>Filter</dt><dd>An arbitrary filtering predicate which determines 
 * whether a given table row should be included in the query output.</dd>
 * <dt>Groups</dt><dd>A list of ISort, which specifies how rows should
 * be sorted into groupings. Rows are sorted first by Groups, then by
 * Sorts.</dd>
 * <dt>Sorts</dt><dd>A list of ISort, specifying how rows should be
 * sorted within groups.</dd>
 * <dt>Output Columns</dt><dd>A list of output columns. Values are not
 * returned for columns that are not specified.</dd>
 * </dl>
 * @author rnorris
 */
public interface IQuery extends Serializable {
	
	/**
	 * Sets the filter for this query, or null to accept all rows.
	 * @param filter a filter, or null to accept all rows
	 */
	void setFilter(IFilter filter);

	/**
	 * Returns the query filter, if any.
	 * @return the query filter, or null
	 */
	IFilter getFilter();
	
	/**
	 * Sets the query groupings. Results will be grouped (sorted) by all
	 * specified ISorts, in order, and values will be returned for each
	 * grouping column.
	 * @param groups a set of ISorts to group by
	 */
	void setGroups(ISort... groups);
	
	/**
	 * Gets the query groupings, or an empty list if none.
	 * @return query groupings, maybe empty but never null
	 */
	List<ISort> getGroups();
	
	/**
	 * Sets the query sorts. Results will be sorted (within each group) by 
	 * all specified ISorts, in order.
	 * @param groups a set of ISorts to sort by
	 */
	void setSorts(ISort... sorts);

	/**
	 * Gets the query sorts, or an empty list if none.
	 * @return query sorts, maybe empty but never null
	 */
	List<ISort> getSorts();
	
	/**
	 * Sets the list of columns to be returned in the query results. 
	 * Values will not be returned for columns that are not included
	 * (but see setGroups).
	 * @param col a list of columns
	 */
	void setOutputColumns(IColumn... col);
	
	/**
	 * Returns the output columns, or an empty list.
	 * @return the output columns, or an empty list
	 */
	List<IColumn> getOutputColumns();
	
	/**
	 * Returns the table against which this query will run.
	 * @return a table
	 */
	ITable getTable();
	
}
