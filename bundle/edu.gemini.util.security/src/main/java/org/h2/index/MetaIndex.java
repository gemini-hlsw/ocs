/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.index;

import java.util.ArrayList;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.MetaTable;

/**
 * The index implementation for meta data tables.
 */
public class MetaIndex extends BaseIndex {

    private final MetaTable meta;
    private final boolean scan;

    public MetaIndex(MetaTable meta, IndexColumn[] columns, boolean scan) {
        initBaseIndex(meta, 0, null, columns, IndexType.createNonUnique(true));
        this.meta = meta;
        this.scan = scan;
    }

    public void close(Session session) {
        // nothing to do
    }

    public void add(Session session, Row row) {
        throw DbException.getUnsupportedException("META");
    }

    public void remove(Session session, Row row) {
        throw DbException.getUnsupportedException("META");
    }

    public Cursor find(Session session, SearchRow first, SearchRow last) {
        ArrayList<Row> rows = meta.generateRows(session, first, last);
        return new MetaCursor(rows);
    }

    public double getCost(Session session, int[] masks) {
        if (scan) {
            return 10 * MetaTable.ROW_COUNT_APPROXIMATION;
        }
        return getCostRangeIndex(masks, MetaTable.ROW_COUNT_APPROXIMATION);
    }

    public void truncate(Session session) {
        throw DbException.getUnsupportedException("META");
    }

    public void remove(Session session) {
        throw DbException.getUnsupportedException("META");
    }

    public int getColumnIndex(Column col) {
        if (scan) {
            // the scan index cannot use any columns
            return -1;
        }
        return super.getColumnIndex(col);
    }

    public void checkRename() {
        throw DbException.getUnsupportedException("META");
    }

    public boolean needRebuild() {
        return false;
    }

    public String getCreateSQL() {
        return null;
    }

    public boolean canGetFirstOrLast() {
        return false;
    }

    public Cursor findFirstOrLast(Session session, boolean first) {
        throw DbException.getUnsupportedException("META");
    }

    public long getRowCount(Session session) {
        return MetaTable.ROW_COUNT_APPROXIMATION;
    }

    public long getRowCountApproximation() {
        return MetaTable.ROW_COUNT_APPROXIMATION;
    }

    public String getPlanSQL() {
        return "meta";
    }

}
