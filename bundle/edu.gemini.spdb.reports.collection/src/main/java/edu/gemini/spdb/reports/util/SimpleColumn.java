package edu.gemini.spdb.reports.util;

import java.util.Comparator;
import java.util.logging.Logger;

import edu.gemini.spdb.reports.IColumn;
import edu.gemini.spdb.reports.IRow;

/**
 * Column implementation that uses a <code>Formatter</code>.
 * @author rnorris
 * @param <T>
 * @deprecated use an enum type
 */
@Deprecated
public class SimpleColumn<T> implements IColumn<T> {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(SimpleColumn.class.getName());
	private static final long serialVersionUID = 2L;

	private final String caption;
	private final String format;
	private final Comparator<T> comparator;
	private final String name;
	

	public SimpleColumn(String name, String caption, String format) {
		this(name, caption, format,  null);		
	}
	
	public SimpleColumn(String name, String caption, String format, Comparator<T> comparator) {
		this.caption = caption;
		this.format = format;
		this.comparator = comparator;
		this.name = name;
	}

	public String getCaption() {
		return caption;
	}

	public String format(T value) {
		return value == IRow.NULL_VALUE ? "" : String.format(format, value);
	}

	public Comparator<T> getComparator() {
		return comparator;
	}

	@Override
	public String toString() {
		return caption;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {		
		return (obj instanceof SimpleColumn) &&	((SimpleColumn) obj).name.equals(name);
	}

	public String name() {
		return name;
	}
	
}

