/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.table;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import org.h2.message.DbException;
import org.h2.util.JdbcUtils;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

/**
 * A connection for a linked table. The same connection may be used for multiple
 * tables, that means a connection may be shared.
 */
public class TableLinkConnection {

    /**
     * The map where the link is kept.
     */
    private final HashMap<TableLinkConnection, TableLinkConnection> map;

    /**
     * The connection information.
     */
    private final String driver, url, user, password;

    /**
     * The database connection.
     */
    private Connection conn;

    /**
     * How many times the connection is used.
     */
    private int useCounter;

    private TableLinkConnection(
            HashMap<TableLinkConnection, TableLinkConnection> map,
            String driver, String url, String user, String password) {
        this.map = map;
        this.driver = driver;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Open a new connection.
     *
     * @param map the map where the connection should be stored
     *      (if shared connections are enabled).
     * @param driver the JDBC driver class name
     * @param url the database URL
     * @param user the user name
     * @param password the password
     * @param shareLinkedConnections if connections should be shared
     * @return a connection
     */
    public static TableLinkConnection open(
            HashMap<TableLinkConnection, TableLinkConnection> map,
            String driver, String url, String user, String password, boolean shareLinkedConnections) {
        TableLinkConnection t = new TableLinkConnection(map, driver, url, user, password);
        if (!shareLinkedConnections) {
            t.open();
            return t;
        }
        synchronized (map) {
            TableLinkConnection result;
            result = map.get(t);
            if (result == null) {
                synchronized (t) {
                    t.open();
                }
                // put the connection in the map after is has been opened,
                // so we know it works
                map.put(t, t);
                result = t;
            }
            synchronized (result) {
                result.useCounter++;
            }
            return result;
        }
    }

    private void open() {
        try {
            conn = JdbcUtils.getConnection(driver, url, user, password);
        } catch (SQLException e) {
            throw DbException.convert(e);
        }
    }

    public int hashCode() {
        return Utils.hashCode(driver)
                ^ Utils.hashCode(url)
                ^ Utils.hashCode(user)
                ^ Utils.hashCode(password);
    }

    public boolean equals(Object o) {
        if (o instanceof TableLinkConnection) {
            TableLinkConnection other = (TableLinkConnection) o;
            return StringUtils.equals(driver, other.driver)
                    && StringUtils.equals(url, other.url)
                    && StringUtils.equals(user, other.user)
                    && StringUtils.equals(password, other.password);
        }
        return false;
    }

    /**
     * Get the connection.
     * This method and methods on the statement must be
     * synchronized on this object.
     *
     * @return the connection
     */
    Connection getConnection() {
        return conn;
    }

    /**
     * Closes the connection if this is the last link to it.
     *
     * @param force if the connection needs to be closed even if it is still
     *            used elsewhere (for example, because the connection is broken)
     */
    synchronized void close(boolean force) {
        if (--useCounter <= 0 || force) {
            JdbcUtils.closeSilently(conn);
            synchronized (map) {
                map.remove(this);
            }
        }
    }

}
