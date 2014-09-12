package edu.gemini.spdb.reports;

import java.io.Serializable;
import java.util.Map;

/**
 * Interface for a filter than can accept or reject a table row.
 * @author rnorris
 */
public interface IFilter extends Serializable {
	
	/**
	 * Implementations should return true if the passed row should be
	 * accepted, or false if the row should be filtered out. Note that
	 * the passed row is a <i>table</i> row, not a result row; all 
	 * defined table columns will be available, even if the associated
	 * query does not declare them as output columns.
	 * @param row
	 * @return true to accept the row, false to filter it out
	 */
	boolean accept(Map<IColumn, ?> row);
	
}
