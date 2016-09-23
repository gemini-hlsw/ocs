package edu.gemini.pot.spdb;

import java.util.Collection;

/**
 * The IDBParallelFunctor interface provides an opportunity to signal that a
 * particular functor is designed to be executed in parallel across the set
 * of slave databases.  It is used only in a master/slave multidatabase
 * configuration of the ODB.  In this context, the master database checks
 * whether the functor is an IDBParallelFunctor and if so sends (a copy) to
 * each slave database.  Each slave runs the functor and returns the result
 * to the master.  The master passes the collection of executed functors
 * returned by the slaves to the {@link #mergeResults} method to be combined
 * in a single result and returned to the caller.
 */
@Deprecated
public interface IDBParallelFunctor extends IDBFunctor {

    /**
     * Merges the results contained in the collection of functors returned by
     * slave databases into one combined result.
     *
     * @param functorCollection collection of functors returned by slave
     * databases
     */
    void mergeResults(Collection<IDBFunctor> functorCollection);
}
