package edu.gemini.catalog.impl;

import edu.gemini.catalog.api.*;
import edu.gemini.shared.util.immutable.ImList;

import java.io.IOException;

/**
 * Runs a collection of AgsCatalogServers sequentially, one after the other
 * returning the result of the first one that returns a valid result.
 */
public class SequentialCatalogServer implements CatalogServer {
    private ImList<? extends CatalogServer> servers;

    public SequentialCatalogServer(ImList<? extends CatalogServer> servers) {
        this.servers = servers;
    }

    @Override public CatalogResult query(QueryConstraint cons) throws IOException {
        IOException last = null;
        for (CatalogServer srv : servers) {
            try {
                return srv.query(cons);
            } catch (IOException ex) {
                last = ex;
            }
        }
        if (last != null) throw last;
        throw new IOException("No catalog servers");
    }
}
