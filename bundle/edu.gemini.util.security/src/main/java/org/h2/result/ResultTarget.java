/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.result;

import org.h2.value.Value;

/**
 * A object where rows are written to.
 */
public interface ResultTarget {

    /**
     * Add the row to the result set.
     *
     * @param values the values
     */
    void addRow(Value[] values);

    /**
     * Get the number of rows.
     *
     * @return the number of rows
     */
    int getRowCount();

}
