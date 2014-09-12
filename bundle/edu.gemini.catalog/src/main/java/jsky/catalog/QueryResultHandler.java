/*
 * ESO Archive
 *
 * $Id: QueryResultHandler.java 4414 2004-02-03 16:21:36Z brighton $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/04/10  Created
 */

package jsky.catalog;

import java.io.IOException;
import java.net.URL;

/**
 * This defines the common interface for classes that can fetch and
 * parse a QueryResult, given the URL.
 */
public abstract interface QueryResultHandler {

    /**
     * Fetch the data for the given URL and return a QueryResult representing
     * it. If a class can not parse the result returned from the HTTP server,
     * it should return a URLQueryResult.
     */
    public QueryResult getQueryResult(URL url) throws IOException;
}
