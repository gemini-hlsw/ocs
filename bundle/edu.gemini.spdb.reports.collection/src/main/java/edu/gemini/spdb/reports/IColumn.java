package edu.gemini.spdb.reports;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Token-like interface for a table column, normally implemented as an
 * enumerated type (although this is not required, and in fact the type
 * parameter must be thrown away if using an enum).
 * @author rnorris
 * @param <T>
 */
public interface IColumn<T> extends Serializable {

	/**
	 * Implementations should return the human-readable column header
	 * caption.
	 * @return the human-readable column header string
	 */
	String getCaption();

	/**
	 * Implementations should return the formatted plaintext string
	 * value for the passed T
	 * @param value
	 * @return a plaintext string
	 */
	String format(T value);

	/**
	 * Implementations should return the unique (per table) symbolic
	 * name for this column. Normally Enum will supply the implementation.
	 */
	String name();

}
