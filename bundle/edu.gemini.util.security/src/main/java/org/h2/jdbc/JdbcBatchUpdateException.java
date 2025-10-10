/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.jdbc;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.BatchUpdateException;
import java.sql.SQLException;

/**
 * Represents a batch update database exception.
 */
public class JdbcBatchUpdateException extends BatchUpdateException {

    private static final long serialVersionUID = 1L;

    /**
     * INTERNAL
     */
    JdbcBatchUpdateException(SQLException next, int[] updateCounts) {
        super(next.getMessage(), next.getSQLState(), next.getErrorCode(), updateCounts);
        setNextException(next);
    }

    /**
     * INTERNAL
     */
    public void printStackTrace() {
        // The default implementation already does that,
        // but we do it again to avoid problems.
        // If it is not implemented, somebody might implement it
        // later on which would be a problem if done in the wrong way.
        printStackTrace(System.err);
    }

    /**
     * INTERNAL
     */
    public void printStackTrace(PrintWriter s) {
        if (s != null) {
            super.printStackTrace(s);
            if (getNextException() != null) {
                getNextException().printStackTrace(s);
            }
        }
    }

    /**
     * INTERNAL
     */
    public void printStackTrace(PrintStream s) {
        if (s != null) {
            super.printStackTrace(s);
            if (getNextException() != null) {
                getNextException().printStackTrace(s);
            }
        }
    }

}
