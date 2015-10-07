package jsky.catalog.gui;

import jsky.catalog.*;

/**
 * This defines the common interface for classes that can display a QueryResult.
 * Classes defining this interface should know how to display the contents of
 * classes implementing the QueryResult interface.
 */
@Deprecated
public interface QueryResultDisplay {

    /**
     * Display the given query result.
     */
    void setQueryResult(QueryResult queryResult);
}
