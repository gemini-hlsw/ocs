/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.index;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.value.DataType;
import org.h2.value.Value;

/**
 * A cursor for a function that returns a JDBC result set.
 */
public class FunctionCursorResultSet implements Cursor {

    private final Session session;
    private final ResultSet result;
    private final ResultSetMetaData meta;
    private Value[] values;
    private Row row;

    FunctionCursorResultSet(Session session, ResultSet result) {
        this.session = session;
        this.result = result;
        try {
            this.meta = result.getMetaData();
        } catch (SQLException e) {
            throw DbException.convert(e);
        }
    }

    public Row get() {
        if (values == null) {
            return null;
        }
        if (row == null) {
            row = new Row(values, 1);
        }
        return row;
    }

    public SearchRow getSearchRow() {
        return get();
    }

    public boolean next() {
        row = null;
        try {
            if (result != null && result.next()) {
                int columnCount = meta.getColumnCount();
                values = new Value[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    int type = DataType.convertSQLTypeToValueType(meta.getColumnType(i + 1));
                    values[i] = DataType.readValue(session, result, i+1, type);
                }
            } else {
                values = null;
            }
        } catch (SQLException e) {
            throw DbException.convert(e);
        }
        return values != null;
    }

    public boolean previous() {
        throw DbException.throwInternalError();
    }

}