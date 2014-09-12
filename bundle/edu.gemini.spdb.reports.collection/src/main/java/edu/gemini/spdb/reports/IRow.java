package edu.gemini.spdb.reports;

import java.io.Serializable;

/**
 * Represents an output row as two Object[] arrays, one for group values
 * and one for output column values. These arrays will be parallel to the
 * columns as defined in the source query's Groups and OutputColumns
 * collections.
 * <p>
 * The idea here is that this would be a trivial type, but because 
 * Velocity is stupid I had to add a few things. Needs to be refactored
 * somewhat.
 * @author rnorris
 */
public interface IRow extends Serializable {
	
	/**
	 * HACK: this is for Velocity, which can't deal with null. Column 
	 * values are never null; null values are represented by this guy.
	 */
	Object NULL_VALUE = "";
	
	/**
	 * HACK: this is for Velocity, which can't do array indexing. 
	 * Equivalent to getValues()[i]
	 */
	Object getValue(int i);

	/**
	 * HACK: this is for Velocity, which can't do array indexing. 
	 * Equivalent to getGroupValues()[i]
	 */
	Object getGroupValue(int i);

	/**
	 * Values corresponding to the query columns. All non-null.
	 * @return an array of Object, all non-null
	 */
	Object[] getValues();

	
	/**
	 * Index of the first grouping that starts on this row, or -1 if no group
	 * starts with this row. For example, if
	 * you are grouping on columns [A, B, C, D, E] and this row's value for
	 * C differs from the previous row, the groupIndex will be 2. UIs will
	 * want to draw new headers for [C, D, E].
	 * @return an array of Object, all non-null
	 */
	int getGroupIndex();

	/**
	 * Values corresponding to the group columns. All non-null.
	 * @return an array of Object, all non-null
	 */
	Object[] getGroupValues();
	
}
