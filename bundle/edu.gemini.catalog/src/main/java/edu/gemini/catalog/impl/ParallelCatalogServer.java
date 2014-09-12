package edu.gemini.catalog.impl;

import edu.gemini.catalog.api.*;
import edu.gemini.shared.util.immutable.ImList;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Runs a collection of AgsCatalogServer queries in parallel, returning the
 * results provided by the first one that responds (and ignoring all others).
 */
public class ParallelCatalogServer implements CatalogServer {

    // How long it waits before giving up.  Too long?
    private static final long TIMEOUT = 120000;

    private final ImList<? extends CatalogServer> servers;

    /**
     * Create with a collection of catalog servers to query in parallel.
     */
    public ParallelCatalogServer(ImList<? extends CatalogServer> servers) {
        this.servers = servers;
    }

    @Override public CatalogResult query(QueryConstraint cons) throws IOException {
        ImList<Callable<CatalogResult>> tasks = servers.map(new Util.ToCallable(cons));
        ExecutorService exec = Executors.newCachedThreadPool();
        try {
            CatalogResult res = exec.invokeAny(tasks.toList(), TIMEOUT, TimeUnit.MILLISECONDS);
            exec.shutdownNow();
            return res;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

}
