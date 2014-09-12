package edu.gemini.spdb.reports.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.IFilter;
import edu.gemini.spdb.reports.IQuery;
import edu.gemini.spdb.reports.ISort;
import edu.gemini.spdb.reports.ITable;

/**
 * Internal implementation of IQuery as a trivial bean class. It is simply
 * a container for its configuration, and contains no logic.
 * @author rnorris
 */
class Query implements IQuery {

	private static final long serialVersionUID = 1L;

	private final ITable table;
	private List<IColumn> outputColumns;
	private List<ISort> sorts = Collections.emptyList();
	private List<ISort> groups = Collections.emptyList();
	private IFilter filter;
	
	public Query(ITable table) {	
		this.table = table;
		this.outputColumns = new ArrayList<IColumn>(table.getColumns());
	}
	
	public IFilter getFilter() {
		return filter;
	}

	public void setFilter(IFilter filter) {
		this.filter = filter;
	}

	public void setOutputColumns(IColumn... columns) {
		this.outputColumns = new ArrayList<IColumn>(Arrays.asList(columns));
	}

	public List<IColumn> getOutputColumns() {
		return Collections.unmodifiableList(outputColumns);
	}

	public ITable getTable() {
		return table;
	}

	public void setSorts(ISort... sort) {
		this.sorts = new ArrayList<ISort>(Arrays.asList(sort));
	}

	public List<ISort> getSorts() {
		return Collections.unmodifiableList(sorts);
	}
	
	public void setGroups(ISort... sort) {
		this.groups = new ArrayList<ISort>(Arrays.asList(sort));
	}

	public List<ISort> getGroups() {
		return Collections.unmodifiableList(groups);
	}
	
}
