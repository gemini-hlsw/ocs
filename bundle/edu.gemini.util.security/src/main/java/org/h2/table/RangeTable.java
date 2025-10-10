/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.table;

import java.util.ArrayList;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.index.RangeIndex;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.value.Value;

/**
 * The table SYSTEM_RANGE is a virtual table that generates incrementing numbers
 * with a given start end end point.
 */
public class RangeTable extends Table {

    /**
     * The name of the range table.
     */
    public static final String NAME = "SYSTEM_RANGE";

    private Expression min, max;
    private boolean optimized;

    /**
     * Create a new range with the given start and end expressions.
     *
     * @param schema the schema (always the main schema)
     * @param min the start expression
     * @param max the end expression
     * @param noColumns whether this table has no columns
     */
    public RangeTable(Schema schema, Expression min, Expression max, boolean noColumns) {
        super(schema, 0, NAME, true, true);
        Column[] cols = noColumns ? new Column[0] : new Column[] { new Column("X", Value.LONG) };
        this.min = min;
        this.max = max;
        setColumns(cols);
    }

    public String getDropSQL() {
        return null;
    }

    public String getCreateSQL() {
        return null;
    }

    public String getSQL() {
        return NAME + "(" + min.getSQL() + ", " + max.getSQL() + ")";
    }

    public void lock(Session session, boolean exclusive, boolean force) {
        // nothing to do
    }

    public void close(Session session) {
        // nothing to do
    }

    public void unlock(Session s) {
        // nothing to do
    }

    public boolean isLockedExclusively() {
        return false;
    }

    public Index addIndex(Session session, String indexName,
            int indexId, IndexColumn[] cols, IndexType indexType,
            boolean create, String indexComment) {
        throw DbException.getUnsupportedException("SYSTEM_RANGE");
    }

    public void removeRow(Session session, Row row) {
        throw DbException.getUnsupportedException("SYSTEM_RANGE");
    }

    public void addRow(Session session, Row row) {
        throw DbException.getUnsupportedException("SYSTEM_RANGE");
    }

    public void checkSupportAlter() {
        throw DbException.getUnsupportedException("SYSTEM_RANGE");
    }

    public void checkRename() {
        throw DbException.getUnsupportedException("SYSTEM_RANGE");
    }

    public boolean canGetRowCount() {
        return true;
    }

    public boolean canDrop() {
        return false;
    }

    public long getRowCount(Session session) {
        return Math.max(0, getMax(session) - getMin(session) + 1);
    }

    public String getTableType() {
        throw DbException.throwInternalError();
    }

    public Index getScanIndex(Session session) {
        return new RangeIndex(this, IndexColumn.wrap(columns));
    }

    /**
     * Calculate and get the start value of this range.
     *
     * @param session the session
     * @return the start value
     */
    public long getMin(Session session) {
        optimize(session);
        return min.getValue(session).getLong();
    }

    /**
     * Calculate and get the end value of this range.
     *
     * @param session the session
     * @return the end value
     */
    public long getMax(Session session) {
        optimize(session);
        return max.getValue(session).getLong();
    }

    private void optimize(Session s) {
        if (!optimized) {
            min = min.optimize(s);
            max = max.optimize(s);
            optimized = true;
        }
    }

    public ArrayList<Index> getIndexes() {
        return null;
    }

    public void truncate(Session session) {
        throw DbException.getUnsupportedException("SYSTEM_RANGE");
    }

    public long getMaxDataModificationId() {
        return 0;
    }

    public Index getUniqueIndex() {
        return null;
    }

    public long getRowCountApproximation() {
        return 100;
    }

    public boolean isDeterministic() {
        return true;
    }

    public boolean canReference() {
        return false;
    }

}
