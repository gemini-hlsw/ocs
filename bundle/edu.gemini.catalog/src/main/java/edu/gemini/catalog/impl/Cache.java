package edu.gemini.catalog.impl;

import edu.gemini.catalog.api.*;
import edu.gemini.shared.util.immutable.*;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * LRU cache of catalog results.
 */
final class Cache {

    // An individual cache entry, which groups the query constraint and the
    // results that were produced with it.
    private static class Entry {
        final QueryConstraint params;
        final CatalogResult result;

        Entry(QueryConstraint params, CatalogResult result) {
            this.params = params;
            this.result = result;
        }
    }

    // How many cache entries to keep in the cache.
    private final int size;

    // Kept ordered by recent use from most recent to least recent.
    private final LinkedList<Entry> entries = new LinkedList<Entry>();

    /**
     * Constructs with the cache size, which determines how many entries are
     * kept around.
     */
    Cache(int size) {
        if (size < 0) throw new IllegalArgumentException("size must be non-negative: " + size);
        this.size = size;
    }

    /**
     * Finds the most recently used cache entry that will suffice for the given
     * query constraint.  Has the side effect of making the matching result
     * the most recently used cache entry.
     *
     * @param cons query constraint for which results are sought
     *
     * @return a matching catalog result that contains a superset (potentially)
     * of guide stars indentified by the given query constraints; None if there
     * is no matching entry in the cache
     */
    synchronized Option<CatalogResult> search(QueryConstraint cons) {
        ListIterator<Entry> lit = entries.listIterator();
        Option<Entry> res = None.instance();
        while (lit.hasNext()) {
            Entry cur = lit.next();
            if (cur.params.isSupersetOf(cons)) {
                res = new Some<Entry>(cur);
                lit.remove();
                break;
            }
        }

        // Move it to the head of the list.
        res.foreach(new ApplyOp<Entry>() {
            @Override public void apply(Entry entry) {
                entries.add(0, entry);
            }
        });

        return res.map(new MapOp<Entry, CatalogResult>() {
            @Override public CatalogResult apply(Entry entry) {
                return entry.result;
            }
        });
    }

    /**
     * Records a new catalog entry in the cache, making it the  most recently
     * used item and removing any older item that exceeds the limits for how
     * many entries may be kept.
     */
    synchronized void record(QueryConstraint cons, CatalogResult result) {
        entries.add(0, new Entry(cons, result));
        if (entries.size() >= size) entries.removeLast();
    }
}
