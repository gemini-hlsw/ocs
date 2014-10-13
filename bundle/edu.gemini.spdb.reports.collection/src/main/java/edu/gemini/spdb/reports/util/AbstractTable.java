package edu.gemini.spdb.reports.util;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.ITable;

/**
 * Base class for a table. Nothing fancy.
 * @author rnorris
 */
public abstract class AbstractTable implements ITable {

	private final Set<IColumn> columns;
	private final Domain domain;

	public AbstractTable(Domain domain, IColumn[] columns) {
		this.domain = domain;
		this.columns = new TreeSet<>(Arrays.asList(columns));
	}

	public final Set<IColumn> getColumns() {
		return columns;
	}

	public final Domain getDomain() {
		return domain;
	}

}

