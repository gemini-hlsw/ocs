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
	private final String displayName;
	private final Domain domain;
	private final String shortDescription;

	public AbstractTable(final Domain domain, final IColumn[] columns, final String displayName, final String shortDescription) {
		this.domain = domain;
		this.columns = new TreeSet<>(Arrays.asList(columns));
		this.displayName = displayName;
		this.shortDescription = shortDescription;
	}

	public final Set<IColumn> getColumns() {
		return columns;
	}

	public final String getDisplayName() {
		return displayName;
	}

	public final Domain getDomain() {
		return domain;
	}

	public final String getShortDescription() {
		return shortDescription;
	}

}

