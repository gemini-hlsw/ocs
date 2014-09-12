package edu.gemini.catalog.impl;

import edu.gemini.catalog.api.*;
import edu.gemini.shared.util.immutable.MapOp;

import java.util.concurrent.Callable;

public final class Util {
    private Util() {}

    /**
     * Wraps the query in a Callable&lt;AgsCatalogResult&gt;
     */
    public static Callable<CatalogResult> toCallable(final CatalogServer server, final QueryConstraint cons) {
        return new Callable<CatalogResult>() {
            @Override public CatalogResult call() throws Exception {
                return server.query(cons);
            }
        };
    }

    /**
     * A map operation that turns a catalog server into a Callable.
     */
    public static final class ToCallable implements MapOp<CatalogServer, Callable<CatalogResult>> {
        private final QueryConstraint cons;

        ToCallable(QueryConstraint cons) {
            this.cons = cons;
        }

        @Override public Callable<CatalogResult> apply(final CatalogServer srv) {
            return toCallable(srv, cons);
        }
    }
}
