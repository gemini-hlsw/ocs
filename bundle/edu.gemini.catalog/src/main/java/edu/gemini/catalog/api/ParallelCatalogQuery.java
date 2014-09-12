package edu.gemini.catalog.api;

import edu.gemini.catalog.impl.Util;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.MapOp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * REL-604: Accepts an edu.gemini.ags.catalog.api.AgsCatalogServer and a
 * List<AgsQueryConstraintProvider>, runs all the queries in the List in parallel and returns
 * the appropriate [...] results. This works somewhat like the AgsParallelCatalogServer
 * does, but obviously at a higher level because it is dealing with multiple queries and results and
 * keeps all the results rather than just the first one it gets back.
 */
public enum ParallelCatalogQuery {
    instance;

    private static final Logger LOG = Logger.getLogger(ParallelCatalogQuery.class.getName());

    // How long it waits before giving up.  Too long?
    private static final long TIMEOUT = 120000;

    /**
     * A map operation that turns a catalog server into a Callable.
     * ( Like the Util class, but with multiple constraints and a single server)
     */
    private static final class ToCallable implements MapOp<QueryConstraint, Callable<CatalogResult>> {
        private final CatalogServer srv;

        ToCallable(CatalogServer srv) {
            this.srv = srv;
        }

        @Override
        public Callable<CatalogResult> apply(final QueryConstraint cons) {
            return Util.toCallable(srv, cons);
        }
    }

    public List<CatalogResult> query(CatalogServer server, ImList<QueryConstraint> constraints)
            throws IOException {

        ImList<Callable<CatalogResult>> tasks = constraints.map(new ToCallable(server));
        ExecutorService exec = Executors.newCachedThreadPool();
        try {
             List<Future<CatalogResult>> list = exec.invokeAll(tasks.toList(), TIMEOUT, TimeUnit.MILLISECONDS);
            exec.shutdownNow();
            List<CatalogResult> res = new ArrayList<CatalogResult>(list.size());
            for(Future<CatalogResult> future : list) {
                if (future.isCancelled()) {
                    throw new IOException("Catalog query error.");
                }
                res.add(future.get());
            }
            return res;
        } catch (ExecutionException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
}
