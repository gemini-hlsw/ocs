package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.ISPNightlyRecord;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.ISPRootNode;
import edu.gemini.pot.sp.SPNodeKey;
import java.util.Collections;
import java.util.List;

/**
 * A persister implemenation that returns nothing and does nothing.  Useful for
 * transient in-memory databases for testing, etc.
 */
public final class DoNothingPersister implements IDBPersister {

    // might as well have only one of these
    public static final DoNothingPersister INSTANCE = new DoNothingPersister();

    private DoNothingPersister() {
    }

    @Override public List<ISPProgram> reloadPrograms() {
        return Collections.emptyList();
    }

    @Override public List<ISPNightlyRecord> reloadPlans() {
        return Collections.emptyList();
    }

    @Override public void store(ISPRootNode node) {
        // Do nothing.
    }

    @Override public void remove(SPNodeKey key) {
        // Do nothing.
    }

    @Override public long size(SPNodeKey key) {
        return -1L;
    }

    @Override public long getTotalStorage() {
        return 0L;
    }
}
