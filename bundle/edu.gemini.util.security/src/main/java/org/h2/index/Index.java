/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.index;

import org.h2.engine.Session;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.schema.SchemaObject;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;

/**
 * An index. Indexes are used to speed up searching data.
 */
public interface Index extends SchemaObject {

    /**
     * Get the message to show in a EXPLAIN statement.
     *
     * @return the plan
     */
    String getPlanSQL();

    /**
     * Close this index.
     *
     * @param session the session used to write data
     */
    void close(Session session);

    /**
     * Add a row to the index.
     *
     * @param session the session to use
     * @param row the row to add
     */
    void add(Session session, Row row);

    /**
     * Remove a row from the index.
     *
     * @param session the session
     * @param row the row
     */
    void remove(Session session, Row row);

    /**
     * Find a row or a list of rows and create a cursor to iterate over the result.
     *
     * @param session the session
     * @param first the first row, or null for no limit
     * @param last the last row, or null for no limit
     * @return the cursor to iterate over the results
     */
    Cursor find(Session session, SearchRow first, SearchRow last);

    /**
     * Find a row or a list of rows and create a cursor to iterate over the result.
     *
     * @param filter the table filter (which possibly knows
     *          about additional conditions)
     * @param first the first row, or null for no limit
     * @param last the last row, or null for no limit
     * @return the cursor to iterate over the results
     */
    Cursor find(TableFilter filter, SearchRow first, SearchRow last);

    /**
     * Estimate the cost to search for rows given the search mask.
     * There is one element per column in the search mask.
     * For possible search masks, see IndexCondition.
     *
     * @param session the session
     * @param masks the search mask
     * @return the estimated cost
     */
    double getCost(Session session, int[] masks);

    /**
     * Remove the index.
     *
     * @param session the session
     */
    void remove(Session session);

    /**
     * Remove all rows from the index.
     *
     * @param session the session
     */
    void truncate(Session session);

    /**
     * Check if the index can directly look up the lowest or highest value of a
     * column.
     *
     * @return true if it can
     */
    boolean canGetFirstOrLast();

    /**
     * Check if the index can get the next higher value.
     *
     * @return true if it can
     */
    boolean canFindNext();

    /**
     * Find a row or a list of rows that is larger and create a cursor to
     * iterate over the result.
     *
     * @param session the session
     * @param higherThan the lower limit (excluding)
     * @param last the last row, or null for no limit
     * @return the cursor
     */
    Cursor findNext(Session session, SearchRow higherThan, SearchRow last);

    /**
     * Find the first (or last) value of this index. The cursor returned is
     * positioned on the correct row, or on null if no row has been found.
     *
     * @param session the session
     * @param first true if the first (lowest for ascending indexes) or last
     *            value should be returned
     * @return a cursor (never null)
     */
    Cursor findFirstOrLast(Session session, boolean first);

    /**
     * Check if the index needs to be rebuilt.
     * This method is called after opening an index.
     *
     * @return true if a rebuild is required.
     */
    boolean needRebuild();

    /**
     * Get the row count of this table, for the given session.
     *
     * @param session the session
     * @return the row count
     */
    long getRowCount(Session session);

    /**
     * Get the approximated row count for this table.
     *
     * @return the approximated row count
     */
    long getRowCountApproximation();

    /**
     * Compare two rows.
     *
     * @param rowData the first row
     * @param compare the second row
     * @return 0 if both rows are equal, -1 if the first row is smaller, otherwise 1
     */
    int compareRows(SearchRow rowData, SearchRow compare);

    /**
     * Get the index of a column in the list of index columns
     *
     * @param col the column
     * @return the index (0 meaning first column)
     */
    int getColumnIndex(Column col);

    /**
     * Get the indexed columns as index columns (with ordering information).
     *
     * @return the index columns
     */
    IndexColumn[] getIndexColumns();

    /**
     * Get the indexed columns.
     *
     * @return the columns
     */
    Column[] getColumns();

    /**
     * Get the index type.
     *
     * @return the index type
     */
    IndexType getIndexType();

    /**
     * Get the table on which this index is based.
     *
     * @return the table
     */
    Table getTable();

    /**
     * Commit the operation for a row. This is only important for multi-version
     * indexes.
     *
     * @param operation the operation type
     * @param row the row
     */
    void commit(int operation, Row row);

    /**
     * Get the row with the given key.
     *
     * @param session the session
     * @param key the unique key
     * @return the row
     */
    Row getRow(Session session, long key);

    /**
     * Does this index support lookup by row id?
     *
     * @return true if it does
     */
    boolean isRowIdIndex();

    /**
     * Can this index iterate over all rows?
     *
     * @return true if it can
     */
    boolean canScan();

    /**
     * Enable or disable the 'sorted insert' optimizations (rows are inserted in
     * ascending or descending order) if applicable for this index
     * implementation.
     *
     * @param sortedInsertMode the new value
     */
    void setSortedInsertMode(boolean sortedInsertMode);

}
