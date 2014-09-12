package edu.gemini.catalog.api;

import java.io.IOException;

/**
 * Describes the interface expected of a catalog server.
 */
public interface CatalogServer {

    /**
     * Synchronously produces a result matching the constraint.
     *
     * @param c query constraint
     *
     * @return query result
     *
     * @throws IOException if there is a problem communicating with remote
     * servers
     */
    CatalogResult query(QueryConstraint c) throws IOException;
}
